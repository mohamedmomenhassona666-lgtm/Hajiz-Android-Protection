package com.hajiz.app.filtering

import com.hajiz.app.data.BlockedDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainNormalizerTest {
    @Test fun normalizesCaseSchemePortAndTrailingDot() {
        assertEquals("example.com", DomainNormalizer.normalize("HTTPS://Example.COM:443/path."))
    }

    @Test fun normalizesInternationalDomainsToPunycode() {
        assertEquals("xn--bcher-kva.example", DomainNormalizer.normalize("bücher.example"))
    }

    @Test fun rejectsInvalidIpAddress() {
        assertNull(DomainNormalizer.normalize("999.1.1.1"))
    }

    @Test fun matcherCoversSubdomainsWithoutMatchingSimilarDomains() {
        val matcher = BlocklistMatcher()
        val rules = listOf(BlockedDomain(domain = "blocked.example"))
        assertTrue(matcher.isBlocked("blocked.example", rules))
        assertTrue(matcher.isBlocked("www.blocked.example", rules))
        assertTrue(matcher.isBlocked("sub.blocked.example", rules))
        assertTrue(!matcher.isBlocked("blocked.example-safe.com", rules))
    }

    @Test fun wildcardRuleMatchesDescendants() {
        val matcher = BlocklistMatcher()
        val rules = listOf(BlockedDomain(domain = "*.blocked.example"))
        assertTrue(matcher.isBlocked("sub.blocked.example", rules))
    }
}