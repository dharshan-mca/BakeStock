package com.example.data.model

enum class ProductionShift(val label: String) {
    MORNING("Morning Shift"),
    AFTERNOON("Afternoon Shift"),
    EVENING("Evening Shift")
}

data class IngredientShortage(
    val ingredientId: String,
    val ingredientName: String,
    val requiredQuantity: Double,
    val availableQuantity: Double,
    val unit: UnitOfMeasure,
    val isShortage: Boolean
)

data class SmartReorderRecommendation(
    val product: Product,
    val currentStock: Double,
    val reorderThreshold: Double,
    val suggestedReorderQuantity: Double,
    val estimatedDailyUsage: Double,
    val estimatedDaysRemaining: Double,
    val supplierName: String
)

data class AnalyticsSummary(
    val totalStockValuation: Double,
    val monthlyWastageValuation: Double,
    val totalPurchasesMonth: Double,
    val inStockCount: Int,
    val lowStockCount: Int,
    val nearExpiryCount: Int,
    val expiredCount: Int,
    val wastageByReason: Map<WastageReason, Double>,
    val categoryDistribution: Map<String, Int>
)
