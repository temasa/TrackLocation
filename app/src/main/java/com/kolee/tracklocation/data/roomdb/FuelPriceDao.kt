package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelPriceDao {

    @Insert
    suspend fun insert(entity: FuelPriceEntity)

    @Query("SELECT * FROM fuel_price ORDER BY effectiveFromMs DESC LIMIT 1")
    fun currentPrice(): Flow<FuelPriceEntity?>

    @Query("SELECT * FROM fuel_price ORDER BY effectiveFromMs DESC LIMIT 1")
    suspend fun currentPriceOnce(): FuelPriceEntity?

    @Query("SELECT * FROM fuel_price WHERE effectiveFromMs <= :ts ORDER BY effectiveFromMs DESC LIMIT 1")
    suspend fun priceEffectiveAt(ts: Long): FuelPriceEntity?

    @Query("SELECT COUNT(*) FROM fuel_price")
    suspend fun count(): Int
}
