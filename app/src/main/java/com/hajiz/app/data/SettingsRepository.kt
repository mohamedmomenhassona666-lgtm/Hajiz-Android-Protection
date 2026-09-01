package com.hajiz.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hajizDataStore by preferencesDataStore(name = "hajiz_settings")

data class ProtectionSettings(
    val protectionEnabled: Boolean = false,
    val strictMode: Boolean = false,
    val blockAdultContent: Boolean = true,
    val blockExplicitSearch: Boolean = true,
    val accountabilityMode: Boolean = false,
    val protectSettings: Boolean = false,
    val emergencyAccess: Boolean = true,
    val onboardingComplete: Boolean = false,
    val blockedAttemptsToday: Int = 0,
    val protectedDays: Int = 0,
    val urgeModeUses: Int = 0,
    val lastProtectionCheck: Long = 0,
    val lastStatsDay: String = "",
    val partnerName: String = "",
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val protectionEnabled = booleanPreferencesKey("protection_enabled")
        val strictMode = booleanPreferencesKey("strict_mode")
        val blockAdultContent = booleanPreferencesKey("block_adult_content")
        val blockExplicitSearch = booleanPreferencesKey("block_explicit_search")
        val accountabilityMode = booleanPreferencesKey("accountability_mode")
        val protectSettings = booleanPreferencesKey("protect_settings")
        val emergencyAccess = booleanPreferencesKey("emergency_access")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val blockedAttemptsToday = intPreferencesKey("blocked_attempts_today")
        val protectedDays = intPreferencesKey("protected_days")
        val urgeModeUses = intPreferencesKey("urge_mode_uses")
        val lastProtectionCheck = longPreferencesKey("last_protection_check")
        val lastStatsDay = stringPreferencesKey("last_stats_day")
        val lastProtectedDay = stringPreferencesKey("last_protected_day")
        val partnerName = stringPreferencesKey("partner_name")
    }

    val settings: Flow<ProtectionSettings> = context.hajizDataStore.data.map { p ->
        ProtectionSettings(
            protectionEnabled = p[Keys.protectionEnabled] ?: false,
            strictMode = p[Keys.strictMode] ?: false,
            blockAdultContent = p[Keys.blockAdultContent] ?: true,
            blockExplicitSearch = p[Keys.blockExplicitSearch] ?: true,
            accountabilityMode = p[Keys.accountabilityMode] ?: false,
            protectSettings = p[Keys.protectSettings] ?: false,
            emergencyAccess = p[Keys.emergencyAccess] ?: true,
            onboardingComplete = p[Keys.onboardingComplete] ?: false,
            blockedAttemptsToday = p[Keys.blockedAttemptsToday] ?: 0,
            protectedDays = p[Keys.protectedDays] ?: 0,
            urgeModeUses = p[Keys.urgeModeUses] ?: 0,
            lastProtectionCheck = p[Keys.lastProtectionCheck] ?: 0,
            lastStatsDay = p[Keys.lastStatsDay] ?: "",
            partnerName = p[Keys.partnerName] ?: "",
        )
    }

    suspend fun setProtectionEnabled(value: Boolean) = update { it[Keys.protectionEnabled] = value }
    suspend fun setStrictMode(value: Boolean) = update { it[Keys.strictMode] = value }
    suspend fun setBlockAdultContent(value: Boolean) = update { it[Keys.blockAdultContent] = value }
    suspend fun setBlockExplicitSearch(value: Boolean) = update { it[Keys.blockExplicitSearch] = value }
    suspend fun setAccountabilityMode(value: Boolean) = update { it[Keys.accountabilityMode] = value }
    suspend fun setProtectSettings(value: Boolean) = update { it[Keys.protectSettings] = value }
    suspend fun setEmergencyAccess(value: Boolean) = update { it[Keys.emergencyAccess] = value }
    suspend fun setOnboardingComplete(value: Boolean) = update { it[Keys.onboardingComplete] = value }
    suspend fun setPartnerName(value: String) = update { it[Keys.partnerName] = value }
    suspend fun incrementUrgeModeUses() = update { it[Keys.urgeModeUses] = (it[Keys.urgeModeUses] ?: 0) + 1 }

    suspend fun recordBlockedAttempt() = update {
        val today = java.time.LocalDate.now().toString()
        if (it[Keys.lastStatsDay] != today) {
            it[Keys.lastStatsDay] = today
            it[Keys.blockedAttemptsToday] = 1
        } else {
            it[Keys.blockedAttemptsToday] = (it[Keys.blockedAttemptsToday] ?: 0) + 1
        }
    }

    suspend fun recordProtectionCheck() = update {
        it[Keys.lastProtectionCheck] = System.currentTimeMillis()
        val today = java.time.LocalDate.now().toString()
        if (it[Keys.lastProtectedDay] != today) {
            it[Keys.lastProtectedDay] = today
            it[Keys.protectedDays] = (it[Keys.protectedDays] ?: 0) + 1
        }
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.hajizDataStore.edit(block)
    }
}