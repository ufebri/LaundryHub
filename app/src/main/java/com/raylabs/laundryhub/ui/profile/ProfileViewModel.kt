package com.raylabs.laundryhub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.toUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userUseCase: UserUseCase,
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase,
    private val observeShowWhatsAppSettingUseCase: ObserveShowWhatsAppSettingUseCase,
    private val setShowWhatsAppSettingUseCase: SetShowWhatsAppSettingUseCase,
    private val getCacheSizeUseCase: GetCacheSizeUseCase,
    private val clearCacheUseCase: ClearCacheUseCase
) : ViewModel() {


    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState


    init {
        fetchUser()
        observeReminderSettings()
        observeSettings()
        fetchCacheSize()
    }

    private fun fetchUser() {
        val user = userUseCase.getCurrentUser()
        _uiState.value = _uiState.value.copy(
            user = SectionState(data = user?.toUI())
        )
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeShowWhatsAppSettingUseCase().collect { isEnabled ->
                _uiState.update { it.copy(showWhatsAppOption = isEnabled) }
            }
        }
    }

    private fun observeReminderSettings() {
        viewModelScope.launch {
            observeReminderSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(reminderSettings = settings) }
            }
        }
    }

    fun setShowWhatsAppOption(enabled: Boolean) {
        viewModelScope.launch {
            setShowWhatsAppSettingUseCase(enabled)
        }
    }

    fun logOut(onSuccess: () -> Unit) {
        _uiState.update { it.copy(logout = it.logout.loading()) }
        viewModelScope.launch {
            val signedOut = userUseCase.signOut()
            if (signedOut) onSuccess()
        }
    }

    private fun fetchCacheSize() {
        viewModelScope.launch {
            _uiState.update { it.copy(cacheSize = it.cacheSize.loading()) }
            try {
                val size = getCacheSizeUseCase()
                _uiState.update { it.copy(cacheSize = it.cacheSize.success(size)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cacheSize = it.cacheSize.error(e.message ?: "Error")) }
            }
        }
    }

    fun openClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = true) }
    }

    fun dismissClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = false) }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(clearCache = it.clearCache.loading()) }
            val success = clearCacheUseCase()
            _uiState.update { it.copy(clearCache = it.clearCache.success(success), showClearCacheDialog = false) }
            fetchCacheSize()
        }
    }
}
