package com.example.ui.admin

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.BakeStockRepository
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseStatusScreen(repository: BakeStockRepository? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val products by repository?.allProducts?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val users by repository?.allUsers?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val batches by repository?.allProductionBatches?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val recipes by repository?.allRecipes?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val requests by repository?.allIngredientRequests?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val logs by repository?.allActivityLogs?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val reports by repository?.allSavedReports?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    var backupList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isBackingUp by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    // Memory stats
    val runtime = Runtime.getRuntime()
    val maxMemoryMb = (runtime.maxMemory() / (1024 * 1024))
    val totalMemoryMb = (runtime.totalMemory() / (1024 * 1024))
    val freeMemoryMb = (runtime.freeMemory() / (1024 * 1024))
    val usedMemoryMb = totalMemoryMb - freeMemoryMb

    LaunchedEffect(refreshTrigger) {
        repository?.fetchLocalBackups(context) { list, error ->
            if (error == null) {
                backupList = list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Health & Database", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("system_status_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshTrigger++ },
                        modifier = Modifier.testTag("refresh_system_status_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Local System Health: Operational",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Room SQLite Database Engine Active • Offline First",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Core Subsystems
            Text("Core Subsystems", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusRow(
                        title = "Local Database (Room/SQLite)",
                        subtitle = "Persistent offline cache active and indexed",
                        isOk = true,
                        icon = Icons.Default.Storage
                    )
                    Divider()
                    StatusRow(
                        title = "Transaction Engine",
                        subtitle = "Instant local atomic reads & writes",
                        isOk = true,
                        icon = Icons.Default.Sync
                    )
                    Divider()
                    StatusRow(
                        title = "Offline Storage Mode",
                        subtitle = "Full offline capability enabled without cloud requirement",
                        isOk = true,
                        icon = Icons.Default.CloudOff
                    )
                }
            }

            // Table Record Counts
            Text("Local Database Records", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordStatRow(label = "Products & Raw Materials", count = products.size, icon = Icons.Default.Inventory2)
                    RecordStatRow(label = "User Accounts", count = users.size, icon = Icons.Default.Group)
                    RecordStatRow(label = "Active Recipes", count = recipes.size, icon = Icons.Default.MenuBook)
                    RecordStatRow(label = "Production Batches", count = batches.size, icon = Icons.Default.PrecisionManufacturing)
                    RecordStatRow(label = "Ingredient Requests", count = requests.size, icon = Icons.Default.LocalShipping)
                    RecordStatRow(label = "Activity & Audit Logs", count = logs.size, icon = Icons.Default.History)
                    RecordStatRow(label = "Saved Storage Reports", count = reports.size, icon = Icons.Default.Summarize)
                }
            }

            // Memory & Performance
            Text("Memory & Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Heap Allocated", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${usedMemoryMb}MB / ${totalMemoryMb}MB", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    LinearProgressIndicator(
                        progress = { (usedMemoryMb.toFloat() / totalMemoryMb.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Max Heap Limit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${maxMemoryMb}MB", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Local Database Backup & Snapshot Recovery
            Text("Database Snapshots & Disaster Recovery", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Generate a local database snapshot file to protect against unintended data modification or device reset.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            isBackingUp = true
                            backupStatusMessage = null
                            coroutineScope.launch {
                                repository?.createLocalBackup(context, "Admin") { success, msg ->
                                    isBackingUp = false
                                    backupStatusMessage = if (success) "Snapshot created: $msg" else "Backup failed: $msg"
                                    refreshTrigger++
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_local_backup_button"),
                        enabled = !isBackingUp
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Snapshot...")
                        } else {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Local Database Snapshot")
                        }
                    }

                    if (backupStatusMessage != null) {
                        Text(
                            text = backupStatusMessage ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (backupList.isNotEmpty()) {
                        Text("Saved Snapshots on Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        backupList.forEach { backup ->
                            val backupId = backup["backupId"]?.toString() ?: "Unknown"
                            val timeLong = (backup["createdAt"] as? Number)?.toLong() ?: 0L
                            val dateStr = if (timeLong > 0) dateFormat.format(Date(timeLong)) else "N/A"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(backupId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Created: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository?.restoreLocalBackup(context, backupId) { success, err ->
                                                    backupStatusMessage = if (success) "Snapshot verified & restored successfully." else "Restore failed: $err"
                                                    refreshTrigger++
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("restore_backup_${backupId}")
                                    ) {
                                        Text("Verify/Restore", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusRow(title: String, subtitle: String, isOk: Boolean, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = if (isOk) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isOk) "READY" else "OFFLINE",
                color = if (isOk) StatusGreen else StatusRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun RecordStatRow(label: String, count: Int, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp)
        }
        Text(
            text = "$count records",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
