package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.BakeStockDatabase
import com.example.data.model.ActivityLog
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class EmployeeRepository(
    private val db: BakeStockDatabase
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allEmployees: Flow<List<User>> = db.userDao().getAllUsers()

    fun startRealtimeEmployeeSync(onStatusUpdate: ((String) -> Unit)? = null) {
        onStatusUpdate?.invoke("Local Database Synchronized")
    }

    fun stopRealtimeEmployeeSync() {}

    suspend fun createEmployee(
        context: Context,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        password: String,
        active: Boolean,
        adminName: String,
        onComplete: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val existing = db.userDao().getUserByEmail(email)
            if (existing != null) {
                onComplete(false, "An account with email $email already exists.")
                return@withContext
            }

            val uid = "emp_" + UUID.randomUUID().toString().take(8)
            val now = System.currentTimeMillis()
            val user = User(
                id = uid,
                name = name,
                email = email,
                role = role,
                phone = phone,
                password = password,
                active = active,
                createdBy = adminName,
                createdDate = now,
                lastLogin = now,
                updatedDate = now
            )

            db.userDao().insertUser(user)
            db.activityLogDao().insertLog(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userName = adminName,
                    userRole = UserRole.ADMIN,
                    action = "Created Employee Account",
                    recordAffected = "$name (${role.name})",
                    previousQuantity = null,
                    updatedQuantity = null,
                    timestamp = now
                )
            )

            onComplete(true, "Employee account created successfully for $name.")
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Failed to create employee")
        }
    }

    suspend fun updateEmployee(
        user: User,
        adminName: String,
        onComplete: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val updatedUser = user.copy(updatedDate = System.currentTimeMillis())
            db.userDao().updateUser(updatedUser)
            db.activityLogDao().insertLog(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userName = adminName,
                    userRole = UserRole.ADMIN,
                    action = "Updated Employee Details",
                    recordAffected = user.name,
                    previousQuantity = null,
                    updatedQuantity = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            onComplete(true, "Employee profile updated successfully.")
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Failed to update employee")
        }
    }

    suspend fun toggleEmployeeStatus(
        user: User,
        newStatus: Boolean,
        adminName: String,
        onComplete: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val updatedUser = user.copy(active = newStatus, updatedDate = System.currentTimeMillis())
            db.userDao().updateUser(updatedUser)
            val statusText = if (newStatus) "Activated" else "Deactivated"
            db.activityLogDao().insertLog(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userName = adminName,
                    userRole = UserRole.ADMIN,
                    action = "$statusText Employee",
                    recordAffected = user.name,
                    previousQuantity = null,
                    updatedQuantity = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            onComplete(true, "Employee ${user.name} is now ${if (newStatus) "ACTIVE" else "INACTIVE"}.")
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Failed to update status")
        }
    }

    suspend fun deleteEmployee(
        user: User,
        adminName: String,
        onComplete: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            db.userDao().deleteUserById(user.id)
            db.activityLogDao().insertLog(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userName = adminName,
                    userRole = UserRole.ADMIN,
                    action = "Deleted Employee Account",
                    recordAffected = user.name,
                    previousQuantity = null,
                    updatedQuantity = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            onComplete(true, "Employee ${user.name} removed successfully.")
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Failed to delete employee")
        }
    }

    fun sendPasswordReset(
        email: String,
        employeeName: String,
        adminName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        repositoryScope.launch {
            try {
                db.activityLogDao().insertLog(
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        userName = adminName,
                        userRole = UserRole.ADMIN,
                        action = "Sent Password Reset Notification",
                        recordAffected = "$employeeName ($email)",
                        previousQuantity = null,
                        updatedQuantity = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
                onComplete(true, "Password reset instructions recorded for $email.")
            } catch (e: Exception) {
                onComplete(false, "Failed to send reset: ${e.localizedMessage}")
            }
        }
    }
}
