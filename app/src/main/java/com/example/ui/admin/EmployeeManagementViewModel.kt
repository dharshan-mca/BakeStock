package com.example.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.EmployeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EmployeeSortOption(val label: String) {
    NAME_ASC("Name (A - Z)"),
    NAME_DESC("Name (Z - A)"),
    NEWEST("Newest First"),
    OLDEST("Oldest First")
}

class EmployeeManagementViewModel(
    private val repository: EmployeeRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val roleFilter = MutableStateFlow<UserRole?>(null) // null = ALL
    val statusFilter = MutableStateFlow<Boolean?>(null) // null = ALL
    val sortOption = MutableStateFlow(EmployeeSortOption.NEWEST)

    val syncStatusMessage = MutableStateFlow("Initializing Realtime Cloud Sync...")
    val uiMessage = MutableStateFlow<String?>(null)

    init {
        repository.startRealtimeEmployeeSync { status ->
            syncStatusMessage.value = status
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopRealtimeEmployeeSync()
    }

    val filteredEmployees: StateFlow<List<User>> = combine(
        repository.allEmployees,
        searchQuery,
        roleFilter,
        statusFilter,
        sortOption
    ) { employees, query, role, status, sort ->
        employees
            .filter { emp ->
                // Filter out Admin accounts from standard employee staff list
                emp.role != UserRole.ADMIN
            }
            .filter { emp ->
                val q = query.trim().lowercase()
                q.isEmpty() || emp.name.lowercase().contains(q) ||
                        emp.email.lowercase().contains(q) ||
                        emp.phone.contains(q)
            }
            .filter { emp ->
                role == null || emp.role == role
            }
            .filter { emp ->
                status == null || emp.active == status
            }
            .let { list ->
                when (sort) {
                    EmployeeSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                    EmployeeSortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                    EmployeeSortOption.NEWEST -> list.sortedByDescending { it.createdDate }
                    EmployeeSortOption.OLDEST -> list.sortedBy { it.createdDate }
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createEmployee(
        context: Context,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        password: String,
        active: Boolean,
        adminName: String
    ) {
        viewModelScope.launch {
            repository.createEmployee(
                context, name, email, phone, role, password, active, adminName
            ) { success, message ->
                uiMessage.value = message
            }
        }
    }

    fun updateEmployee(user: User, adminName: String) {
        viewModelScope.launch {
            repository.updateEmployee(user, adminName) { success, message ->
                uiMessage.value = message
            }
        }
    }

    fun toggleEmployeeStatus(user: User, newStatus: Boolean, adminName: String) {
        viewModelScope.launch {
            repository.toggleEmployeeStatus(user, newStatus, adminName) { success, message ->
                uiMessage.value = message
            }
        }
    }

    fun deleteEmployee(user: User, adminName: String) {
        viewModelScope.launch {
            repository.deleteEmployee(user, adminName) { success, message ->
                uiMessage.value = message
            }
        }
    }

    fun sendPasswordReset(user: User, adminName: String) {
        repository.sendPasswordReset(user.email, user.name, adminName) { success, message ->
            uiMessage.value = message
        }
    }

    fun clearUiMessage() {
        uiMessage.value = null
    }
}
