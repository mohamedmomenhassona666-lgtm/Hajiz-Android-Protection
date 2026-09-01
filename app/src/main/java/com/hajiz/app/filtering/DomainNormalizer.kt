package com.hajiz.app.filtering

import java.net.IDN
import java.util.Locale

object DomainNormalizer {
    fun normalize(value: String): String? {
        var candidate = value.trim().lowercase(Locale.ROOT)
        if (candidate.isEmpty()) return null
        candidate = candidate.removePrefix("*.")
        candidate = candidate.substringAfter("://", candidate)
        candidate = candidate.substringBefore('/').substringBefore('?').substringBefore('#')
        if (candidate.startsWith('[') && candidate.contains(']')) {
            candidate = candidate.substring(1, candidate.indexOf(']'))
        } else if (candidate.count { it == ':' } == 1) {
            candidate = candidate.substringBefore(':')
        }
        candidate = candidate.trimEnd('.')
        if (candidate.isEmpty()) return null

        return try {
            if (candidate.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
                if (candidate.split('.').all { it.toIntOrNull() in 0..255 }) candidate else null
            } else {
                IDN.toASCII(candidate, IDN.USE_STD3_ASCII_RULES)
                    .lowercase(Locale.ROOT)
                    .takeIf { it.length <= 253 && it.split('.').all(String::isNotEmpty) }
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}