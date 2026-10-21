package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: EmployeeManagementViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val employees by viewModel.filteredEmployees.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRole by viewModel.roleFilter.collectAsState()
    val selectedStatus by viewModel.statusFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<User?>(null) }
    var employeeToDelete by remember { mutableStateOf<User?>(null) }
    var employeeProfileDetails by remember { mutableStateOf<User?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Employee Management", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Realtime Firestore & Room Cache",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("employee_management_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Live Sync",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    employeeToEdit = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Employee", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_employee_fab")
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Realtime Sync Status Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = syncStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_employee_input"),
                placeholder = { Text("Search by name, email or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter & Sorting Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Role Filters
                item {
                    FilterChip(
                        selected = selectedRole == null,
                        onClick = { viewModel.roleFilter.value = null },
                        label = { Text("All Roles") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedRole == UserRole.KITCHEN_MASTER,
                        onClick = { viewModel.roleFilter.value = UserRole.KITCHEN_MASTER },
                        label = { Text("Kitchen Master") },
                        leadingIcon = { Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedRole == UserRole.DELIVERY_MAN,
                        onClick = { viewModel.roleFilter.value = UserRole.DELIVERY_MAN },
                        label = { Text("Delivery Man") },
                        leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Status Filters
                item {
                    FilterChip(
                        selected = selectedStatus == true,
                        onClick = { viewModel.statusFilter.value = if (selectedStatus == true) null else true },
                        label = { Text("Active") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatus == false,
                        onClick = { viewModel.statusFilter.value = if (selectedStatus == false) null else false },
                        label = { Text("Inactive") },
                        leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section Header & Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Members (${employees.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Sorting dropdown selector
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sortOption.label, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        EmployeeSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.sortOption.value = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Employee List
            if (employees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No employee profiles found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Click '+' to add new staff members.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(employees, key = { it.id }) { employee ->
                        EmployeeItemCard(
                            user = employee,
                            onClick = { employeeProfileDetails = employee },
                            onEdit = {
                                employeeToEdit = employee
                                showAddEditDialog = true
                            },
                            onToggleStatus = {
                                viewModel.toggleEmployeeStatus(employee, !employee.active, "Admin")
                            },
                            onResetPassword = {
                                viewModel.sendPasswordReset(employee, "Admin")
                            },
                            onDelete = {
                                employeeToDelete = employee
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Add/Edit Employee Dialog
    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            employee = employeeToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, email, phone, role, password, active ->
                if (employeeToEdit == null) {
                    viewModel.createEmployee(context, name, email, phone, role, password, active, "Admin")
                } else {
                    val updated = employeeToEdit!!.copy(
                        name = name,
                        email = email,
                        phone = phone,
                        role = role,
                        active = active
                    )
                    viewModel.updateEmployee(updated, "Admin")
                }
                showAddEditDialog = false
            }
        )
    }

    // Modal Confirm Delete Dialog
    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete Employee Profile?") },
            text = { Text("Are you sure you want to permanently delete '${employeeToDelete?.name}' from Firestore and local cache?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        employeeToDelete?.let { viewModel.deleteEmployee(it, "Admin") }
                        employeeToDelete = null
                    }
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Profile Details Sheet/Dialog
    if (employeeProfileDetails != null) {
        EmployeeProfileDialog(
            user = employeeProfileDetails!!,
            onDismiss = { employeeProfileDetails = null },
            onResetPassword = {
                viewModel.sendPasswordReset(employeeProfileDetails!!, "Admin")
            },
            onToggleStatus = {
                viewModel.toggleEmployeeStatus(employeeProfileDetails!!, !employeeProfileDetails!!.active, "Admin")
                employeeProfileDetails = employeeProfileDetails!!.copy(active = !employeeProfileDetails!!.active)
            }
        )
    }
}

@Composable
fun EmployeeItemCard(
    user: User,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("employee_item_${user.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (user.role == UserRole.KITCHEN_MASTER) Icons.Default.SoupKitchen else Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (user.role == UserRole.KITCHEN_MASTER) "Kitchen Master" else "Delivery Man",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (user.active) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (user.active) "ACTIVE" else "INACTIVE",
                        color = if (user.active) StatusGreen else StatusRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (user.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = user.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added: " + dateFormat.format(Date(user.createdDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Employee", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onResetPassword, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.LockReset, contentDescription = "Reset Password", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleStatus, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (user.active) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = if (user.active) "Deactivate" else "Activate",
                            tint = if (user.active) StatusRed else StatusGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Employee", tint = StatusRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditEmployeeDialog(
    employee: User?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, role: UserRole, password: String, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var email by remember { mutableStateOf(employee?.email ?: "") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var password by remember { mutableStateOf("bakery123") }
    var selectedRole by remember { mutableStateOf(employee?.role ?: UserRole.KITCHEN_MASTER) }
    var active by remember { mutableStateOf(employee?.active ?: true) }

    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employee == null) "Add New Employee" else "Edit Employee Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("employee_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = employee == null, // email locked on edit
                    modifier = Modifier.fillMaxWidth().testTag("employee_email_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (employee == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Initial Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("employee_password_input")
                    )
                }

                Text("Employee Role *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == UserRole.KITCHEN_MASTER,
                        onClick = { selectedRole = UserRole.KITCHEN_MASTER },
                        label = { Text("Kitchen Master") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == UserRole.DELIVERY_MAN,
                        onClick = { selectedRole = UserRole.DELIVERY_MAN },
                        label = { Text("Delivery Man") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Account Status", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || (employee == null && password.isBlank())) {
                        errorMessage = "Please fill in all required fields (*)."
                    } else {
                        onSave(name.trim(), email.trim(), phone.trim(), selectedRole, password.trim(), active)
                    }
                },
                modifier = Modifier.testTag("save_employee_button")
            ) {
                Text(if (employee == null) "Create Account" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmployeeProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Employee Profile Details", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (user.role == UserRole.KITCHEN_MASTER) "Kitchen Master" else "Delivery Man",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                DetailItem(label = "Firebase UID", value = user.id)
                DetailItem(label = "Email Address", value = user.email)
                DetailItem(label = "Phone Contact", value = if (user.phone.isNotBlank()) user.phone else "Not specified")
                DetailItem(label = "Account Status", value = if (user.active) "ACTIVE" else "INACTIVE")
                DetailItem(label = "Provisioned By", value = user.createdBy)
                DetailItem(label = "Created Date", value = dateFormat.format(Date(user.createdDate)))
                DetailItem(label = "Last Login", value = dateFormat.format(Date(user.lastLogin)))
            }
        },
        confirmButton = {
            Button(onClick = onResetPassword) {
                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Password Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
