package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowlistRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AllowlistRuleEntity)

    @Update
    suspend fun update(rule: AllowlistRuleEntity)

    @Query("SELECT * FROM allowlist_rule ORDER BY createdAt ASC")
    fun getAllRules(): Flow<List<AllowlistRuleEntity>>

    @Query("SELECT * FROM allowlist_rule WHERE enabled = 1 ORDER BY createdAt ASC")
    suspend fun getEnabledRules(): List<AllowlistRuleEntity>

    @Query("DELETE FROM allowlist_rule WHERE id = :id")
    suspend fun deleteById(id: String)
}
