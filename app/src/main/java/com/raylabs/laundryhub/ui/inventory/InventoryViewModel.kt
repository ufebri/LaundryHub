package com.raylabs.laundryhub.ui.inventory

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadInventoryUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.inventory.state.toGroupedInventoryUi
import com.raylabs.laundryhub.ui.inventory.state.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val readInventoryUseCase: ReadInventoryUseCase,
    private val readPackageUseCase: ReadPackageUseCase,
    private val getOtherPackageUseCase: GetOtherPackageUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(InventoryUiState())
    val uiState: InventoryUiState get() = _uiState.value

    init {
        fetchInventory()
        fetchPackages()
        fetchOtherPackages()
    }

    private fun fetchInventory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inventory = SectionState(isLoading = true))

            when (val result = readInventoryUseCase()) {
                is Resource.Success -> {
                    val grouped = result.data.toGroupedInventoryUi()
                    _uiState.value = _uiState.value.copy(inventory = SectionState(data = grouped))
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        inventory = SectionState(errorMessage = result.message)
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        inventory = SectionState(errorMessage = "Data kosong")
                    )
                }

                else -> Unit
            }
        }
    }


    private fun fetchPackages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(packages = SectionState(isLoading = true))

            when (val result = readPackageUseCase()) {
                is Resource.Success -> {
                    val mapped = result.data.toUi()
                    _uiState.value = _uiState.value.copy(
                        packages = SectionState(data = mapped)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        packages = SectionState(errorMessage = result.message)
                    )
                }

                else -> Unit
            }
        }
    }

    private fun fetchOtherPackages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(otherPackages = SectionState(isLoading = true))

            when (val result = getOtherPackageUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = SectionState(data = result.data)
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = SectionState(errorMessage = result.message)
                    )
                }
                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = SectionState(errorMessage = "Tidak ada paket lain.")
                    )
                }
                else -> Unit
            }
        }
    }
}