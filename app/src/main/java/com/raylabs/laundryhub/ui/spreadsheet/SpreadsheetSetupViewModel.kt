package com.raylabs.laundryhub.ui.spreadsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.data.repository.GSheetRepositoryErrorHandling
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SaveSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ValidateSpreadsheetUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.spreadsheet.state.SpreadsheetSetupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpreadsheetSetupViewModel @Inject constructor(
    private val observeSpreadsheetConfigUseCase: ObserveSpreadsheetConfigUseCase,
    private val saveSpreadsheetConnectionUseCase: SaveSpreadsheetConnectionUseCase,
    private val validateSpreadsheetUseCase: ValidateSpreadsheetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpreadsheetSetupUiState())
    val uiState: StateFlow<SpreadsheetSetupUiState> = _uiState

    init {
        observeSpreadsheetConfig()
    }

    fun onInputChanged(value: String) {
        _uiState.update {
            it.copy(
                input = value,
                errorMessage = null,
                infoMessage = null,
                showRequestAccess = false,
                isReady = false
            )
        }
    }

    fun validateAndContinue() {
        val rawInput = uiState.value.input.trim()
        if (rawInput.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = INPUT_REQUIRED_MESSAGE,
                    infoMessage = null,
                    showRequestAccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isValidating = true,
                    errorMessage = null,
                    infoMessage = null,
                    showRequestAccess = false
                )
            }

            when (val result = validateSpreadsheetUseCase(rawInput)) {
                is Resource.Success -> handleValidationSuccess(
                    validationResult = result.data,
                    rawInput = rawInput
                )

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            isReady = false,
                            errorMessage = mapValidationError(result.message),
                            infoMessage = null,
                            showRequestAccess = shouldOfferGoogleSheetsRequest(result.message.orEmpty())
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            isReady = false,
                            errorMessage = VALIDATION_FALLBACK_MESSAGE,
                            infoMessage = null,
                            showRequestAccess = false
                        )
                    }
                }
            }
        }
    }

    private fun observeSpreadsheetConfig() {
        viewModelScope.launch {
            observeSpreadsheetConfigUseCase().collect { config ->
                val configuredInput = config.spreadsheetUrl ?: config.spreadsheetId.orEmpty()
                val hasConfiguredSpreadsheet = config.hasConfiguredSpreadsheet && config.hasCurrentValidation
                _uiState.update { state ->
                    state.copy(
                        configuredSpreadsheetId = config.spreadsheetId,
                        configuredSpreadsheetName = config.spreadsheetName,
                        configuredSpreadsheetUrl = config.spreadsheetUrl,
                        configuredValidationVersion = config.validationVersion,
                        hasLoadedConfiguration = true,
                        isRestoring = false,
                        isReady = hasConfiguredSpreadsheet,
                        errorMessage = if (hasConfiguredSpreadsheet) null else state.errorMessage,
                        infoMessage = if (hasConfiguredSpreadsheet) null else state.infoMessage,
                        showRequestAccess = if (hasConfiguredSpreadsheet) false else state.showRequestAccess,
                        input = if (
                            state.input.isBlank() ||
                            state.configuredSpreadsheetId != config.spreadsheetId ||
                            state.configuredSpreadsheetUrl != config.spreadsheetUrl
                        ) {
                            configuredInput
                        } else {
                            state.input
                        }
                    )
                }

                if (configuredInput.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            isReady = false
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleValidationSuccess(
        validationResult: SpreadsheetValidationResult,
        rawInput: String
    ) {
        saveSpreadsheetConnectionUseCase(
            spreadsheetId = validationResult.spreadsheetId,
            spreadsheetName = validationResult.spreadsheetTitle,
            spreadsheetUrl = rawInput
        )

        _uiState.update {
            it.copy(
                configuredSpreadsheetId = validationResult.spreadsheetId,
                configuredSpreadsheetName = validationResult.spreadsheetTitle,
                configuredSpreadsheetUrl = validationResult.spreadsheetUrl,
                configuredValidationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION,
                hasLoadedConfiguration = true,
                input = rawInput,
                isRestoring = false,
                isValidating = false,
                isReady = true,
                errorMessage = null,
                infoMessage = null,
                showRequestAccess = false
            )
        }
    }

    private fun shouldOfferGoogleSheetsRequest(message: String): Boolean {
        if (message.contains("Google Sheets access is not connected", ignoreCase = true)) {
            return false
        }
        if (message.contains("Invalid spreadsheet URL or ID", ignoreCase = true)) {
            return false
        }
        if (message.contains("Error 404", ignoreCase = true)) {
            return false
        }
        if (message == GSheetRepositoryErrorHandling.AUTHORIZATION_CONFIGURATION_MESSAGE) {
            return false
        }
        if (message == GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE) {
            return true
        }
        if (message == GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE) {
            return false
        }
        if (message.contains("access token is unavailable", ignoreCase = true)) {
            return true
        }
        if (message.contains("developer_error", ignoreCase = true)) {
            return false
        }
        if (message.contains("Unknown calling package name", ignoreCase = true)) {
            return false
        }

        return message.contains("Error 403") ||
            message.contains("Error 404") ||
            message.contains("access", ignoreCase = true) ||
            message.contains("permission", ignoreCase = true)
    }

    private fun mapValidationError(message: String?): String {
        val rawMessage = message.orEmpty()
        return when {
            rawMessage.isBlank() -> VALIDATION_FALLBACK_MESSAGE
            rawMessage == GoogleSheetService.MISSING_ACCESS_MESSAGE ->
                GOOGLE_SHEETS_ACCESS_REQUIRED_MESSAGE

            rawMessage == GSheetRepositoryErrorHandling.EDIT_ACCESS_REQUIRED_MESSAGE ->
                EDITOR_ACCESS_REQUIRED_MESSAGE

            rawMessage == GSheetRepositoryErrorHandling.AUTHORIZATION_CONFIGURATION_MESSAGE ->
                GOOGLE_SHEETS_RECONNECT_MESSAGE

            rawMessage == GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE ->
                GOOGLE_SHEETS_ACCESS_EXPIRED_MESSAGE

            rawMessage == GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE ->
                DRIVE_API_NOT_ENABLED_FRIENDLY_MESSAGE

            rawMessage.contains("Invalid spreadsheet URL or ID", ignoreCase = true) ->
                INVALID_SPREADSHEET_MESSAGE

            rawMessage.contains("Error 404", ignoreCase = true) ->
                SPREADSHEET_NOT_FOUND_MESSAGE

            rawMessage.contains("Error 403", ignoreCase = true) ||
                rawMessage.contains("permission", ignoreCase = true) ->
                SPREADSHEET_ACCESS_DENIED_MESSAGE

            else -> rawMessage
        }
    }

    private companion object {
        const val INPUT_REQUIRED_MESSAGE = "Paste your spreadsheet URL or ID first."
        const val INVALID_SPREADSHEET_MESSAGE = "Use a valid spreadsheet URL or spreadsheet ID."
        const val VALIDATION_FALLBACK_MESSAGE = "We couldn't validate this spreadsheet. Try again."
        const val GOOGLE_SHEETS_ACCESS_REQUIRED_MESSAGE =
            "Connect Google Sheets first to continue."
        const val GOOGLE_SHEETS_RECONNECT_MESSAGE =
            "Google Sheets couldn't reconnect cleanly on this device. Try granting access again."
        const val GOOGLE_SHEETS_ACCESS_EXPIRED_MESSAGE =
            "Google Sheets access expired. Grant access again to continue."
        const val EDITOR_ACCESS_REQUIRED_MESSAGE =
            "This account can open the spreadsheet, but it still needs Editor access."
        const val DRIVE_API_NOT_ENABLED_FRIENDLY_MESSAGE =
            "LaundryHub still needs Google Drive API enabled before it can verify spreadsheet access."
        const val SPREADSHEET_NOT_FOUND_MESSAGE =
            "Spreadsheet not found. Check the URL or ID and try again."
        const val SPREADSHEET_ACCESS_DENIED_MESSAGE =
            "This account doesn't have enough access to use this spreadsheet yet."
    }
}
