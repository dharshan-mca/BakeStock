package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductListScreen(
    repository: BakeStockRepository,
    onAddProductClick: () -> Unit,
    onProductClick: (String) -> Unit,
    userRole: UserRole? = UserRole.ADMIN
) {
    val products by repository.allProducts.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf("Newest") }
    var syncStatusMessage by remember { mutableStateOf("Connecting to Cloud Sync...") }

    DisposableEffect(Unit) {
        repository.startRealtimeInventorySync { status ->
            syncStatusMessage = status
        }
        onDispose {
            repository.stopRealtimeInventorySync()
        }
    }

    val filterOptions = listOf("All", "Grocery", "Raw Materials", "Finished", "Low Stock", "Near Expiry")
    val sortOptions = listOf("Newest", "Oldest", "A-Z", "Z-A", "Highest Qty", "Lowest Qty")

    val filteredProducts = products.filter { prod ->
        val q = searchQuery.trim().lowercase()
        val matchesSearch = q.isEmpty() ||
                prod.name.lowercase().contains(q) ||
                prod.category.lowercase().contains(q) ||
                prod.supplierName.lowercase().contains(q) ||
                prod.batchNumber.lowercase().contains(q) ||
                prod.storageLocation.lowercase().contains(q)

        val matchesFilter = when (selectedFilter) {
            "Grocery" -> prod.productType == ProductType.GROCERY
            "Raw Materials" -> prod.productType == ProductType.BAKING_RAW_MATERIAL || prod.productType == ProductType.PERISHABLE
            "Finished" -> prod.productType == ProductType.FINISHED_BAKERY_PRODUCT
            "Low Stock" -> prod.stockStatus == StockStatus.LOW_STOCK || prod.stockStatus == StockStatus.OUT_OF_STOCK
            "Near Expiry" -> prod.stockStatus == StockStatus.NEAR_EXPIRY || prod.stockStatus == StockStatus.EXPIRED
            else -> true
        }

        matchesSearch && matchesFilter
    }.let { list ->
        when (sortOption) {
            "A-Z" -> list.sortedBy { it.name.lowercase() }
            "Z-A" -> list.sortedByDescending { it.name.lowercase() }
            "Oldest" -> list.sortedBy { it.createdDate }
            "Highest Qty" -> list.sortedByDescending { it.currentQuantity }
            "Lowest Qty" -> list.sortedBy { it.currentQuantity }
            else -> list.sortedByDescending { it.createdDate }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (userRole == UserRole.ADMIN) {
                FloatingActionButton(
                    onClick = onAddProductClick,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (userRole == UserRole.ADMIN) "Product & Stock Catalog" else "Inventory & Raw Material Catalog",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Products")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    sortOption = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Realtime Sync Status Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Realtime Firestore Cache: $syncStatusMessage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (userRole != UserRole.ADMIN) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Catalog Read-Only Mode: Stock maintenance & edits are restricted to Admin.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            SearchAndFilterBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                filterOptions = filterOptions,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    title = "No Products Found",
                    message = "Try clearing search or filter parameters."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    repository: BakeStockRepository,
    productIdToEdit: String? = null,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val products by repository.allProducts.collectAsState(initial = emptyList())
    val existingProduct = products.find { it.id == productIdToEdit }

    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "Baking Raw Materials") }
    var productType by remember { mutableStateOf(existingProduct?.productType ?: ProductType.BAKING_RAW_MATERIAL) }
    var quantityText by remember { mutableStateOf(existingProduct?.currentQuantity?.toString() ?: "50") }
    var unit by remember { mutableStateOf(existingProduct?.unit ?: UnitOfMeasure.KG) }
    var minLevelText by remember { mutableStateOf(existingProduct?.minStockLevel?.toString() ?: "10") }
    var reorderLevelText by remember { mutableStateOf(existingProduct?.reorderLevel?.toString() ?: "20") }
    var priceText by remember { mutableStateOf(existingProduct?.purchasePrice?.toString() ?: "2.5") }
    var supplierName by remember { mutableStateOf(existingProduct?.supplierName ?: "Golden Wheat Mills") }
    var batchNumber by remember { mutableStateOf(existingProduct?.batchNumber ?: "BATCH-${System.currentTimeMillis() / 10000}") }
    var storageLocation by remember { mutableStateOf(existingProduct?.storageLocation ?: "Main Pantry") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productIdToEdit == null) "Add New Product" else "Edit Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )
            }

            item {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Box {
                    OutlinedTextField(
                        value = productType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product Type") },
                        trailingIcon = { IconButton(onClick = { typeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                        modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        ProductType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    productType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Initial Quantity *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_qty_input")
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = unit.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { IconButton(onClick = { unitExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                            modifier = Modifier.fillMaxWidth().clickable { unitExpanded = true }
                        )
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            UnitOfMeasure.values().forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.label} (${u.symbol})") },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minLevelText,
                        onValueChange = { minLevelText = it },
                        label = { Text("Min Stock Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reorderLevelText,
                        onValueChange = { reorderLevelText = it },
                        label = { Text("Reorder Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Purchase Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_price_input")
                    )
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = storageLocation,
                    onValueChange = { storageLocation = it },
                    label = { Text("Storage Location") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull()
                        val minLvl = minLevelText.toDoubleOrNull()
                        val reorderLvl = reorderLevelText.toDoubleOrNull()
                        val price = priceText.toDoubleOrNull()

                        if (name.isBlank() || category.isBlank()) {
                            errorMessage = "Product name and category are required."
                            return@Button
                        }
                        if (qty == null || qty < 0) {
                            errorMessage = "Valid non-negative quantity required."
                            return@Button
                        }
                        if (price == null || price < 0) {
                            errorMessage = "Valid purchase price required."
                            return@Button
                        }

                        val prod = Product(
                            id = existingProduct?.id ?: "prod_${UUID.randomUUID()}",
                            name = name,
                            category = category,
                            productType = productType,
                            currentQuantity = qty,
                            unit = unit,
                            minStockLevel = minLvl ?: 10.0,
                            reorderLevel = reorderLvl ?: 20.0,
                            purchasePrice = price,
                            supplierName = supplierName,
                            batchNumber = batchNumber,
                            storageLocation = storageLocation
                        )

                        coroutineScope.launch {
                            if (productIdToEdit == null) {
                                repository.insertProduct(prod)
                            } else {
                                repository.updateProduct(prod)
                            }
                            onSaveSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_product_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Product Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    repository: BakeStockRepository,
    productId: String,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit,
    userRole: UserRole? = UserRole.ADMIN
) {
    val coroutineScope = rememberCoroutineScope()
    val products by repository.allProducts.collectAsState(initial = emptyList())
    val transactions by repository.allStockTransactions.collectAsState(initial = emptyList())
    val product = products.find { it.id == productId }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var newQtyText by remember { mutableStateOf("") }
    var adjustReason by remember { mutableStateOf("") }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Product not found.")
        }
        return
    }

    val productTxs = transactions.filter { it.productId == productId }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (userRole == UserRole.ADMIN) {
                        IconButton(onClick = { onEditClick(product.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = product.productType.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            StockStatusBadge(status = product.stockStatus)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "${product.currentQuantity} ${product.unit.symbol}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Purchase Price: $${product.purchasePrice} / ${product.unit.symbol}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                SectionHeader(title = "Specifications")
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpecRow("Category", product.category)
                        SpecRow("Supplier", product.supplierName)
                        SpecRow("Batch Number", product.batchNumber)
                        SpecRow("Min Stock Level", "${product.minStockLevel} ${product.unit.symbol}")
                        SpecRow("Reorder Level", "${product.reorderLevel} ${product.unit.symbol}")
                        SpecRow("Storage Location", product.storageLocation)
                        SpecRow("Expiry Date", dateFormat.format(Date(product.expiryDate)))
                    }
                }
            }

            item {
                if (userRole == UserRole.ADMIN) {
                    Button(
                        onClick = {
                            newQtyText = product.currentQuantity.toString()
                            showAdjustDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manual Stock Adjustment")
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Manual stock editing is restricted to Admin. Request stock via Kitchen / Delivery workflow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Stock Movement History")
                if (productTxs.isEmpty()) {
                    Text("No transaction history logged for this product.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        productTxs.forEach { tx ->
                            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = tx.transactionType, fontWeight = FontWeight.Bold)
                                        Text(text = tx.reason, style = MaterialTheme.typography.bodySmall)
                                        Text(text = dateFormat.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(text = "${tx.quantity} ${tx.unit.symbol}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Adjust Stock Quantity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newQtyText,
                        onValueChange = { newQtyText = it },
                        label = { Text("New Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = adjustReason,
                        onValueChange = { adjustReason = it },
                        label = { Text("Reason for adjustment") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQ = newQtyText.toDoubleOrNull()
                        if (newQ != null) {
                            coroutineScope.launch {
                                repository.updateManualStock(product.id, newQ, adjustReason.ifEmpty { "Manual adjustment" }, "Admin", UserRole.ADMIN)
                                showAdjustDialog = false
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdjustDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}
