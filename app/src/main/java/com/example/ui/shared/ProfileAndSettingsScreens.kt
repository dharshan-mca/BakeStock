package com.example.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.data.repository.BakeStockRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    repository: BakeStockRepository,
    onNavigateToNotifications: () -> Unit,
    onNavigateToActivityLogs: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(currentUser?.name ?: "User") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            name = it.name
            phone = it.phone
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = currentUser?.name ?: "BakeStock User",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${currentUser?.email} • ${currentUser?.role?.name?.replace("_", " ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        updateSuccessMessage = null
                    },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        updateSuccessMessage = null
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (updateSuccessMessage != null) {
                    Text(
                        text = updateSuccessMessage!!,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        authViewModel.updateProfile(name, phone)
                        updateSuccessMessage = "Profile updated successfully!"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update Profile")
                }
            }
        }

        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("System Notifications") },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNotifications() }
                        .testTag("nav_notifications")
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Audit Activity Logs") },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToActivityLogs() }
                        .testTag("nav_logs")
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of BakeStock", fontWeight = FontWeight.Bold)
        }
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Confirm Logout",
            message = "Are you sure you want to sign out of BakeStock?",
            confirmText = "Sign Out",
            onConfirm = {
                showLogoutDialog = false
                authViewModel.logout()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val notifications by repository.allNotifications.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isEvaluating by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Settings state
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var inventoryAlerts by remember { mutableStateOf(true) }
    var productionAlerts by remember { mutableStateOf(true) }
    var deliveryAlerts by remember { mutableStateOf(true) }
    var reportAlerts by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.startRealtimeNotificationSync()
    }

    DisposableEffect(Unit) {
        onDispose {
            repository.stopRealtimeNotificationSync()
        }
    }

    val filteredNotifications = notifications.filter { notif ->
        val matchesSearch = searchQuery.isBlank() ||
                notif.title.contains(searchQuery, ignoreCase = true) ||
                notif.message.contains(searchQuery, ignoreCase = true) ||
                notif.module.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Unread" -> !notif.isRead
            "Low Stock" -> notif.type == "LOW_STOCK" || notif.module == "INVENTORY"
            "Expiry" -> notif.type == "EXPIRY"
            "Production" -> notif.module == "PRODUCTION" || notif.type == "BATCH"
            "Delivery" -> notif.module == "DELIVERY" || notif.type == "REQUEST"
            "Reports" -> notif.module == "REPORTS" || notif.type == "REPORT"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    val unreadCount = notifications.count { !it.isRead }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification Sound")
                        Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration")
                        Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Role Channels & Alerts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Inventory & Low Stock Alerts")
                        Switch(checked = inventoryAlerts, onCheckedChange = { inventoryAlerts = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Production & Kitchen Alerts")
                        Switch(checked = productionAlerts, onCheckedChange = { productionAlerts = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Delivery & Request Alerts")
                        Switch(checked = deliveryAlerts, onCheckedChange = { deliveryAlerts = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Executive Reports & Summaries")
                        Switch(checked = reportAlerts, onCheckedChange = { reportAlerts = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Save Preferences")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("In-App Notification Center", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text("$unreadCount unread alerts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Realtime sync active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Notification Settings")
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.markAllNotificationsAsRead()
                            }
                        }
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark All Read")
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.clearAllNotifications()
                            }
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notifications...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isEvaluating = true
                            repository.evaluateSmartNotifications()
                            isEvaluating = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isEvaluating
                ) {
                    Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEvaluating) "Scanning System..." else "Run Smart Alert Engine")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filters = listOf("All", "Unread", "Low Stock", "Expiry", "Production", "Delivery", "Reports")
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter).coerceAtLeast(0),
                edgePadding = 0.dp
            ) {
                filters.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredNotifications.isEmpty()) {
                EmptyStateView(
                    title = "No Notifications Found",
                    message = if (searchQuery.isNotBlank()) "No alerts matching '$searchQuery'." else "Your notification log is clear for '$selectedFilter'."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notif ->
                        val isCritical = notif.priority == "CRITICAL" || notif.type == "CRITICAL"
                        val isHigh = notif.priority == "HIGH" || notif.type == "LOW_STOCK" || notif.type == "EXPIRY"

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!notif.isRead) {
                                    if (isCritical) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!notif.isRead) {
                                    coroutineScope.launch {
                                        repository.markNotificationAsRead(notif.id)
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = when (notif.type) {
                                        "LOW_STOCK" -> Icons.Default.Warning
                                        "EXPIRY" -> Icons.Default.EventBusy
                                        "REQUEST", "DELIVERY" -> Icons.Default.LocalShipping
                                        "BATCH", "PRODUCTION" -> Icons.Default.SoupKitchen
                                        "REPORT" -> Icons.Default.Assessment
                                        else -> Icons.Default.Notifications
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isCritical -> StatusRed
                                        isHigh -> StatusAmber
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = notif.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (isCritical) {
                                            Surface(
                                                color = StatusRed.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text(
                                                    text = "CRITICAL",
                                                    color = StatusRed,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = notif.message, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dateFormat.format(Date(notif.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (notif.module.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = notif.module,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.deleteNotification(notif.id)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogsScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val logs by repository.allActivityLogs.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Audit Logs") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = log.action, fontWeight = FontWeight.Bold)
                            Text(text = log.userRole.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(text = "By ${log.userName} on ${log.recordAffected}", style = MaterialTheme.typography.bodySmall)
                        Text(text = dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
