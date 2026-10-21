package com.example.ui.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.BakeStockRepository
import com.example.ui.shared.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DeliveryDashboardScreen(
    repository: BakeStockRepository,
    onRequestClick: (String) -> Unit
) {
    val requests by repository.allIngredientRequests.collectAsState(initial = emptyList())

    val pendingRequests = requests.filter { it.status == RequestStatus.PENDING || it.status == RequestStatus.ACCEPTED }
    val completedRequests = requests.filter { it.status == RequestStatus.COMPLETED || it.status == RequestStatus.FULLY_COLLECTED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Delivery Man Workspace",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Collect grocery requirements & update store collection status",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Pending Collection",
                    value = "${pendingRequests.size} requests",
                    icon = Icons.Default.Pending,
                    containerColor = StatusAmber.copy(alpha = 0.15f),
                    contentColor = StatusAmber,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Completed Today",
                    value = "${completedRequests.size} requests",
                    icon = Icons.Default.TaskAlt,
                    containerColor = StatusGreen.copy(alpha = 0.15f),
                    contentColor = StatusGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader(title = "Assigned Delivery & Collection Requests")
            if (requests.isEmpty()) {
                EmptyStateView(title = "No Delivery Requests", message = "No kitchen requests submitted currently.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    requests.forEach { req ->
                        Card(
                            onClick = { onRequestClick(req.id) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = req.requestNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    RequestStatusBadge(status = req.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Requested by: ${req.requestedByName}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Notes: ${req.notes}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    PriorityBadge(priority = req.priority)
                                    Text(text = "Tap to update collection >", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
fun DeliveryUpdateScreen(
    repository: BakeStockRepository,
    requestId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val requests by repository.allIngredientRequests.collectAsState(initial = emptyList())
    val request = requests.find { it.id == requestId }
    val requestItems by repository.getRequestItems(requestId).collectAsState(initial = emptyList())

    val itemCounts = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(requestItems) {
        requestItems.forEach { item ->
            if (!itemCounts.containsKey(item.id)) {
                val initialQty = if (item.collectedQuantity > 0) item.collectedQuantity else item.requestedQuantity
                itemCounts[item.id] = if (initialQty % 1.0 == 0.0) initialQty.toInt().toString() else initialQty.toString()
            }
        }
    }

    var noteText by remember { mutableStateOf("Verified and collected count from store pantry") }
    var selectedStatus by remember { mutableStateOf(RequestStatus.FULLY_COLLECTED) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    if (request == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Request not found.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Request #${request.requestNumber}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Requested by: ${request.requestedByName}", fontWeight = FontWeight.Bold)
                            PriorityBadge(priority = request.priority)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Kitchen Notes: ${request.notes}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delivery Man: Enter the actual collected count for each requested stock item below.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Requested Stock Items")
            }

            if (requestItems.isEmpty()) {
                item {
                    Text("No items linked to this request.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(requestItems, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = item.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Requested: ${item.requestedQuantity} ${item.unit.symbol}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Available Stock: ${item.availableStock} ${item.unit.symbol}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            OutlinedTextField(
                                value = itemCounts[item.id] ?: "",
                                onValueChange = { itemCounts[item.id] = it },
                                label = { Text("Enter Fulfill Count (${item.unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("collected_qty_input_${item.id}")
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Delivery Remarks / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Select Delivery Request Status", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedStatus = RequestStatus.ACCEPTED },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStatus == RequestStatus.ACCEPTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedStatus == RequestStatus.ACCEPTED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Accept") }

                    Button(
                        onClick = { selectedStatus = RequestStatus.FULLY_COLLECTED },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStatus == RequestStatus.FULLY_COLLECTED) ForestGreenTertiary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedStatus == RequestStatus.FULLY_COLLECTED) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Collected") }
                }
            }

            if (warningMessage != null) {
                item { Text(text = warningMessage!!, color = MaterialTheme.colorScheme.error) }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val updatedItems = requestItems.map { item ->
                            val countVal = itemCounts[item.id]?.toDoubleOrNull() ?: item.requestedQuantity
                            item.copy(
                                collectedQuantity = countVal,
                                isCollected = true
                            )
                        }
                        coroutineScope.launch {
                            repository.updateDeliveryStatus(
                                request = request.copy(notes = "${request.notes} | Delivery notes: $noteText"),
                                updatedItems = updatedItems,
                                newStatus = selectedStatus,
                                performedBy = "Delivery Man"
                            )
                            onSuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_delivery_update_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Request Count & Submit Delivery", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
