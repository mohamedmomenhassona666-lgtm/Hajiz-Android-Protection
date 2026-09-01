package com.hajiz.app.filtering

import com.hajiz.app.data.BlockedDomain
import com.hajiz.app.data.BlockedDomainDao
import kotlinx.coroutines.flow.Flow

class LocalBlocklistProvider(private val dao: BlockedDomainDao) : BlocklistProvider {
    override fun observeBlockedDomains(): Flow<List<BlockedDomain>> = dao.observeEnabled()

    override suspend fun seedSafeDefaults() {
        if (dao.count() == 0) {
            dao.insertAll(
                listOf(
                    BlockedDomain(domain = "blocked.example"),
                    BlockedDomain(domain = "adult-test.example"),
                ),
            )
        }
    }
}