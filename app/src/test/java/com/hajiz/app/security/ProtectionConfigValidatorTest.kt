package com.hajiz.app.security

import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionConfigValidatorTest {
    @Test fun strictModeRequiresPin() {
        val errors = ProtectionConfigValidator.validate(
            ProtectionConfiguration(
                strictMode = true,
                accountabilityMode = false,
                hasPin = false,
                partnerName = "",
            ),
        )
        assertTrue(errors.any { it.contains("PIN") })
    }

    @Test fun accountabilityNeedsPartnerName() {
        val errors = ProtectionConfigValidator.validate(
            ProtectionConfiguration(
                strictMode = false,
                accountabilityMode = true,
                hasPin = true,
                partnerName = "",
            ),
        )
        assertTrue(errors.isNotEmpty())
    }
}