package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseManagementScreen(
    repository: BakeStockRepository,
    onAddPurchaseClick: () -> Unit,
    onBack: () -> Unit
) {
    val purchases by repository.allPurchases.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPurchaseClick,
                modifier = Modifier.testTag("add_purchase_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Purchase")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            SummaryCard(
                title = "Total Purchases",
                value = "$${String.format("%.2f", purchases.sumOf { it.totalAmount })}",
                icon = Icons.Default.Receipt,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (purchases.isEmpty()) {
                EmptyStateView(title = "No Purchases Recorded", message = "Tap + to record new stock purchases.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(purchases) { pur ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Invoice #${pur.invoiceNumber}", fontWeight = FontWeight.Bold)
                                    Text(text = "Supplier: ${pur.supplierName}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = dateFormat.format(Date(pur.purchaseDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "$${String.format("%.2f", pur.totalAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    StatusBadge(statusText = pur.paymentStatus, backgroundColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.primary)
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
fun AddPurchaseScreen(
    repository: BakeStockRepository,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val suppliers by repository.allSuppliers.collectAsState(initial = emptyList())
    val products by repository.allProducts.collectAsState(initial = emptyList())

    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis() / 100000}") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "Golden Wheat Mills") }
    var paymentStatus by remember { mutableStateOf("Paid") }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var purchaseQtyText by remember { mutableStateOf("50") }

    val purchasedItemsList = remember { mutableStateListOf<Pair<Product, Double>>() }
    val totalAmount = purchasedItemsList.sumOf { (prod, qty) -> qty * prod.purchasePrice }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record New Purchase") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice Number") },
                    modifier = Modifier.fillMaxWidth().testTag("invoice_input")
                )
            }

            item {
                OutlinedTextField(
                    value = selectedSupplier,
                    onValueChange = { selectedSupplier = it },
                    label = { Text("Supplier Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SectionHeader(title = "Add Products to Purchase")
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = purchaseQtyText,
                                onValueChange = { purchaseQtyText = it },
                                label = { Text("Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                val qty = purchaseQtyText.toDoubleOrNull()
                                val prod = selectedProduct
                                if (prod != null && qty != null && qty > 0) {
                                    purchasedItemsList.add(prod to qty)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Item")
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Purchase Summary")
                purchasedItemsList.forEach { (prod, qty) ->
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "${prod.name} (x$qty ${prod.unit.symbol})")
                            Text(text = "$${String.format("%.2f", qty * prod.purchasePrice)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Total Amount: $${String.format("%.2f", totalAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (purchasedItemsList.isNotEmpty()) {
                            coroutineScope.launch {
                                repository.addPurchase(
                                    Purchase(
                                        id = "pur_${UUID.randomUUID()}",
                                        invoiceNumber = invoiceNumber,
                                        supplierId = "sup_1",
                                        supplierName = selectedSupplier,
                                        totalAmount = totalAmount,
                                        paymentStatus = paymentStatus
                                    ),
                                    purchasedItemsList.toList()
                                )
                                onSaveSuccess()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_purchase_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = purchasedItemsList.isNotEmpty()
                ) {
                    Text("Complete Purchase & Add to Stock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierManagementScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val suppliers by repository.allSuppliers.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var itemsSupplied by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers Directory") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suppliers) { sup ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = sup.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Contact: ${sup.contactNumber}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Supplies: ${sup.suppliedItems}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name") })
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Number") })
                    OutlinedTextField(value = itemsSupplied, onValueChange = { itemsSupplied = it }, label = { Text("Supplied Items") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotEmpty()) {
                        coroutineScope.launch {
                            repository.insertSupplier(Supplier("sup_${UUID.randomUUID()}", name, contact, "Main Address", itemsSupplied, "Net 30"))
                            showAddDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val users by repository.allUsers.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) } // null = All
    var selectedStatusFilter by remember { mutableStateOf<Boolean?>(null) } // null = All

    var showAddDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToResetPassword by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val staffList = remember(users, searchQuery, selectedRoleFilter, selectedStatusFilter) {
        users.filter { user ->
            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.phone.contains(searchQuery, ignoreCase = true)
            val matchesRole = selectedRoleFilter == null || user.role == selectedRoleFilter
            val matchesStatus = selectedStatusFilter == null || user.active == selectedStatusFilter
            matchesSearch && matchesRole && matchesStatus
        }
    }

    val totalCount = users.size
    val kitchenCount = users.count { it.role == UserRole.KITCHEN_MASTER }
    val deliveryCount = users.count { it.role == UserRole.DELIVERY_MAN }
    val activeCount = users.count { it.active }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Management", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Employee", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_employee_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Metrics Summary
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                SummaryMiniCard(title = "Total Staff", value = "$totalCount", icon = Icons.Default.People, modifier = Modifier.weight(1f))
                SummaryMiniCard(title = "Kitchen", value = "$kitchenCount", icon = Icons.Default.SoupKitchen, modifier = Modifier.weight(1f))
                SummaryMiniCard(title = "Delivery", value = "$deliveryCount", icon = Icons.Default.LocalShipping, modifier = Modifier.weight(1f))
                SummaryMiniCard(title = "Active", value = "$activeCount", icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search staff by name, email, or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") } }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                FilterChip(
                    selected = selectedRoleFilter == null,
                    onClick = { selectedRoleFilter = null },
                    label = { Text("All Roles") }
                )
                FilterChip(
                    selected = selectedRoleFilter == UserRole.KITCHEN_MASTER,
                    onClick = { selectedRoleFilter = if (selectedRoleFilter == UserRole.KITCHEN_MASTER) null else UserRole.KITCHEN_MASTER },
                    label = { Text("Kitchen Master") },
                    leadingIcon = { Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedRoleFilter == UserRole.DELIVERY_MAN,
                    onClick = { selectedRoleFilter = if (selectedRoleFilter == UserRole.DELIVERY_MAN) null else UserRole.DELIVERY_MAN },
                    label = { Text("Delivery Man") },
                    leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (staffList.isEmpty()) {
                EmptyStateView(
                    title = "No Staff Accounts Found",
                    message = "No employee accounts match your current search and filter criteria."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(staffList, key = { it.id }) { employee ->
                        EmployeeCard(
                            user = employee,
                            dateFormat = dateFormat,
                            onEdit = { userToEdit = employee },
                            onResetPassword = { userToResetPassword = employee },
                            onToggleActive = {
                                coroutineScope.launch {
                                    val updated = employee.copy(active = !employee.active, updatedDate = System.currentTimeMillis())
                                    repository.updateUser(updated)
                                    snackbarHostState.showSnackbar(
                                        "Employee '${employee.name}' is now ${if (updated.active) "Active" else "Inactive"}."
                                    )
                                }
                            },
                            onDelete = { userToDelete = employee }
                        )
                    }
                }
            }
        }
    }

    // Add Employee Dialog
    if (showAddDialog) {
        AddEmployeeDialog(
            repository = repository,
            onDismiss = { showAddDialog = false },
            onSuccess = { createdName ->
                showAddDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Employee '$createdName' account created successfully!")
                }
            }
        )
    }

    // Edit Employee Dialog
    userToEdit?.let { employee ->
        EditEmployeeDialog(
            employee = employee,
            repository = repository,
            onDismiss = { userToEdit = null },
            onSuccess = { updatedName ->
                userToEdit = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Employee '$updatedName' updated successfully!")
                }
            }
        )
    }

    // Reset Password Dialog
    userToResetPassword?.let { employee ->
        ResetPasswordDialog(
            employee = employee,
            repository = repository,
            onDismiss = { userToResetPassword = null },
            onSuccess = {
                userToResetPassword = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Password reset for '${employee.name}' successfully!")
                }
            }
        )
    }

    // Delete Employee Confirmation Dialog
    userToDelete?.let { employee ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete Employee Account", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete account for '${employee.name}' (${employee.email})?\n\nThis employee will no longer be able to sign in.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteUser(employee)
                            userToDelete = null
                            snackbarHostState.showSnackbar("Employee '${employee.name}' deleted.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmployeeCard(
    user: User,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (user.role == UserRole.KITCHEN_MASTER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (user.role) {
                                    UserRole.KITCHEN_MASTER -> Icons.Default.SoupKitchen
                                    UserRole.DELIVERY_MAN -> Icons.Default.LocalShipping
                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = null,
                                tint = if (user.role == UserRole.KITCHEN_MASTER) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                StatusBadge(
                    statusText = if (user.active) "Active" else "Inactive",
                    backgroundColor = if (user.active) com.example.ui.theme.StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f),
                    textColor = if (user.active) com.example.ui.theme.StatusGreen else StatusRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Role: ${user.role.name.replace("_", " ")}", style = MaterialTheme.typography.labelMedium)
                }

                if (user.phone.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = user.phone, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Created: ${dateFormat.format(Date(user.createdDate))} • By ${user.createdBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onResetPassword,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Pass", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            imageVector = if (user.active) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = if (user.active) "Deactivate" else "Activate",
                            tint = if (user.active) StatusRed else com.example.ui.theme.StatusGreen
                        )
                    }

                    if (user.role != UserRole.ADMIN) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeDialog(
    repository: BakeStockRepository,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.KITCHEN_MASTER) }
    var active by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Employee Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password *") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm Password *") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assign Role:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = role == UserRole.KITCHEN_MASTER,
                        onClick = { role = UserRole.KITCHEN_MASTER },
                        label = { Text("Kitchen Master") },
                        leadingIcon = { Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = role == UserRole.DELIVERY_MAN,
                        onClick = { role = UserRole.DELIVERY_MAN },
                        label = { Text("Delivery Man") },
                        leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Status:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (active) "Active" else "Inactive", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }

                errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedEmail = email.trim()
                    if (trimmedName.isEmpty() || trimmedEmail.isEmpty() || password.isEmpty()) {
                        errorMessage = "Please fill in all required fields."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    if (password.length < 4) {
                        errorMessage = "Password must be at least 4 characters."
                        return@Button
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                        val existing = repository.getUserByEmail(trimmedEmail)
                        if (existing != null) {
                            errorMessage = "An account with email '$trimmedEmail' already exists."
                            isSubmitting = false
                            return@launch
                        }

                        val newUser = User(
                            id = "emp_${UUID.randomUUID()}",
                            name = trimmedName,
                            email = trimmedEmail,
                            phone = phone.trim(),
                            password = password,
                            role = role,
                            active = active,
                            createdBy = "Admin",
                            createdDate = System.currentTimeMillis(),
                            lastLogin = 0L,
                            updatedDate = System.currentTimeMillis()
                        )
                        repository.insertUser(newUser)
                        onSuccess(trimmedName)
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmployeeDialog(
    employee: User,
    repository: BakeStockRepository,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var name by remember { mutableStateOf(employee.name) }
    var email by remember { mutableStateOf(employee.email) }
    var phone by remember { mutableStateOf(employee.phone) }
    var role by remember { mutableStateOf(employee.role) }
    var active by remember { mutableStateOf(employee.active) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Employee", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = role == UserRole.KITCHEN_MASTER,
                        onClick = { role = UserRole.KITCHEN_MASTER },
                        label = { Text("Kitchen Master") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = role == UserRole.DELIVERY_MAN,
                        onClick = { role = UserRole.DELIVERY_MAN },
                        label = { Text("Delivery Man") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (active) "Active" else "Inactive")
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }

                errorMessage?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty() || email.trim().isEmpty()) {
                        errorMessage = "Name and Email are required."
                        return@Button
                    }
                    coroutineScope.launch {
                        val updated = employee.copy(
                            name = name.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            role = role,
                            active = active,
                            updatedDate = System.currentTimeMillis()
                        )
                        repository.updateUser(updated)
                        onSuccess(updated.name)
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResetPasswordDialog(
    employee: User,
    repository: BakeStockRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reset password for: ${employee.name} (${employee.email})", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text("New Password *") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm New Password *") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isEmpty()) {
                        errorMessage = "Please enter new password."
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    if (newPassword.length < 4) {
                        errorMessage = "Password must be at least 4 characters."
                        return@Button
                    }
                    coroutineScope.launch {
                        val updated = employee.copy(
                            password = newPassword,
                            updatedDate = System.currentTimeMillis()
                        )
                        repository.updateUser(updated)
                        onSuccess()
                    }
                }
            ) {
                Text("Reset Password")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeManagementScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val recipes by repository.allRecipes.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bakery Recipes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recipes) { rec ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = rec.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Yield: ${rec.yieldQuantity} ${rec.yieldUnit.symbol}", style = MaterialTheme.typography.bodySmall)
                        Text(text = rec.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WastageScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val wastageRecords by repository.allWastageRecords.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Wastage Tracker") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SummaryCard(
                title = "Total Wastage Cost",
                value = "$${String.format("%.2f", wastageRecords.sumOf { it.totalWastageValue })}",
                icon = Icons.Default.Delete,
                containerColor = StatusRed.copy(alpha = 0.15f),
                contentColor = StatusRed
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(wastageRecords) { rec ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = rec.productName, fontWeight = FontWeight.Bold)
                                Text(text = "Reason: ${rec.reason.label}", style = MaterialTheme.typography.bodySmall)
                                Text(text = dateFormat.format(Date(rec.date)), style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "${rec.quantity} ${rec.unit.symbol}", fontWeight = FontWeight.Bold)
                                Text(text = "$${String.format("%.2f", rec.totalWastageValue)}", color = StatusRed, fontWeight = FontWeight.Bold)
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
fun ReportsScreen(repository: BakeStockRepository, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedReports by repository.allSavedReports.collectAsState(initial = emptyList())
    val purchasesList by repository.allPurchases.collectAsState(initial = emptyList())
    val lowStockList by repository.lowStockProducts.collectAsState(initial = emptyList())
    val productsList by repository.allProducts.collectAsState(initial = emptyList())
    val wastageList by repository.allWastageRecords.collectAsState(initial = emptyList())

    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedReportForView by remember { mutableStateOf<ReportRecord?>(null) }
    var viewedReportContent by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock & Audit Reports") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Generate & Export Reports to Local Device Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reports are generated directly from live Room database data and saved to app local storage directory (/files/reports/).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportExportTile(
                            title = "Daily Purchase Summary Report",
                            subtitle = "CSV audit log of stock purchases & supplier invoices",
                            onExport = {
                                coroutineScope.launch {
                                    val sb = StringBuilder()
                                    sb.append("BakeStock Inventory System - Daily Purchase Summary Report\n")
                                    sb.append("Generated At: ").append(dateFormat.format(Date())).append("\n\n")
                                    sb.append("InvoiceNumber,SupplierName,TotalAmount,PaymentStatus,PurchaseDate\n")
                                    purchasesList.forEach { p ->
                                        sb.append("${p.invoiceNumber},\"${p.supplierName}\",${p.totalAmount},${p.paymentStatus},${dateFormat.format(Date(p.purchaseDate))}\n")
                                    }
                                    val totalVal = purchasesList.sumOf { it.totalAmount }
                                    sb.append("\nTotal Purchase Count: ").append(purchasesList.size).append("\n")
                                    sb.append("Total Expense Value: $").append(String.format("%.2f", totalVal)).append("\n")

                                    val record = repository.saveReportToLocalStorage(
                                        context = context,
                                        title = "Daily Purchase Summary Report",
                                        reportType = "PURCHASE_SUMMARY",
                                        content = sb.toString(),
                                        format = "CSV"
                                    )
                                    exportStatusMessage = "Report saved to local storage!\nFile: ${record.fileName}\nPath: ${record.filePath}\nSize: ${record.fileSizeKb} KB"
                                }
                            }
                        )

                        ReportExportTile(
                            title = "Low Stock & Reorder Level Report",
                            subtitle = "Real-time list of critical stock & items needing reorder",
                            onExport = {
                                coroutineScope.launch {
                                    val sb = StringBuilder()
                                    sb.append("BakeStock Inventory System - Low Stock & Reorder Level Report\n")
                                    sb.append("Generated At: ").append(dateFormat.format(Date())).append("\n\n")
                                    sb.append("ID,ProductName,Category,Type,CurrentQty,MinStockLevel,ReorderLevel,Unit,Status\n")
                                    lowStockList.forEach { prod ->
                                        sb.append("${prod.id},\"${prod.name}\",\"${prod.category}\",${prod.productType.name},${prod.currentQuantity},${prod.minStockLevel},${prod.reorderLevel},${prod.unit.symbol},${prod.stockStatus.name}\n")
                                    }
                                    sb.append("\nTotal Low Stock Items: ").append(lowStockList.size).append("\n")

                                    val record = repository.saveReportToLocalStorage(
                                        context = context,
                                        title = "Low Stock & Reorder Report",
                                        reportType = "LOW_STOCK",
                                        content = sb.toString(),
                                        format = "CSV"
                                    )
                                    exportStatusMessage = "Report saved to local storage!\nFile: ${record.fileName}\nPath: ${record.filePath}\nSize: ${record.fileSizeKb} KB"
                                }
                            }
                        )

                        ReportExportTile(
                            title = "Product Expiry & FEFO Audit Report",
                            subtitle = "FEFO (First-Expired, First-Out) batch audit",
                            onExport = {
                                coroutineScope.launch {
                                    val sb = StringBuilder()
                                    sb.append("BakeStock Inventory System - Product Expiry & FEFO Audit\n")
                                    sb.append("Generated At: ").append(dateFormat.format(Date())).append("\n\n")
                                    sb.append("ID,ProductName,BatchNumber,StorageLocation,ExpiryDate,Quantity,Unit,StockStatus\n")
                                    productsList.sortedBy { it.expiryDate }.forEach { prod ->
                                        sb.append("${prod.id},\"${prod.name}\",\"${prod.batchNumber}\",\"${prod.storageLocation}\",${dateFormat.format(Date(prod.expiryDate))},${prod.currentQuantity},${prod.unit.symbol},${prod.stockStatus.name}\n")
                                    }

                                    val record = repository.saveReportToLocalStorage(
                                        context = context,
                                        title = "Product Expiry & FEFO Audit",
                                        reportType = "EXPIRY_AUDIT",
                                        content = sb.toString(),
                                        format = "CSV"
                                    )
                                    exportStatusMessage = "Report saved to local storage!\nFile: ${record.fileName}\nPath: ${record.filePath}\nSize: ${record.fileSizeKb} KB"
                                }
                            }
                        )

                        ReportExportTile(
                            title = "Monthly Production & Wastage Audit",
                            subtitle = "Wastage logs, damaged goods value & kitchen efficiency",
                            onExport = {
                                coroutineScope.launch {
                                    val sb = StringBuilder()
                                    sb.append("BakeStock Inventory System - Monthly Production & Wastage Audit\n")
                                    sb.append("Generated At: ").append(dateFormat.format(Date())).append("\n\n")
                                    sb.append("ID,ProductName,BatchNumber,Quantity,Unit,UnitPrice,TotalWastageValue,Reason,Date\n")
                                    wastageList.forEach { w ->
                                        sb.append("${w.id},\"${w.productName}\",\"${w.batchNumber}\",${w.quantity},${w.unit.symbol},${w.unitPrice},${w.totalWastageValue},${w.reason.name},${dateFormat.format(Date(w.date))}\n")
                                    }
                                    val totalLoss = wastageList.sumOf { it.totalWastageValue }
                                    sb.append("\nTotal Wastage Incidents: ").append(wastageList.size).append("\n")
                                    sb.append("Total Financial Loss: $").append(String.format("%.2f", totalLoss)).append("\n")

                                    val record = repository.saveReportToLocalStorage(
                                        context = context,
                                        title = "Monthly Production & Wastage Audit",
                                        reportType = "WASTAGE_AUDIT",
                                        content = sb.toString(),
                                        format = "CSV"
                                    )
                                    exportStatusMessage = "Report saved to local storage!\nFile: ${record.fileName}\nPath: ${record.filePath}\nSize: ${record.fileSizeKb} KB"
                                }
                            }
                        )
                    }
                }


                if (exportStatusMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Report Saved Successfully!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exportStatusMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Saved Reports in Device Local Storage (${savedReports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (savedReports.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No Reports Saved in Local Storage",
                            message = "Tap any report tile above to generate and store a CSV report on this device."
                        )
                    }
                } else {
                    items(savedReports, key = { it.id }) { report ->
                        SavedReportItemCard(
                            report = report,
                            onView = {
                                coroutineScope.launch {
                                    val content = repository.readReportFileContent(report.filePath)
                                    selectedReportForView = report
                                    viewedReportContent = content
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    repository.deleteReportFromLocalStorage(report)
                                    exportStatusMessage = "Report '${report.title}' deleted from local storage."
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (selectedReportForView != null && viewedReportContent != null) {
        AlertDialog(
            onDismissRequest = {
                selectedReportForView = null
                viewedReportContent = null
            },
            title = {
                Column {
                    Text(text = selectedReportForView!!.title, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Local Storage Path: ${selectedReportForView!!.filePath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = viewedReportContent!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedReportForView = null
                    viewedReportContent = null
                }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ReportExportTile(title: String, subtitle: String, onExport: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onExport,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        }
    }
}

@Composable
fun SavedReportItemCard(
    report: ReportRecord,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(report.generatedDate) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(report.generatedDate))
    }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = report.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = report.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = report.fileFormat,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr • ${report.fileSizeKb} KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    TextButton(onClick = onView) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View File")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

