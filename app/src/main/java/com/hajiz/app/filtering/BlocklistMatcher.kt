package com.hajiz.app.filtering

import com.hajiz.app.data.BlockedDomain

class BlocklistMatcher {
    fun isBlocked(host: String, rules: Collection<BlockedDomain>): Boolean {
        val normalizedHost = DomainNormalizer.normalize(host) ?: return false
        return rules.asSequence()
            .filter { it.enabled }
            .mapNotNull { DomainNormalizer.normalize(it.domain)?.let { normalized -> normalized to it.domain.trim() } }
            .any { (rule, rawRule) ->
                val wildcard = rawRule.trim().startsWith("*.")
                normalizedHost == rule ||
                    (normalizedHost.endsWith(".$rule") && (wildcard || !rule.contains('/')))
            }
    }
}