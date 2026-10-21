package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        User::class,
        Product::class,
        Supplier::class,
        Purchase::class,
        Recipe::class,
        RecipeIngredient::class,
        ProductionBatch::class,
        ProductionHistory::class,
        IngredientRequest::class,
        IngredientRequestItem::class,
        WastageRecord::class,
        NotificationItem::class,
        ActivityLog::class,
        StockTransaction::class,
        ReportRecord::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BakeStockDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun recipeDao(): RecipeDao
    abstract fun productionDao(): ProductionDao
    abstract fun productionHistoryDao(): ProductionHistoryDao
    abstract fun requestDao(): RequestDao
    abstract fun wastageDao(): WastageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: BakeStockDatabase? = null

        fun getDatabase(context: Context): BakeStockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BakeStockDatabase::class.java,
                    "bakestock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
