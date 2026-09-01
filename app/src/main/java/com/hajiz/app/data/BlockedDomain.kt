package com.hajiz.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DomainCategory {
    ADULT,
    EXPLICIT,
    GAMBLING,
    DATING,
    CUSTOM,
}

@Entity(tableName = "blocked_domains")
data class BlockedDomain(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val category: String = DomainCategory.ADULT.name.lowercase(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)