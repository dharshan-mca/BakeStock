package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: UserRole): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE (LOWER(email) = LOWER(:identifier) OR LOWER(name) = LOWER(:identifier)) LIMIT 1")
    suspend fun getUserByUsernameOrEmail(identifier: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    suspend fun getAllProductsList(): List<Product>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE active = 1 AND currentQuantity <= minStockLevel")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE active = 1 AND expiryDate <= :thresholdTime")
    fun getNearExpiryProducts(thresholdTime: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE active = 1 AND productType = :type")
    fun getProductsByType(type: ProductType): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET active = 0, lastUpdatedDate = :timestamp WHERE id = :id")
    suspend fun deactivateProduct(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id LIKE 'prod_%'")
    suspend fun deletePredefinedProducts()
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE active = 1 ORDER BY name ASC")
    fun getAllActiveSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: String): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)

    @Update
    suspend fun updateSupplier(supplier: Supplier)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    suspend fun getAllPurchasesList(): List<Purchase>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: String): Purchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase)
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY productName ASC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes ORDER BY productName ASC")
    suspend fun getAllRecipesList(): List<Recipe>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId")
    fun getIngredientsForRecipe(recipeId: String): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun getIngredientsListForRecipe(recipeId: String): List<RecipeIngredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)
}

@Dao
interface ProductionDao {
    @Query("SELECT * FROM production_batches ORDER BY productionDate DESC")
    fun getAllProductionBatches(): Flow<List<ProductionBatch>>

    @Query("SELECT * FROM production_batches ORDER BY productionDate DESC")
    suspend fun getAllProductionBatchesList(): List<ProductionBatch>

    @Query("SELECT * FROM production_batches WHERE id = :id LIMIT 1")
    suspend fun getProductionBatchById(id: String): ProductionBatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductionBatch(batch: ProductionBatch)

    @Update
    suspend fun updateProductionBatch(batch: ProductionBatch)
}

@Dao
interface ProductionHistoryDao {
    @Query("SELECT * FROM production_history ORDER BY timestamp DESC")
    fun getAllProductionHistory(): Flow<List<ProductionHistory>>

    @Query("SELECT * FROM production_history ORDER BY timestamp DESC")
    suspend fun getAllProductionHistoryList(): List<ProductionHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductionHistory(history: ProductionHistory)
}

@Dao
interface RequestDao {
    @Query("SELECT * FROM ingredient_requests ORDER BY requestDate DESC")
    fun getAllIngredientRequests(): Flow<List<IngredientRequest>>

    @Query("SELECT * FROM ingredient_requests ORDER BY requestDate DESC")
    suspend fun getAllRequestsList(): List<IngredientRequest>

    @Query("SELECT * FROM ingredient_requests WHERE targetDeliveryManId = :deliveryManId OR targetDeliveryManId = '' ORDER BY requestDate DESC")
    fun getRequestsForDelivery(deliveryManId: String): Flow<List<IngredientRequest>>

    @Query("SELECT * FROM ingredient_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): IngredientRequest?

    @Query("SELECT * FROM request_items WHERE requestId = :requestId")
    fun getRequestItems(requestId: String): Flow<List<IngredientRequestItem>>

    @Query("SELECT * FROM request_items WHERE requestId = :requestId")
    suspend fun getRequestItemsList(requestId: String): List<IngredientRequestItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: IngredientRequest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestItems(items: List<IngredientRequestItem>)

    @Update
    suspend fun updateRequest(request: IngredientRequest)

    @Update
    suspend fun updateRequestItem(item: IngredientRequestItem)
}

@Dao
interface WastageDao {
    @Query("SELECT * FROM wastage_records ORDER BY date DESC")
    fun getAllWastageRecords(): Flow<List<WastageRecord>>

    @Query("SELECT * FROM wastage_records ORDER BY date DESC")
    suspend fun getAllWastageRecordsList(): List<WastageRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWastageRecord(record: WastageRecord)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<ActivityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)
}

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions WHERE productId = :productId ORDER BY timestamp DESC")
    fun getTransactionsForProduct(productId: String): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllStockTransactions(): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<StockTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransaction)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM saved_reports ORDER BY generatedDate DESC")
    fun getAllSavedReports(): Flow<List<ReportRecord>>

    @Query("SELECT * FROM saved_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): ReportRecord?

    @Query("SELECT * FROM saved_reports WHERE reportType = :type ORDER BY generatedDate DESC")
    fun getReportsByType(type: String): Flow<List<ReportRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportRecord)

    @Query("DELETE FROM saved_reports WHERE id = :id")
    suspend fun deleteReport(id: String)
}

