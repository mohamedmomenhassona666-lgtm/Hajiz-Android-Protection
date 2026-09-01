package com.hajiz.app.security

data class ProtectionConfiguration(
    val strictMode: Boolean,
    val accountabilityMode: Boolean,
    val hasPin: Boolean,
    val partnerName: String,
)

object ProtectionConfigValidator {
    fun validate(configuration: ProtectionConfiguration): List<String> = buildList {
        if (configuration.strictMode && !configuration.hasPin) {
            add("Strict Mode requires a protection PIN.")
        }
        if (configuration.accountabilityMode && configuration.partnerName.isBlank()) {
            add("Accountability Mode needs a trusted partner name.")
        }
    }
}