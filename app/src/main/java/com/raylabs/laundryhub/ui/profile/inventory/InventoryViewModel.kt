package com.raylabs.laundryhub.ui.profile.inventory

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.usecase.sheets.DeletePackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdatePackageUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.profile.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
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
    private val deletePackageUseCase: DeletePackageUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(InventoryUiState())
    val uiState: InventoryUiState get() = _uiState.value

    init {
        fetchPackages()
        fetchOtherPackages()
    }

    fun refreshInventory(isSilent: Boolean = false) {
        fetchPackages(isSilent)
        fetchOtherPackages(isSilent)
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
                onComplete()
                addPackageLocally(packageData)
                refreshInventory(isSilent = true)
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
        originalPackageName: String? = null,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            savePackage = _uiState.value.savePackage.loading()
        )

        when (val result = updatePackageUseCase(packageData = packageData, originalPackageName = originalPackageName)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    savePackage = _uiState.value.savePackage.success(result.data)
                )
                onComplete()
                updatePackageLocally(packageData)
                refreshInventory(isSilent = true)
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

    fun deletePackage(
        packageName: String,
        onComplete: suspend () -> Unit = {},
        onError: suspend (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletePackage = _uiState.value.deletePackage.loading()
            )

            when (val result = deletePackageUseCase(packageName = packageName)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        deletePackage = _uiState.value.deletePackage.success(result.data)
                    )
                    onComplete()
                    removePackageLocally(packageName)
                    refreshInventory(isSilent = true)
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
    }

    private fun fetchPackages(isSilent: Boolean = false) {
        if (!isSilent) {
            _uiState.value = _uiState.value.copy(
                packages = _uiState.value.packages.loading()
            )
        }

        viewModelScope.launch {
            when (val result = readPackageUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        packages = _uiState.value.packages.success(result.data.toUi())
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        packages = _uiState.value.packages.error(result.message)
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        packages = _uiState.value.packages.success(emptyList())
                    )
                }

                else -> Unit
            }
        }
    }

    private fun fetchOtherPackages(isSilent: Boolean = false) {
        if (!isSilent) {
            _uiState.value = _uiState.value.copy(
                otherPackages = _uiState.value.otherPackages.loading()
            )
        }

        viewModelScope.launch {
            when (val result = getOtherPackageUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = _uiState.value.otherPackages.success(result.data)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = _uiState.value.otherPackages.error(result.message)
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        otherPackages = _uiState.value.otherPackages.success(emptyList())
                    )
                }

                else -> Unit
            }
        }
    }

    private fun addPackageLocally(packageData: PackageData) {
        val current = _uiState.value.packages.data.orEmpty()
        val item = packageData.toPackageItem()
        _uiState.value = _uiState.value.copy(
            packages = _uiState.value.packages.success((current + item).distinctBy { it.name.lowercase() })
        )
    }

    private fun updatePackageLocally(packageData: PackageData) {
        val current = _uiState.value.packages.data.orEmpty()
        val item = packageData.toPackageItem()
        _uiState.value = _uiState.value.copy(
            packages = _uiState.value.packages.success(
                current.map { existing ->
                    if (existing.matchesPackage(packageData)) item else existing
                }
            )
        )
    }

    private fun removePackageLocally(packageName: String) {
        val current = _uiState.value.packages.data.orEmpty()
        _uiState.value = _uiState.value.copy(
            packages = _uiState.value.packages.success(
                current.filterNot { it.name.equals(packageName, ignoreCase = true) }
            )
        )
    }
}

private fun PackageData.toPackageItem(): PackageItem =
    PackageItem(
        name = name,
        price = price,
        work = duration,
        unit = unit,
        sheetRowIndex = sheetRowIndex,
        id = id
    )

private fun PackageItem.matchesPackage(packageData: PackageData): Boolean {
    return when {
        id > 0 && packageData.id > 0 -> id == packageData.id
        sheetRowIndex >= 2 && packageData.sheetRowIndex >= 2 -> sheetRowIndex == packageData.sheetRowIndex
        else -> name.equals(packageData.name, ignoreCase = true)
    }
}
