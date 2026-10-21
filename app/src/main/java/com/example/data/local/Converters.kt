package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try { UserRole.valueOf(value) } catch (e: Exception) { UserRole.ADMIN }

    @TypeConverter
    fun fromProductType(value: ProductType): String = value.name

    @TypeConverter
    fun toProductType(value: String): ProductType = try { ProductType.valueOf(value) } catch (e: Exception) { ProductType.GROCERY }

    @TypeConverter
    fun fromUnitOfMeasure(value: UnitOfMeasure): String = value.name

    @TypeConverter
    fun toUnitOfMeasure(value: String): UnitOfMeasure = try { UnitOfMeasure.valueOf(value) } catch (e: Exception) { UnitOfMeasure.KG }

    @TypeConverter
    fun fromStockStatus(value: StockStatus): String = value.name

    @TypeConverter
    fun toStockStatus(value: String): StockStatus = try { StockStatus.valueOf(value) } catch (e: Exception) { StockStatus.IN_STOCK }

    @TypeConverter
    fun fromRequestStatus(value: RequestStatus): String = value.name

    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus = try { RequestStatus.valueOf(value) } catch (e: Exception) { RequestStatus.PENDING }

    @TypeConverter
    fun fromRequestPriority(value: RequestPriority): String = value.name

    @TypeConverter
    fun toRequestPriority(value: String): RequestPriority = try { RequestPriority.valueOf(value) } catch (e: Exception) { RequestPriority.MEDIUM }

    @TypeConverter
    fun fromProductionStatus(value: ProductionStatus): String = value.name

    @TypeConverter
    fun toProductionStatus(value: String): ProductionStatus = try { ProductionStatus.valueOf(value) } catch (e: Exception) { ProductionStatus.PLANNED }

    @TypeConverter
    fun fromWastageReason(value: WastageReason): String = value.name

    @TypeConverter
    fun toWastageReason(value: String): WastageReason = try { WastageReason.valueOf(value) } catch (e: Exception) { WastageReason.EXPIRED }
}
