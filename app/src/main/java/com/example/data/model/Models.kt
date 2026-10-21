package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    KITCHEN_MASTER,
    DELIVERY_MAN
}

enum class ProductType(val label: String) {
    GROCERY("Grocery Item"),
    BAKING_RAW_MATERIAL("Baking Raw Material"),
    PACKAGING_MATERIAL("Packaging Material"),
    PERISHABLE("Perishable Ingredient"),
    FINISHED_BAKERY_PRODUCT("Finished Bakery Product")
}

enum class UnitOfMeasure(val symbol: String, val label: String) {
    KG("kg", "Kilogram"),
    GRAM("g", "Gram"),
    LITRE("L", "Litre"),
    MILLILITRE("ml", "Millilitre"),
    PACKET("pkt", "Packet"),
    BOX("box", "Box"),
    PIECE("pc", "Piece"),
    TRAY("tray", "Tray"),
    BAG("bag", "Bag")
}

enum class StockStatus(val label: String) {
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    NEAR_EXPIRY("Nearing Expiry"),
    EXPIRED("Expired")
}

enum class RequestStatus(val label: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    PARTIALLY_COLLECTED("Partially Collected"),
    FULLY_COLLECTED("Fully Collected"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class RequestPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class ProductionStatus(val label: String) {
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class WastageReason(val label: String) {
    EXPIRED("Expired"),
    DAMAGED("Damaged"),
    SPOILED("Spoiled"),
    PRODUCTION_REJECT("Production Reject")
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val phone: String = "",
    val password: String = "admin@2026",
    val active: Boolean = true,
    val createdBy: String = "Admin",
    val createdDate: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val productType: ProductType,
    val currentQuantity: Double,
    val unit: UnitOfMeasure,
    val minStockLevel: Double,
    val reorderLevel: Double,
    val purchasePrice: Double,
    val supplierName: String = "",
    val supplierContact: String = "",
    val batchNumber: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val mfgDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val storageLocation: String = "Main Pantry",
    val imageUrl: String = "",
    val stockStatus: StockStatus = StockStatus.IN_STOCK,
    val createdDate: Long = System.currentTimeMillis(),
    val lastUpdatedDate: Long = System.currentTimeMillis(),
    val active: Boolean = true
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey val id: String,
    val name: String,
    val contactNumber: String,
    val address: String,
    val suppliedItems: String, // comma separated items
    val paymentDetails: String,
    val active: Boolean = true
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val supplierId: String,
    val supplierName: String,
    val purchaseDate: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paymentStatus: String, // Paid, Pending, Partial
    val billImageUrl: String = "",
    val itemsSummary: String = "", // JSON or summarized text
    val createdBy: String = "Admin"
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String,
    val productName: String,
    val finishedProductId: String,
    val category: String = "Bakery",
    val description: String = "",
    val estimatedPreparationTime: Int = 15, // in minutes
    val estimatedBakeTime: Int = 30, // in minutes
    val yieldQuantity: Double = 1.0,
    val yieldUnit: UnitOfMeasure = UnitOfMeasure.PIECE,
    val instructions: String = "",
    val status: String = "ACTIVE",
    val createdBy: String = "Admin",
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "recipe_ingredients")
data class RecipeIngredient(
    @PrimaryKey val id: String,
    val recipeId: String,
    val rawMaterialId: String,
    val rawMaterialName: String,
    val requiredQuantity: Double,
    val unit: UnitOfMeasure
)

@Entity(tableName = "production_batches")
data class ProductionBatch(
    @PrimaryKey val id: String,
    val batchNumber: String,
    val recipeId: String = "",
    val productId: String,
    val productName: String,
    val plannedQuantity: Double,
    val producedQuantity: Double = 0.0,
    val damagedQuantity: Double = 0.0,
    val productionDate: Long = System.currentTimeMillis(),
    val completedTime: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val status: ProductionStatus = ProductionStatus.PLANNED,
    val notes: String = "",
    val createdBy: String = "Kitchen Master"
)

@Entity(tableName = "production_history")
data class ProductionHistory(
    @PrimaryKey val id: String,
    val batchId: String,
    val recipeName: String,
    val quantityProduced: Double,
    val operator: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Long,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ingredient_requests")
data class IngredientRequest(
    @PrimaryKey val id: String,
    val requestNumber: String,
    val requestedBy: String,
    val requestedByName: String,
    val targetDeliveryManId: String = "",
    val targetDeliveryManName: String = "",
    val priority: RequestPriority = RequestPriority.MEDIUM,
    val notes: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val requestDate: Long = System.currentTimeMillis(),
    val billOrProductImageUrl: String = ""
)

@Entity(tableName = "request_items")
data class IngredientRequestItem(
    @PrimaryKey val id: String,
    val requestId: String,
    val productId: String,
    val productName: String,
    val requestedQuantity: Double,
    val availableStock: Double,
    val collectedQuantity: Double = 0.0,
    val unit: UnitOfMeasure,
    val note: String = "",
    val isCollected: Boolean = false
)

@Entity(tableName = "wastage_records")
data class WastageRecord(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val batchNumber: String = "",
    val quantity: Double,
    val unit: UnitOfMeasure,
    val unitPrice: Double = 0.0,
    val totalWastageValue: Double = 0.0,
    val reason: WastageReason,
    val date: Long = System.currentTimeMillis(),
    val recordedBy: String = "Kitchen Master"
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String = "INFO", // LOW_STOCK, EXPIRY, REQUEST, BATCH, PRODUCTION, DELIVERY, REPORT, INFO, SUCCESS, WARNING, ERROR, CRITICAL
    val module: String = "GENERAL", // INVENTORY, PRODUCTION, DELIVERY, REPORTS, EMPLOYEE, AUTH, GENERAL
    val referenceId: String = "",
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val receiverUid: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val createdDate: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetRole: UserRole? = null
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String,
    val userName: String,
    val userRole: UserRole,
    val action: String,
    val recordAffected: String,
    val previousQuantity: Double? = null,
    val updatedQuantity: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val transactionType: String, // STOCK_IN, STOCK_OUT, PRODUCTION_DEDUCT, WASTAGE, PURCHASE
    val quantity: Double,
    val unit: UnitOfMeasure,
    val reason: String = "",
    val batchNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val performedBy: String
)

@Entity(tableName = "saved_reports")
data class ReportRecord(
    @PrimaryKey val id: String,
    val title: String,
    val reportType: String,
    val fileName: String,
    val filePath: String,
    val fileFormat: String = "CSV",
    val summaryText: String,
    val fileSizeKb: Double = 0.0,
    val generatedDate: Long = System.currentTimeMillis(),
    val generatedBy: String = "Admin"
)

