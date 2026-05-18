package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowlist_rule")
data class AllowlistRuleEntity(
    @PrimaryKey val id: String,
    val matchType: String,
    val pattern: String,
    val enabled: Boolean,
    val createdAt: Long
)
