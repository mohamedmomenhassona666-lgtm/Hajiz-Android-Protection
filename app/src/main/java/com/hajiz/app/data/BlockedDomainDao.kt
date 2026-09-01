package com.hajiz.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDomainDao {
    @Query("SELECT * FROM blocked_domains WHERE enabled = 1 ORDER BY domain")
    fun observeEnabled(): Flow<List<BlockedDomain>>

    @Query("SELECT * FROM blocked_domains ORDER BY domain")
    fun observeAll(): Flow<List<BlockedDomain>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlockedDomain>)

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun count(): Int
}