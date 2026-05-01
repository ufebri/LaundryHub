package com.raylabs.laundryhub.ui.profile.inventory

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.DeletePackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdatePackageUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.profile.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val readPackageUseCase: ReadPackageUseCase,
    private val getOtherPackageUseCase: GetOtherPackageUseCase,
    private val submitPackageUseCase: SubmitPackageUseCase,
    private val updatePackageUseCase: UpdatePackageUseCase,
    private val deletePackageUseCase: DeletePackageUseCase,
    private val observeSpreadsheetConfigUseCase: ObserveSpreadsheetConfigUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(InventoryUiState())
    val uiState: InventoryUiState get() = _uiState.value

    init {
        observeSpreadsheetConfig()
        fetchPackages()
        fetchOtherPackages()
    }

    fun refreshInventory() {
        fetchPackages()
        fetchOtherPackages()
    }

    suspend fun submitPackage(
        packageData: PackageData,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            savePackage = _uiState.value.savePackage.loading()
        )

        when (val result = submitPackageUseCase(packageData = packageData)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    savePackage = _uiState.value.savePackage.success(result.data)
                )
                refreshInventory()
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    savePackage = _uiState.value.savePackage.error(result.message)
                )
                onError(result.message)
            }

            else -> Unit
        }
    }

    suspend fun updatePackage(
        packageData: PackageData,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            savePackage = _uiState.value.savePackage.loading()
        )

        when (val result = updatePackageUseCase(packageData = packageData)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    savePackage = _uiState.value.savePackage.success(result.data)
                )
                refreshInventory()
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    savePackage = _uiState.value.savePackage.error(result.message)
                )
                onError(result.message)
            }

            else -> Unit
        }
    }

    suspend fun deletePackage(
        sheetRowIndex: Int,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            deletePackage = _uiState.value.deletePackage.loading()
        )

        when (val result = deletePackageUseCase(sheetRowIndex = sheetRowIndex)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    deletePackage = _uiState.value.deletePackage.success(result.data)
                )
                refreshInventory()
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    deletePackage = _uiState.value.deletePackage.error(result.message)
                )
                onError(result.message)
            }

            else -> Unit
        }
    }

    private fun observeSpreadsheetConfig() {
        viewModelScope.launch {
            observeSpreadsheetConfigUseCase().collect { config ->
                _uiState.value = _uiState.value.copy(
                    spreadsheetName = config.spreadsheetName,
                    spreadsheetId = config.spreadsheetId,
                    spreadsheetUrl = config.spreadsheetUrl
                )
            }
        }
    }

    private fun fetchPackages() {
        viewModelScope.launch {
            val current = _uiState.value.packages
            _uiState.value = _uiState.value.copy(packages = current.loading())

            when (val result = readPackageUseCase()) {
                is Resource.Success -> {
                    val mapped = result.data.toUi()
                    _uiState.value = _uiState.value.copy(
                        packages = current.success(mapped)
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        packages = current.success(emptyList())
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        packages = if (current.data != null) {
                            current.copy(isLoading = false, errorMessage = result.message)
                        } else {
                            SectionState(errorMessage = result.message)
                        }
                    )
                }

                else -> Unit
            }
        }
    }

    private fun fetchOtherPackages() {
        viewModelScope.launch {
            val current = _uiState.value.otherPackages
            _uiState.value = _uiState.value.copy(otherPackages = current.loading())

            when (val result = getOtherPackageUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = current.success(result.data)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = if (current.data != null) {
                            current.copy(isLoading = false, errorMessage = result.message)
                        } else {
                            SectionState(errorMessage = result.message)
                        }
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = current.success(emptyList())
                    )
                }

                else -> Unit
            }
        }
    }
}
