package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BakeStockDatabase
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BakeStockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BakeStockDatabase.getDatabase(application)
    val repository = BakeStockRepository(db)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSeedDataIfEmpty()
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun loginAdmin(username: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val trimmedUser = username.trim()
            if ((trimmedUser.equals("admin", ignoreCase = true) || trimmedUser.equals("admin@bakestock.com", ignoreCase = true)) && pass == "admin@2026") {
                val adminUser = repository.getUserByUsernameOrEmail("admin")
                    ?: User("user_admin", "Master Bakery Admin", "admin", UserRole.ADMIN, password = "admin@2026")
                _currentUser.value = adminUser
                _authState.value = AuthState.Authenticated(adminUser)
            } else {
                _authState.value = AuthState.Error("Invalid username or password. Please try again.")
            }
        }
    }

    fun loginStaff(usernameOrEmail: String, pass: String, role: UserRole) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val trimmedIdentifier = usernameOrEmail.trim()
            if (trimmedIdentifier.isEmpty() || pass.isEmpty()) {
                _authState.value = AuthState.Error("Please enter both username/email and password.")
                return@launch
            }

            val user = repository.getUserByUsernameOrEmail(trimmedIdentifier)
            if (user == null) {
                _authState.value = AuthState.Error("Account not found. Please contact the Administrator to create your account.")
            } else if (user.role != role) {
                _authState.value = AuthState.Error("You are not authorized to login using this portal.")
            } else if (!user.active) {
                _authState.value = AuthState.Error("Your account is currently inactive. Please contact Administrator.")
            } else if (user.password != pass) {
                _authState.value = AuthState.Error("Incorrect password. Please try again.")
            } else {
                val updatedUser = user.copy(lastLogin = System.currentTimeMillis())
                repository.updateUser(updatedUser)
                _currentUser.value = updatedUser
                _authState.value = AuthState.Authenticated(updatedUser)
            }
        }
    }

    fun registerStaff(name: String, usernameOrEmail: String, pass: String, confirmPass: String, role: UserRole) {
        _authState.value = AuthState.Error("Self-registration is disabled. Please contact the Administrator to create your account.")
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun updateProfile(name: String, phone: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = current.copy(name = name, phone = phone)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }
}
