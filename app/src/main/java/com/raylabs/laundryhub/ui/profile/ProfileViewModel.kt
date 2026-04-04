package com.raylabs.laundryhub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SaveSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ValidateSpreadsheetUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.toUI
import com.raylabs.laundryhub.ui.profile.state.toUi
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
    private val clearCacheUseCase: ClearCacheUseCase,
    private val observeSpreadsheetConfigUseCase: ObserveSpreadsheetConfigUseCase,
    private val clearSpreadsheetConnectionUseCase: ClearSpreadsheetConnectionUseCase,
    private val saveSpreadsheetConnectionUseCase: SaveSpreadsheetConnectionUseCase,
    private val validateSpreadsheetUseCase: ValidateSpreadsheetUseCase
) : ViewModel() {


    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState


    init {
        fetchUser()
        observeSettings()
        observeSpreadsheetConfig()
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

    private fun observeSpreadsheetConfig() {
        viewModelScope.launch {
            observeSpreadsheetConfigUseCase().collect { config ->
                _uiState.update {
                    it.copy(
                        connectedSpreadsheet = it.connectedSpreadsheet.success(config.toUi()),
                        spreadsheetValidation = SectionState()
                    )
                }
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

    fun openChangeSpreadsheetDialog() {
        _uiState.update { it.copy(showChangeSpreadsheetDialog = true) }
    }

    fun dismissChangeSpreadsheetDialog() {
        _uiState.update { it.copy(showChangeSpreadsheetDialog = false) }
    }

    fun confirmChangeSpreadsheet() {
        viewModelScope.launch {
            clearSpreadsheetConnectionUseCase()
            _uiState.update {
                it.copy(
                    showChangeSpreadsheetDialog = false,
                    spreadsheetValidation = SectionState()
                )
            }
        }
    }

    fun revalidateSpreadsheet() {
        val spreadsheet = _uiState.value.connectedSpreadsheet.data ?: run {
            _uiState.update {
                it.copy(
                    spreadsheetValidation = it.spreadsheetValidation.error(
                        "No spreadsheet connected yet."
                    )
                )
            }
            return
        }

        val validationInput = spreadsheet.spreadsheetUrl ?: spreadsheet.spreadsheetId
        _uiState.update {
            it.copy(spreadsheetValidation = it.spreadsheetValidation.loading())
        }

        viewModelScope.launch {
            when (val result = validateSpreadsheetUseCase(validationInput)) {
                is Resource.Success -> {
                    saveSpreadsheetConnectionUseCase(
                        spreadsheetId = result.data.spreadsheetId,
                        spreadsheetName = result.data.spreadsheetTitle,
                        spreadsheetUrl = spreadsheet.spreadsheetUrl ?: result.data.spreadsheetUrl
                    )
                    _uiState.update {
                        it.copy(
                            spreadsheetValidation = it.spreadsheetValidation.success(
                                "Spreadsheet validated successfully."
                            )
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            spreadsheetValidation = it.spreadsheetValidation.error(result.message)
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            spreadsheetValidation = it.spreadsheetValidation.error(
                                "Unable to validate spreadsheet."
                            )
                        )
                    }
                }
            }
        }
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
