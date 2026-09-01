package com.hajiz.app.filtering

import com.hajiz.app.data.BlockedDomain
import kotlinx.coroutines.flow.Flow

interface BlocklistProvider {
    fun observeBlockedDomains(): Flow<List<BlockedDomain>>
    suspend fun seedSafeDefaults()
}