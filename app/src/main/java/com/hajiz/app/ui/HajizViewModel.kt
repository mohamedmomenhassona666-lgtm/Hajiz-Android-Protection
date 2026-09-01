package com.hajiz.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hajiz.app.data.ProtectionSettings
import com.hajiz.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class HajizViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<ProtectionSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProtectionSettings(),
    )
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _vpnActive = MutableStateFlow(false)
    val vpnActive: StateFlow<Boolean> = _vpnActive.asStateFlow()

    fun setVpnActive(value: Boolean) {
        _vpnActive.value = value
    }

    fun startProtection() = viewModelScope.launch {
        repository.setProtectionEnabled(true)
        repository.recordProtectionCheck()
    }

    fun stopProtection() = viewModelScope.launch {
        repository.setProtectionEnabled(false)
    }

    fun reportVpnPermissionDenied() {
        _message.value = "VPN permission is needed before protection can start."
    }

    fun clearMessage() {
        _message.value = null
    }

    fun completeOnboarding() = viewModelScope.launch {
        repository.setOnboardingComplete(true)
        repository.setProtectionEnabled(false)
    }

    fun setStrictMode(value: Boolean) = viewModelScope.launch { repository.setStrictMode(value) }
    fun setBlockAdultContent(value: Boolean) = viewModelScope.launch { repository.setBlockAdultContent(value) }
    fun setBlockExplicitSearch(value: Boolean) = viewModelScope.launch { repository.setBlockExplicitSearch(value) }
    fun setAccountabilityMode(value: Boolean) = viewModelScope.launch { repository.setAccountabilityMode(value) }
    fun setProtectSettings(value: Boolean) = viewModelScope.launch { repository.setProtectSettings(value) }
    fun setEmergencyAccess(value: Boolean) = viewModelScope.launch { repository.setEmergencyAccess(value) }
    fun setPartnerName(value: String) = viewModelScope.launch { repository.setPartnerName(value) }
    fun addUrgeModeUse() = viewModelScope.launch { repository.incrementUrgeModeUses() }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HajizViewModel(repository) as T
    }
}