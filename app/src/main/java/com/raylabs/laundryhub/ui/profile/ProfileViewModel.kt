package com.raylabs.laundryhub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val observeShowWhatsAppSettingUseCase: ObserveShowWhatsAppSettingUseCase,
    private val setShowWhatsAppSettingUseCase: SetShowWhatsAppSettingUseCase,
    private val getCacheSizeUseCase: GetCacheSizeUseCase,
    private val clearCacheUseCase: ClearCacheUseCase
) : ViewModel() {


    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState


    init {
        fetchUser()
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

    fun setShowWhatsAppOption(enabled: Boolean) {
        viewModelScope.launch {
            setShowWhatsAppSettingUseCase(enabled)
        }
    }

    fun logOut(onSuccess: () -> Unit) {
        _uiState.update { it.copy(logout = it.logout.loading()) }
        viewModelScope.launch {
            val logout = userUseCase.signOut()
            _uiState.update {
                it.copy(logout = it.logout.success(logout))
            }
            if (logout)
                onSuccess()
        }
    }

    private fun fetchCacheSize() {
        _uiState.update { it.copy(cacheSize = it.cacheSize.loading()) }
        viewModelScope.launch {
            runCatching { getCacheSizeUseCase() }
                .onSuccess { size ->
                    _uiState.update { state ->
                        state.copy(cacheSize = state.cacheSize.success(size))
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(cacheSize = state.cacheSize.error(error.message))
                    }
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
        _uiState.update { it.copy(clearCache = it.clearCache.loading()) }
        viewModelScope.launch {
            runCatching { clearCacheUseCase() }
                .onSuccess { cleared ->
                    _uiState.update { state ->
                        state.copy(
                            clearCache = state.clearCache.success(cleared),
                            showClearCacheDialog = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            clearCache = state.clearCache.error(error.message),
                            showClearCacheDialog = false
                        )
                    }
                }
            fetchCacheSize()
        }
    }
}
