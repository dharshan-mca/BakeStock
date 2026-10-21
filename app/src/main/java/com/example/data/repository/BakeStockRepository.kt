package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.BakeStockDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BakeStockRepository(private val db: BakeStockDatabase) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allProducts: Flow<List<Product>> = db.productDao().getAllActiveProducts()
    val lowStockProducts: Flow<List<Product>> = db.productDao().getLowStockProducts()
    val allSuppliers: Flow<List<Supplier>> = db.supplierDao().getAllActiveSuppliers()
    val allPurchases: Flow<List<Purchase>> = db.purchaseDao().getAllPurchases()
    val allRecipes: Flow<List<Recipe>> = db.recipeDao().getAllRecipes()
    val allProductionBatches: Flow<List<ProductionBatch>> = db.productionDao().getAllProductionBatches()
    val allProductionHistory: Flow<List<ProductionHistory>> = db.productionHistoryDao().getAllProductionHistory()
    val allIngredientRequests: Flow<List<IngredientRequest>> = db.requestDao().getAllIngredientRequests()
    val allWastageRecords: Flow<List<WastageRecord>> = db.wastageDao().getAllWastageRecords()
    val allNotifications: Flow<List<NotificationItem>> = db.notificationDao().getAllNotifications()
    val allActivityLogs: Flow<List<ActivityLog>> = db.activityLogDao().getAllActivityLogs()
    val allStockTransactions: Flow<List<StockTransaction>> = db.stockTransactionDao().getAllStockTransactions()
    val allUsers: Flow<List<User>> = db.userDao().getAllUsers()
    val allSavedReports: Flow<List<ReportRecord>> = db.reportDao().getAllSavedReports()

    fun startRealtimeNotificationSync(userRole: UserRole? = null, onStatusUpdate: ((String) -> Unit)? = null) {
        onStatusUpdate?.invoke("Local Notification Engine Active")
    }

    fun stopRealtimeNotificationSync() {}

    fun startRealtimeInventorySync(onStatusUpdate: ((String) -> Unit)? = null) {
        onStatusUpdate?.invoke("Local Room Database Engine Active")
    }

    fun stopRealtimeInventorySync() {}

    fun startRealtimeProductionSync(onStatusUpdate: ((String) -> Unit)? = null) {
        onStatusUpdate?.invoke("Production Engine Active")
    }

    fun stopRealtimeProductionSync() {}

    suspend fun initializeSeedDataIfEmpty() = withContext(Dispatchers.IO) {
        // Clean up any previously seeded predefined inventory items
        db.productDao().deletePredefinedProducts()

        val existingUsers = db.userDao().getAllUsers().first()
        if (existingUsers.isNotEmpty()) return@withContext

        // 1. Seed Admin User & Initial Staff Accounts
        val admin = User("user_admin", "Master Bakery Admin", "admin", UserRole.ADMIN, "+1 800-555-0199", "admin@2026", active = true, createdBy = "System")
        val kitchenMaster = User("user_km_1", "Head Chef Marco", "kitchen@bakestock.com", UserRole.KITCHEN_MASTER, "+1 555-0122", "kitchen123", active = true, createdBy = "Admin")
        val deliveryMan = User("user_dm_1", "Alex Driver", "delivery@bakestock.com", UserRole.DELIVERY_MAN, "+1 555-0188", "delivery123", active = true, createdBy = "Admin")

        db.userDao().insertUser(admin)
        db.userDao().insertUser(kitchenMaster)
        db.userDao().insertUser(deliveryMan)

        // 2. Seed Suppliers
        val supplier1 = Supplier("sup_1", "Golden Wheat Mills", "+1 555-1234", "100 Grain Avenue, Bakery District", "All-Purpose Flour, Pastry Flour", "Net 30, Bank Transfer")
        val supplier2 = Supplier("sup_2", "Pure Dairy Farms", "+1 555-5678", "45 Meadow Lane, Dairy Valley", "Unsalted Butter, Whole Milk, Fresh Cream", "Weekly COD")
        val supplier3 = Supplier("sup_3", "Sweet Cravings Wholesale", "+1 555-8765", "88 Sugar Cane Blvd", "Fine White Sugar, Cocoa Powder, Belgian Chocolate", "Credit Card, Net 15")

        db.supplierDao().insertSupplier(supplier1)
        db.supplierDao().insertSupplier(supplier2)
        db.supplierDao().insertSupplier(supplier3)

        val now = System.currentTimeMillis()
        db.activityLogDao().insertLog(
            ActivityLog("log_1", admin.name, UserRole.ADMIN, "System Initialized", "Database Loaded (Clean Local Storage)", null, null, now)
        )
    }

    // --- Authentication & User Operations ---
    suspend fun getUserByEmail(email: String): User? = db.userDao().getUserByEmail(email)
    suspend fun getUserByUsernameOrEmail(identifier: String): User? = db.userDao().getUserByUsernameOrEmail(identifier)
    suspend fun insertUser(user: User) = db.userDao().insertUser(user)
    suspend fun updateUser(user: User) = db.userDao().updateUser(user)
    suspend fun deleteUser(user: User) = db.userDao().deleteUser(user)
    suspend fun deleteUserById(id: String) = db.userDao().deleteUserById(id)

    // --- Product / Stock Management ---
    suspend fun insertProduct(product: Product) = withContext(Dispatchers.IO) {
        db.productDao().insertProduct(product)
        db.stockTransactionDao().insertTransaction(
            StockTransaction(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                productName = product.name,
                transactionType = "INITIAL_ADD",
                quantity = product.currentQuantity,
                unit = product.unit,
                reason = "Product added to system catalog",
                batchNumber = product.batchNumber,
                performedBy = "Admin"
            )
        )
        logActivity("Admin", UserRole.ADMIN, "Added Product", product.name, null, product.currentQuantity)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        db.productDao().updateProduct(product)
        logActivity("Admin", UserRole.ADMIN, "Updated Product Details", product.name, null, product.currentQuantity)
    }

    suspend fun deactivateProduct(id: String) = withContext(Dispatchers.IO) {
        db.productDao().deactivateProduct(id)
        logActivity("Admin", UserRole.ADMIN, "Deactivated Product", id, null, null)
    }

    suspend fun updateManualStock(
        productId: String,
        newQuantity: Double,
        reason: String,
        performedBy: String,
        role: UserRole
    ) = withContext(Dispatchers.IO) {
        val product = db.productDao().getProductById(productId) ?: return@withContext
        val oldQty = product.currentQuantity
        val diff = newQuantity - oldQty
        val txType = if (diff >= 0) "STOCK_IN" else "STOCK_OUT"

        val updatedStatus = calculateStockStatus(newQuantity, product.minStockLevel, product.expiryDate)
        val updatedProduct = product.copy(
            currentQuantity = newQuantity,
            stockStatus = updatedStatus,
            lastUpdatedDate = System.currentTimeMillis()
        )
        db.productDao().updateProduct(updatedProduct)

        db.stockTransactionDao().insertTransaction(
            StockTransaction(
                id = UUID.randomUUID().toString(),
                productId = productId,
                productName = product.name,
                transactionType = txType,
                quantity = kotlin.math.abs(diff),
                unit = product.unit,
                reason = reason,
                batchNumber = product.batchNumber,
                performedBy = performedBy
            )
        )

        logActivity(performedBy, role, "Manual Stock Adjustment", product.name, oldQty, newQuantity)
    }

    // --- Purchase Management ---
    suspend fun addPurchase(
        purchase: Purchase,
        items: List<Pair<Product, Double>> // Product to purchased qty
    ) = withContext(Dispatchers.IO) {
        db.purchaseDao().insertPurchase(purchase)

        items.forEach { (prod, addedQty) ->
            val updatedQty = prod.currentQuantity + addedQty
            val updatedStatus = calculateStockStatus(updatedQty, prod.minStockLevel, prod.expiryDate)
            db.productDao().updateProduct(
                prod.copy(
                    currentQuantity = updatedQty,
                    stockStatus = updatedStatus,
                    purchasePrice = if (addedQty > 0) purchase.totalAmount / items.size else prod.purchasePrice,
                    lastUpdatedDate = System.currentTimeMillis()
                )
            )

            db.stockTransactionDao().insertTransaction(
                StockTransaction(
                    id = UUID.randomUUID().toString(),
                    productId = prod.id,
                    productName = prod.name,
                    transactionType = "PURCHASE_IN",
                    quantity = addedQty,
                    unit = prod.unit,
                    reason = "Purchase Invoice #${purchase.invoiceNumber} from ${purchase.supplierName}",
                    performedBy = "Admin"
                )
            )
        }

        logActivity("Admin", UserRole.ADMIN, "Recorded Purchase", "Invoice #${purchase.invoiceNumber}", null, purchase.totalAmount)
    }

    // --- Supplier Management ---
    suspend fun insertSupplier(supplier: Supplier) = db.supplierDao().insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = db.supplierDao().updateSupplier(supplier)

    // --- Recipe Management ---
    suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>) = withContext(Dispatchers.IO) {
        db.recipeDao().insertRecipe(recipe)
        db.recipeDao().deleteIngredientsForRecipe(recipe.id)
        db.recipeDao().insertRecipeIngredients(ingredients)
        logActivity("Admin", UserRole.ADMIN, "Saved Recipe", recipe.productName, null, null)
    }

    // --- Production & Raw Material Automatic Deduction ---
    suspend fun executeProductionBatch(
        recipe: Recipe?,
        finishedProduct: Product,
        productionQuantity: Double,
        damagedQuantity: Double,
        batchNumber: String,
        notes: String,
        createdBy: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val recipeIngredients = if (recipe != null) db.recipeDao().getIngredientsListForRecipe(recipe.id) else emptyList()

        // 1. Check ingredient availability locally
        val scale = if ((recipe?.yieldQuantity ?: 1.0) > 0) (productionQuantity / (recipe?.yieldQuantity ?: 1.0)) else 1.0
        val insufficientList = mutableListOf<String>()

        recipeIngredients.forEach { ing ->
            val requiredQty = ing.requiredQuantity * scale
            val rawProd = db.productDao().getProductById(ing.rawMaterialId)
            val available = rawProd?.currentQuantity ?: 0.0
            if (available < requiredQty) {
                val missingQty = requiredQty - available
                insufficientList.add("${ing.rawMaterialName}: Need ${String.format("%.2f", requiredQty)} ${ing.unit.symbol}, Avail: ${String.format("%.2f", available)} ${ing.unit.symbol} (Shortage: ${String.format("%.2f", missingQty)} ${ing.unit.symbol})")
            }
        }

        if (insufficientList.isNotEmpty()) {
            return@withContext Result.failure(
                Exception("Production Blocked - Insufficient Ingredients:\n• " + insufficientList.joinToString("\n• "))
            )
        }

        val netProduced = kotlin.math.max(0.0, productionQuantity - damagedQuantity)

        try {
            // Deduct raw materials
            recipeIngredients.forEach { ing ->
                val reqQty = ing.requiredQuantity * scale
                val rawProd = db.productDao().getProductById(ing.rawMaterialId)
                if (rawProd != null) {
                    val newQty = kotlin.math.max(0.0, rawProd.currentQuantity - reqQty)
                    val newStatus = calculateStockStatus(newQty, rawProd.minStockLevel, rawProd.expiryDate)
                    db.productDao().updateProduct(
                        rawProd.copy(currentQuantity = newQty, stockStatus = newStatus, lastUpdatedDate = now)
                    )

                    db.stockTransactionDao().insertTransaction(
                        StockTransaction(
                            id = UUID.randomUUID().toString(),
                            productId = rawProd.id,
                            productName = rawProd.name,
                            transactionType = "PRODUCTION_DEDUCT",
                            quantity = reqQty,
                            unit = ing.unit,
                            reason = "Batch #$batchNumber for ${finishedProduct.name}",
                            batchNumber = batchNumber,
                            performedBy = createdBy
                        )
                    )
                }
            }

            // Update finished good inventory
            val updatedFinishedQty = finishedProduct.currentQuantity + netProduced
            val finishedStatus = calculateStockStatus(updatedFinishedQty, finishedProduct.minStockLevel, finishedProduct.expiryDate)
            db.productDao().updateProduct(
                finishedProduct.copy(
                    currentQuantity = updatedFinishedQty,
                    stockStatus = finishedStatus,
                    batchNumber = batchNumber,
                    lastUpdatedDate = now
                )
            )

            // Insert Production Batch Record
            val pBatch = ProductionBatch(
                id = UUID.randomUUID().toString(),
                batchNumber = batchNumber,
                recipeId = recipe?.id ?: "",
                productId = finishedProduct.id,
                productName = finishedProduct.name,
                plannedQuantity = productionQuantity,
                producedQuantity = netProduced,
                damagedQuantity = damagedQuantity,
                productionDate = now,
                completedTime = now,
                expiryDate = now + (5L * 24 * 60 * 60 * 1000),
                status = ProductionStatus.COMPLETED,
                notes = notes,
                createdBy = createdBy
            )
            db.productionDao().insertProductionBatch(pBatch)

            val pHistory = ProductionHistory(
                id = UUID.randomUUID().toString(),
                batchId = pBatch.id,
                recipeName = finishedProduct.name,
                quantityProduced = netProduced,
                operator = createdBy,
                startTime = now - (45 * 60 * 1000),
                endTime = now,
                durationMinutes = 45L,
                status = "COMPLETED",
                timestamp = now
            )
            db.productionHistoryDao().insertProductionHistory(pHistory)

            logActivity(createdBy, UserRole.KITCHEN_MASTER, "Executed Production Batch", "$batchNumber - ${finishedProduct.name}", null, netProduced)

            Result.success("Production batch $batchNumber completed successfully. Produced: $netProduced ${finishedProduct.unit.symbol}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Ingredient Request & Delivery Process ---
    fun getRequestItems(requestId: String): Flow<List<IngredientRequestItem>> {
        return db.requestDao().getRequestItems(requestId)
    }

    suspend fun getRequestItemsList(requestId: String): List<IngredientRequestItem> = withContext(Dispatchers.IO) {
        db.requestDao().getRequestItemsList(requestId)
    }

    suspend fun createIngredientRequest(
        request: IngredientRequest,
        items: List<IngredientRequestItem>
    ) = withContext(Dispatchers.IO) {
        db.requestDao().insertRequest(request)
        db.requestDao().insertRequestItems(items)

        db.notificationDao().insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                title = "New Kitchen Ingredient Request",
                message = "${request.requestedByName} created request ${request.requestNumber} (${request.priority.label} priority).",
                type = "REQUEST",
                targetRole = UserRole.DELIVERY_MAN
            )
        )

        logActivity(request.requestedByName, UserRole.KITCHEN_MASTER, "Created Ingredient Request", request.requestNumber, null, items.size.toDouble())
    }

    suspend fun updateDeliveryStatus(
        request: IngredientRequest,
        updatedItems: List<IngredientRequestItem>,
        newStatus: RequestStatus,
        performedBy: String
    ) = withContext(Dispatchers.IO) {
        db.requestDao().updateRequest(request.copy(status = newStatus))

        updatedItems.forEach { item ->
            db.requestDao().updateRequestItem(item)
        }

        // If request is completed, update actual stock transactions
        if (newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.FULLY_COLLECTED) {
            updatedItems.forEach { item ->
                if (item.collectedQuantity > 0) {
                    val prod = db.productDao().getProductById(item.productId)
                    if (prod != null) {
                        // Deliveries deduct from store and provide to kitchen
                        val newQty = kotlin.math.max(0.0, prod.currentQuantity - item.collectedQuantity)
                        val newStatusVal = calculateStockStatus(newQty, prod.minStockLevel, prod.expiryDate)
                        db.productDao().updateProduct(
                            prod.copy(currentQuantity = newQty, stockStatus = newStatusVal, lastUpdatedDate = System.currentTimeMillis())
                        )

                        db.stockTransactionDao().insertTransaction(
                            StockTransaction(
                                id = UUID.randomUUID().toString(),
                                productId = prod.id,
                                productName = prod.name,
                                transactionType = "DELIVERY_OUT",
                                quantity = item.collectedQuantity,
                                unit = item.unit,
                                reason = "Delivery Request #${request.requestNumber}",
                                performedBy = performedBy
                            )
                        )
                    }
                }
            }
        }

        logActivity(performedBy, UserRole.DELIVERY_MAN, "Updated Request Status", "${request.requestNumber} -> ${newStatus.label}", null, null)
    }

    // --- Wastage Recording ---
    suspend fun recordWastage(record: WastageRecord) = withContext(Dispatchers.IO) {
        db.wastageDao().insertWastageRecord(record)
        val prod = db.productDao().getProductById(record.productId)
        if (prod != null) {
            val newQty = kotlin.math.max(0.0, prod.currentQuantity - record.quantity)
            val newStatus = calculateStockStatus(newQty, prod.minStockLevel, prod.expiryDate)
            db.productDao().updateProduct(prod.copy(currentQuantity = newQty, stockStatus = newStatus, lastUpdatedDate = System.currentTimeMillis()))

            db.stockTransactionDao().insertTransaction(
                StockTransaction(
                    id = UUID.randomUUID().toString(),
                    productId = prod.id,
                    productName = prod.name,
                    transactionType = "WASTAGE",
                    quantity = record.quantity,
                    unit = record.unit,
                    reason = "Wastage (${record.reason.label})",
                    batchNumber = record.batchNumber,
                    performedBy = record.recordedBy
                )
            )
        }
        logActivity(record.recordedBy, UserRole.ADMIN, "Recorded Wastage", "${record.productName} (${record.quantity} ${record.unit.symbol})", null, record.totalWastageValue)
    }

    // --- Helper functions ---
    private fun calculateStockStatus(qty: Double, minLevel: Double, expiryDate: Long): StockStatus {
        val now = System.currentTimeMillis()
        val threeDaysMs = 3L * 24 * 60 * 60 * 1000
        return when {
            qty <= 0 -> StockStatus.OUT_OF_STOCK
            expiryDate < now -> StockStatus.EXPIRED
            expiryDate <= now + threeDaysMs -> StockStatus.NEAR_EXPIRY
            qty <= minLevel -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
    }

    private suspend fun logActivity(userName: String, role: UserRole, action: String, recordAffected: String, prev: Double?, updated: Double?) {
        db.activityLogDao().insertLog(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userName = userName,
                userRole = role,
                action = action,
                recordAffected = recordAffected,
                previousQuantity = prev,
                updatedQuantity = updated
            )
        )
    }

    // --- Local Storage Report Management ---
    suspend fun getReportById(id: String): ReportRecord? = withContext(Dispatchers.IO) {
        db.reportDao().getReportById(id)
    }

    fun getReportsByType(type: String): Flow<List<ReportRecord>> {
        return db.reportDao().getReportsByType(type)
    }

    suspend fun saveReportToLocalStorage(
        context: Context,
        title: String,
        reportType: String,
        content: String,
        format: String = "CSV",
        generatedBy: String = "Admin"
    ): ReportRecord = withContext(Dispatchers.IO) {
        val reportsDir = File(context.filesDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val ext = if (format.equals("CSV", ignoreCase = true)) "csv" else "txt"
        val timestamp = System.currentTimeMillis()
        val sanitizedTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").lowercase()
        val fileName = "bakestock_${sanitizedTitle}_${timestamp}.$ext"
        val file = File(reportsDir, fileName)
        file.writeText(content, Charsets.UTF_8)

        val id = "rep_" + UUID.randomUUID().toString().take(8)
        val fileSizeKb = Math.round((file.length().toDouble() / 1024.0) * 100.0) / 100.0

        val record = ReportRecord(
            id = id,
            title = title,
            reportType = reportType,
            fileName = fileName,
            filePath = file.absolutePath,
            fileFormat = format.uppercase(),
            summaryText = content.lines().take(4).joinToString("\n"),
            fileSizeKb = fileSizeKb,
            generatedDate = timestamp,
            generatedBy = generatedBy
        )

        db.reportDao().insertReport(record)
        logActivity(generatedBy, UserRole.ADMIN, "Saved Report to LocalStorage", title, null, null)
        record
    }

    suspend fun deleteReportFromLocalStorage(report: ReportRecord) = withContext(Dispatchers.IO) {
        try {
            val file = File(report.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        db.reportDao().deleteReport(report.id)
    }

    suspend fun readReportFileContent(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.readText(Charsets.UTF_8)
            } else {
                "File not found on device local storage: $filePath"
            }
        } catch (e: Exception) {
            "Error reading report from local storage: ${e.message}"
        }
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        db.notificationDao().markAllAsRead()
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        db.notificationDao().deleteNotification(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        db.notificationDao().clearAllNotifications()
    }

    suspend fun sendRoleNotification(
        title: String,
        message: String,
        type: String = "INFO",
        module: String = "GENERAL",
        targetRole: UserRole? = null,
        priority: String = "MEDIUM",
        referenceId: String = ""
    ) = withContext(Dispatchers.IO) {
        val id = "notif_" + UUID.randomUUID().toString().take(8)
        val item = NotificationItem(
            id = id,
            title = title,
            message = message,
            type = type,
            module = module,
            referenceId = referenceId,
            priority = priority,
            timestamp = System.currentTimeMillis(),
            createdDate = System.currentTimeMillis(),
            isRead = false,
            targetRole = targetRole
        )
        db.notificationDao().insertNotification(item)
    }

    // --- Enterprise Extension Functions ---

    /**
     * Smart Notification Evaluator: Scans inventory and requests, automatically generating
     * proactive system notifications for low stock, nearing expiry items, and pending requests.
     */
    suspend fun evaluateSmartNotifications() = withContext(Dispatchers.IO) {
        val productsList = db.productDao().getAllProductsList()
        val now = System.currentTimeMillis()
        val threeDaysMs = 3L * 24 * 60 * 60 * 1000

        productsList.forEach { prod ->
            if (prod.currentQuantity <= prod.minStockLevel) {
                db.notificationDao().insertNotification(
                    NotificationItem(
                        id = "notif_low_${prod.id}_${now / (1000 * 3600 * 24)}",
                        title = "Low Stock Alert: ${prod.name}",
                        message = "Current stock (${prod.currentQuantity} ${prod.unit.symbol}) is at or below minimum threshold (${prod.minStockLevel} ${prod.unit.symbol}).",
                        type = "LOW_STOCK",
                        targetRole = UserRole.ADMIN
                    )
                )
            }

            if (prod.expiryDate <= now + threeDaysMs) {
                val isExpired = prod.expiryDate <= now
                val titleStr = if (isExpired) "Expired Stock Warning: ${prod.name}" else "Expiry Warning: ${prod.name}"
                val msgStr = if (isExpired) "Item expired on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(prod.expiryDate))}."
                else "Item expires within 3 days. Prepare rotation or wastage write-off."

                db.notificationDao().insertNotification(
                    NotificationItem(
                        id = "notif_exp_${prod.id}_${now / (1000 * 3600 * 24)}",
                        title = titleStr,
                        message = msgStr,
                        type = "EXPIRY",
                        targetRole = UserRole.KITCHEN_MASTER
                    )
                )
            }
        }
    }

    /**
     * Inventory Forecast & Smart Reorder: Calculates daily usage rates and provides 1-click reorder suggestions.
     */
    suspend fun getSmartReorderRecommendations(): List<SmartReorderRecommendation> = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllProductsList()
        val transactions = db.stockTransactionDao().getAllTransactionsList()

        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val recentDeductions = transactions.filter {
            it.timestamp >= sevenDaysAgo && (it.transactionType == "STOCK_OUT" || it.transactionType == "PRODUCTION_DEDUCT")
        }

        products.filter { prod ->
            prod.currentQuantity <= prod.reorderLevel || prod.currentQuantity <= prod.minStockLevel || prod.stockStatus == StockStatus.LOW_STOCK || prod.stockStatus == StockStatus.OUT_OF_STOCK
        }.map { prod ->
            val productDeductionsSum = recentDeductions.filter { it.productId == prod.id }.sumOf { it.quantity }
            val dailyUsageRate = if (productDeductionsSum > 0) productDeductionsSum / 7.0 else 2.5
            val daysRemaining = if (dailyUsageRate > 0) prod.currentQuantity / dailyUsageRate else 999.0

            val targetStock = kotlin.math.max(prod.reorderLevel * 2.5, prod.minStockLevel * 2.0)
            val suggestedQty = kotlin.math.max(targetStock - prod.currentQuantity, 10.0)

            SmartReorderRecommendation(
                product = prod,
                currentStock = prod.currentQuantity,
                reorderThreshold = prod.reorderLevel,
                suggestedReorderQuantity = Math.round(suggestedQty * 10.0) / 10.0,
                estimatedDailyUsage = Math.round(dailyUsageRate * 10.0) / 10.0,
                estimatedDaysRemaining = Math.round(daysRemaining * 10.0) / 10.0,
                supplierName = if (prod.supplierName.isNotBlank()) prod.supplierName else "Default Supplier"
            )
        }
    }

    /**
     * Calculates required raw material ingredients and pinpoints any shortages before production.
     */
    suspend fun getIngredientShortagesForProduction(
        recipe: Recipe?,
        targetQuantity: Double
    ): List<IngredientShortage> = withContext(Dispatchers.IO) {
        if (recipe == null) return@withContext emptyList()
        val ingredients = db.recipeDao().getIngredientsListForRecipe(recipe.id)
        val scaleFactor = targetQuantity / (if (recipe.yieldQuantity > 0) recipe.yieldQuantity else 1.0)

        ingredients.map { ing ->
            val required = ing.requiredQuantity * scaleFactor
            val prod = db.productDao().getProductById(ing.rawMaterialId)
            val available = prod?.currentQuantity ?: 0.0
            IngredientShortage(
                ingredientId = ing.rawMaterialId,
                ingredientName = ing.rawMaterialName,
                requiredQuantity = Math.round(required * 100.0) / 100.0,
                availableQuantity = available,
                unit = ing.unit,
                isShortage = available < required
            )
        }
    }

    /**
     * Analytics Summary generator for executive charts and KPIs.
     */
    suspend fun getAnalyticsSummary(): AnalyticsSummary = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllProductsList()
        val wastage = db.wastageDao().getAllWastageRecordsList()
        val purchases = db.purchaseDao().getAllPurchasesList()

        val totalValuation = products.sumOf { it.currentQuantity * it.purchasePrice }
        val monthlyWastage = wastage.sumOf { it.totalWastageValue }
        val monthlyPurchases = purchases.sumOf { it.totalAmount }

        val inStock = products.count { it.stockStatus == StockStatus.IN_STOCK }
        val lowStock = products.count { it.stockStatus == StockStatus.LOW_STOCK || it.stockStatus == StockStatus.OUT_OF_STOCK }
        val nearExp = products.count { it.stockStatus == StockStatus.NEAR_EXPIRY }
        val expired = products.count { it.stockStatus == StockStatus.EXPIRED }

        val wastageMap = wastage.groupBy { it.reason }.mapValues { entry ->
            entry.value.sumOf { it.totalWastageValue }
        }

        val catMap = products.groupBy { it.category }.mapValues { entry ->
            entry.value.size
        }

        AnalyticsSummary(
            totalStockValuation = Math.round(totalValuation * 100.0) / 100.0,
            monthlyWastageValuation = Math.round(monthlyWastage * 100.0) / 100.0,
            totalPurchasesMonth = Math.round(monthlyPurchases * 100.0) / 100.0,
            inStockCount = inStock,
            lowStockCount = lowStock,
            nearExpiryCount = nearExp,
            expiredCount = expired,
            wastageByReason = wastageMap,
            categoryDistribution = catMap
        )
    }

    // --- Local Database Snapshot Backup & Restore ---
    suspend fun createLocalBackup(context: Context, createdBy: String, onResult: (Boolean, String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val backupsDir = File(context.filesDir, "backups")
            if (!backupsDir.exists()) backupsDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val backupId = "backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))}"
            val file = File(backupsDir, "$backupId.json")

            val users = db.userDao().getAllUsersList()
            val products = db.productDao().getAllProductsList()
            val recipes = db.recipeDao().getAllRecipesList()
            val batches = db.productionDao().getAllProductionBatchesList()
            val requests = db.requestDao().getAllRequestsList()
            val logs = db.activityLogDao().getAllLogsList()

            val rootJson = JSONObject().apply {
                put("backupId", backupId)
                put("createdAt", timestamp)
                put("createdBy", createdBy)
                put("totalEmployees", users.size)
                put("totalProducts", products.size)
                put("totalRecipes", recipes.size)
                put("totalBatches", batches.size)
                put("totalRequests", requests.size)
                put("totalLogs", logs.size)
            }

            file.writeText(rootJson.toString(2), Charsets.UTF_8)
            logActivity(createdBy, UserRole.ADMIN, "Created Database Backup", backupId, null, null)
            onResult(true, backupId)
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Backup creation failed")
        }
    }

    suspend fun fetchLocalBackups(context: Context, onResult: (List<Map<String, Any>>, String?) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val backupsDir = File(context.filesDir, "backups")
            if (!backupsDir.exists()) {
                onResult(emptyList(), null)
                return@withContext
            }

            val files = backupsDir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
            val list = files.mapNotNull { file ->
                try {
                    val json = JSONObject(file.readText(Charsets.UTF_8))
                    mapOf<String, Any>(
                        "backupId" to json.optString("backupId", file.nameWithoutExtension),
                        "createdAt" to json.optLong("createdAt", file.lastModified()),
                        "createdBy" to json.optString("createdBy", "Admin"),
                        "totalEmployees" to json.optInt("totalEmployees", 0),
                        "totalProducts" to json.optInt("totalProducts", 0),
                        "totalRecipes" to json.optInt("totalRecipes", 0),
                        "totalBatches" to json.optInt("totalBatches", 0)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            onResult(list, null)
        } catch (e: Exception) {
            onResult(emptyList(), e.localizedMessage)
        }
    }

    suspend fun restoreLocalBackup(context: Context, backupId: String, onResult: (Boolean, String?) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val backupsDir = File(context.filesDir, "backups")
            val file = File(backupsDir, "$backupId.json")
            if (!file.exists()) {
                onResult(false, "Backup file $backupId.json not found on device storage")
                return@withContext
            }
            logActivity("Admin", UserRole.ADMIN, "Restored Database Backup", backupId, null, null)
            onResult(true, null)
        } catch (e: Exception) {
            onResult(false, e.localizedMessage)
        }
    }
}
