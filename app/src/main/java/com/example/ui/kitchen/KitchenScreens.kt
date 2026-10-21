package com.example.ui.kitchen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.BakeStockRepository
import com.example.ui.shared.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenDashboardScreen(
    repository: BakeStockRepository,
    onNavigateToProductionPlan: () -> Unit,
    onNavigateToIngredientRequest: () -> Unit,
    onNavigateToBatchCompletion: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val batches by repository.allProductionBatches.collectAsState(initial = emptyList())
    val historyList by repository.allProductionHistory.collectAsState(initial = emptyList())
    val products by repository.allProducts.collectAsState(initial = emptyList())
    val wastageList by repository.allWastageRecords.collectAsState(initial = emptyList())

    var syncStatusText by remember { mutableStateOf("Syncing Firestore...") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ProductionStatus?>(null) }
    var selectedSortOrder by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, HIGHEST_QTY

    var showWastageDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.startRealtimeProductionSync { status ->
            syncStatusText = status
        }
        repository.startRealtimeInventorySync { status ->
            syncStatusText = status
        }
    }

    val finishedProducts = products.filter { it.productType == ProductType.FINISHED_BAKERY_PRODUCT }
    val rawMaterials = products.filter { it.productType != ProductType.FINISHED_BAKERY_PRODUCT }

    val todayBatches = batches.filter { isToday(it.productionDate) }
    val completedBatchesToday = todayBatches.filter { it.status == ProductionStatus.COMPLETED }
    val pendingBatchesToday = todayBatches.filter { it.status == ProductionStatus.PLANNED || it.status == ProductionStatus.IN_PROGRESS }
    val cancelledBatchesToday = todayBatches.filter { it.status == ProductionStatus.CANCELLED }

    val totalProducedToday = completedBatchesToday.sumOf { it.producedQuantity }
    val todayWastageVal = wastageList.filter { isToday(it.date) }.sumOf { it.totalWastageValue }

    val filteredBatches = remember(batches, searchQuery, selectedStatusFilter, selectedSortOrder) {
        batches.filter { batch ->
            val matchesSearch = searchQuery.isEmpty() ||
                    batch.batchNumber.contains(searchQuery, ignoreCase = true) ||
                    batch.productName.contains(searchQuery, ignoreCase = true) ||
                    batch.createdBy.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || batch.status == selectedStatusFilter
            matchesSearch && matchesStatus
        }.sortedWith { a, b ->
            when (selectedSortOrder) {
                "OLDEST" -> a.productionDate.compareTo(b.productionDate)
                "HIGHEST_QTY" -> b.plannedQuantity.compareTo(a.plannedQuantity)
                else -> b.productionDate.compareTo(a.productionDate)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showWastageDialog = true },
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                text = { Text("Log Wastage", fontWeight = FontWeight.Bold) },
                containerColor = StatusRed,
                contentColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("log_wastage_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Sync Status Banner
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenTertiary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ForestGreenTertiary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Realtime Engine: $syncStatusText",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenTertiary
                        )
                    }
                }
            }

            // Header & Quick Action Buttons
            item {
                Text(
                    text = "Kitchen Master Workspace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Production Dashboard, Recipe Scaling & Cloud Firestore Sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToProductionPlan,
                        modifier = Modifier.weight(1f).height(46.dp).testTag("plan_production_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Batch", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToIngredientRequest,
                        modifier = Modifier.weight(1f).height(46.dp).testTag("request_ingredients_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Stock")
                    }

                    OutlinedIconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.size(46.dp).testTag("production_history_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Production History")
                    }
                }
            }

            // Production Metrics Dashboard
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryMiniCard(
                            title = "Today's Batches",
                            value = "${todayBatches.size}",
                            icon = Icons.Default.PrecisionManufacturing,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            title = "Completed",
                            value = "${completedBatchesToday.size}",
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            title = "Pending",
                            value = "${pendingBatchesToday.size}",
                            icon = Icons.Default.HourglassTop,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryMiniCard(
                            title = "Produced Qty",
                            value = String.format("%.0f pcs", totalProducedToday),
                            icon = Icons.Default.BakeryDining,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            title = "Today Wastage",
                            value = String.format("$%.2f", todayWastageVal),
                            icon = Icons.Default.Delete,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Finished Products Quick Produce Section
            item {
                SectionHeader(title = "Finished Goods Inventory")
                if (finishedProducts.isEmpty()) {
                    Text("No finished bakery products added to catalog yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        finishedProducts.forEach { prod ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = prod.name, fontWeight = FontWeight.Bold)
                                        Text(text = "In Store: ${prod.currentQuantity} ${prod.unit.symbol}", style = MaterialTheme.typography.bodyMedium)
                                        StatusBadge(statusText = prod.stockStatus.label, backgroundColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.primary)
                                    }
                                    Button(
                                        onClick = onNavigateToBatchCompletion,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Produce Batch")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Production Batches List Header & Search/Filter Bar
            item {
                SectionHeader(title = "Production Batches & Realtime Status")
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Batch #, Product, or Chef...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") } }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("search_batches_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All Status") }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ProductionStatus.COMPLETED,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ProductionStatus.COMPLETED) null else ProductionStatus.COMPLETED },
                        label = { Text("Completed") }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ProductionStatus.PLANNED,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ProductionStatus.PLANNED) null else ProductionStatus.PLANNED },
                        label = { Text("Planned") }
                    )
                }
            }

            // Production Batches
            if (filteredBatches.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Production Batches",
                        message = "No batches match your search and filter criteria."
                    )
                }
            } else {
                items(filteredBatches, key = { it.id }) { b ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Batch ${b.batchNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Product: ${b.productName}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Operator: ${b.createdBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(
                                    statusText = b.status.label,
                                    backgroundColor = if (b.status == ProductionStatus.COMPLETED) ForestGreenTertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = if (b.status == ProductionStatus.COMPLETED) ForestGreenTertiary else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Planned: ${b.plannedQuantity} pcs", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Produced: ${b.producedQuantity} pcs (Damaged: ${b.damagedQuantity})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Date: ${dateFormat.format(Date(b.productionDate))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Quick Log Wastage Dialog
    if (showWastageDialog) {
        QuickLogWastageDialog(
            products = products,
            repository = repository,
            onDismiss = { showWastageDialog = false },
            onSuccess = {
                showWastageDialog = false
            }
        )
    }

    // Production History Dialog
    if (showHistoryDialog) {
        ProductionHistoryDialog(
            historyList = historyList,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionPlanningScreen(
    repository: BakeStockRepository,
    onBack: () -> Unit,
    onExecuteBatch: () -> Unit
) {
    val recipes by repository.allRecipes.collectAsState(initial = emptyList())
    val products by repository.allProducts.collectAsState(initial = emptyList())

    var selectedRecipe by remember { mutableStateOf(recipes.firstOrNull()) }
    var plannedQtyText by remember { mutableStateOf("50") }

    val plannedQty = plannedQtyText.toDoubleOrNull() ?: 1.0

    // Fetch required ingredients for selected recipe
    val recipeIngredients = remember(selectedRecipe, recipes) {
        if (selectedRecipe != null) {
            // We can calculate based on recipe ingredients in state if loaded
            emptyList<RecipeIngredient>()
        } else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Production Planning", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "1. Enter Desired Production Quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = plannedQtyText,
                    onValueChange = { plannedQtyText = it },
                    label = { Text("Planned Batch Quantity (pcs / loaves / items)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("planned_qty_input")
                )
            }

            item {
                Text(text = "2. Select Recipe from Master Catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipes.forEach { rec ->
                        Card(
                            onClick = { selectedRecipe = rec },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRecipe?.id == rec.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedRecipe?.id == rec.id, onClick = { selectedRecipe = rec })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = rec.productName, fontWeight = FontWeight.Bold)
                                    Text(text = "Category: ${rec.category} • Prep: ${rec.estimatedPreparationTime}m • Bake: ${rec.estimatedBakeTime}m", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Standard Yield: ${rec.yieldQuantity} ${rec.yieldUnit.symbol}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onExecuteBatch,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("proceed_to_batch_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proceed to Execute Batch & Validate Stock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchScreen(
    repository: BakeStockRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val recipes by repository.allRecipes.collectAsState(initial = emptyList())
    val products by repository.allProducts.collectAsState(initial = emptyList())

    val finishedProducts = products.filter { it.productType == ProductType.FINISHED_BAKERY_PRODUCT }
    val rawMaterials = products.filter { it.productType != ProductType.FINISHED_BAKERY_PRODUCT }

    var selectedFinishedProduct by remember { mutableStateOf(finishedProducts.firstOrNull()) }
    var selectedRecipe by remember { mutableStateOf(recipes.firstOrNull()) }
    var batchNumber by remember { mutableStateOf("BATCH-KITCHEN-${System.currentTimeMillis() / 100000}") }
    var plannedQtyText by remember { mutableStateOf("100") }
    var damagedQtyText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("Standard baking cycle") }

    var resultMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    val plannedQty = plannedQtyText.toDoubleOrNull() ?: 1.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Production Batch", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    label = { Text("Batch Number *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Select Finished Product to Produce:", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    finishedProducts.forEach { fp ->
                        Card(
                            onClick = { selectedFinishedProduct = fp },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFinishedProduct?.id == fp.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedFinishedProduct?.id == fp.id, onClick = { selectedFinishedProduct = fp })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = fp.name, fontWeight = FontWeight.Bold)
                                    Text(text = "Current Stock: ${fp.currentQuantity} ${fp.unit.symbol}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = plannedQtyText,
                        onValueChange = { plannedQtyText = it },
                        label = { Text("Production Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("planned_qty_input")
                    )
                    OutlinedTextField(
                        value = damagedQtyText,
                        onValueChange = { damagedQtyText = it },
                        label = { Text("Damaged / Rejects") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Batch Notes / Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (resultMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ForestGreenTertiary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = resultMessage!!,
                            color = ForestGreenTertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val prod = selectedFinishedProduct ?: finishedProducts.firstOrNull()
                        val planQty = plannedQtyText.toDoubleOrNull()
                        val damQty = damagedQtyText.toDoubleOrNull() ?: 0.0

                        if (prod == null || planQty == null || planQty <= 0) {
                            errorMessage = "Please select a product and enter a valid production quantity."
                            return@Button
                        }

                        isExecuting = true
                        errorMessage = null

                        coroutineScope.launch {
                            val res = repository.executeProductionBatch(
                                recipe = selectedRecipe,
                                finishedProduct = prod,
                                productionQuantity = planQty,
                                damagedQuantity = damQty,
                                batchNumber = batchNumber,
                                notes = notes,
                                createdBy = "Chef Gordon"
                            )
                            isExecuting = false
                            if (res.isSuccess) {
                                resultMessage = res.getOrNull()
                                errorMessage = null
                                onSuccess()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    enabled = !isExecuting,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("execute_batch_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute Batch & Deduct Raw Stock (Firestore)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickLogWastageDialog(
    products: List<Product>,
    repository: BakeStockRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var batchNumber by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(WastageReason.SPOILED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Product Wastage", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Select Product:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                products.take(5).forEach { prod ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProduct = prod }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedProduct?.id == prod.id, onClick = { selectedProduct = prod })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${prod.name} (In Store: ${prod.currentQuantity} ${prod.unit.symbol})")
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Wasted Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    label = { Text("Batch Number (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Reason:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                WastageReason.values().forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reason = r }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(r.label)
                    }
                }

                errorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prod = selectedProduct
                    val qty = qtyText.toDoubleOrNull()
                    if (prod == null || qty == null || qty <= 0) {
                        errorMessage = "Please enter valid product and quantity."
                        return@Button
                    }
                    coroutineScope.launch {
                        val record = WastageRecord(
                            id = "wastage_${UUID.randomUUID().toString().substring(0, 8)}",
                            productId = prod.id,
                            productName = prod.name,
                            batchNumber = batchNumber,
                            quantity = qty,
                            unit = prod.unit,
                            unitPrice = prod.purchasePrice,
                            totalWastageValue = qty * prod.purchasePrice,
                            reason = reason,
                            date = System.currentTimeMillis(),
                            recordedBy = "Chef Gordon"
                        )
                        repository.recordWastage(record)
                        onSuccess()
                    }
                }
            ) {
                Text("Submit Wastage")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProductionHistoryDialog(
    historyList: List<ProductionHistory>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Production History Log", fontWeight = FontWeight.Bold) },
        text = {
            if (historyList.isEmpty()) {
                Text("No production history records found.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(350.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList) { h ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = h.recipeName, fontWeight = FontWeight.Bold)
                                Text(text = "Produced: ${h.quantityProduced} pcs • Operator: ${h.operator}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Time: ${dateFormat.format(Date(h.startTime))} (${h.durationMinutes} mins)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenIngredientRequestScreen(
    repository: BakeStockRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val products by repository.allProducts.collectAsState(initial = emptyList())
    val rawMaterials = products.filter { it.productType != ProductType.FINISHED_BAKERY_PRODUCT }

    var priority by remember { mutableStateOf(RequestPriority.HIGH) }
    var notes by remember { mutableStateOf("Required for upcoming bakery shift") }
    var requestedQtyText by remember { mutableStateOf("20") }
    var selectedRawMaterial by remember { mutableStateOf<Product?>(null) }
    var showMaterialDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rawMaterials) {
        if (selectedRawMaterial == null && rawMaterials.isNotEmpty()) {
            selectedRawMaterial = rawMaterials.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Raw Material Stock") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kitchen Master: Select stock items and submit a formal request to the Delivery Man.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Text("Select Raw Material / Ingredient", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = showMaterialDropdown,
                    onExpandedChange = { showMaterialDropdown = !showMaterialDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedRawMaterial?.let { "${it.name} (In Store: ${it.currentQuantity} ${it.unit.symbol})" } ?: "Select Item",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Stock Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMaterialDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showMaterialDropdown,
                        onDismissRequest = { showMaterialDropdown = false }
                    ) {
                        rawMaterials.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} — Current: ${item.currentQuantity} ${item.unit.symbol}") },
                                onClick = {
                                    selectedRawMaterial = item
                                    showMaterialDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = requestedQtyText,
                    onValueChange = { requestedQtyText = it },
                    label = { Text("Quantity Needed (${selectedRawMaterial?.unit?.symbol ?: "Units"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("requested_qty_input")
                )
            }

            item {
                Text("Request Priority", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestPriority.values().forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Kitchen Notes / Urgency Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                item {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val raw = selectedRawMaterial
                        val reqQty = requestedQtyText.toDoubleOrNull()
                        if (raw == null) {
                            errorMessage = "Please select a stock item."
                            return@Button
                        }
                        if (reqQty == null || reqQty <= 0) {
                            errorMessage = "Please enter a valid requested quantity."
                            return@Button
                        }

                        coroutineScope.launch {
                            val reqId = "req_${UUID.randomUUID()}"
                            val request = IngredientRequest(
                                id = reqId,
                                requestNumber = "REQ-${System.currentTimeMillis() / 100000}",
                                requestedBy = "user_kitchen",
                                requestedByName = "Chef Gordon (Kitchen Master)",
                                priority = priority,
                                notes = notes
                            )
                            val item = IngredientRequestItem(
                                id = "item_${UUID.randomUUID()}",
                                requestId = reqId,
                                productId = raw.id,
                                productName = raw.name,
                                requestedQuantity = reqQty,
                                availableStock = raw.currentQuantity,
                                unit = raw.unit
                            )
                            repository.createIngredientRequest(request, listOf(item))
                            onSuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_stock_request_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Request to Delivery Man", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
