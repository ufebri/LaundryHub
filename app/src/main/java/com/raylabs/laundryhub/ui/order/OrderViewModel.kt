package com.raylabs.laundryhub.ui.order

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetLastOrderIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitOrderUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.inventory.state.toUi
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val getLastOrderIdUseCase: GetLastOrderIdUseCase,
    private val submitOrderUseCase: SubmitOrderUseCase,
    private val packageListUseCase: ReadPackageUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(OrderUiState())
    val uiState: OrderUiState get() = _uiState.value

    init {
        fetchLastOrderId()
        getPackageList()
    }

    private fun fetchLastOrderId() {
        viewModelScope.launch {
            when (val result = getLastOrderIdUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(lastOrderId = result.data)
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(lastOrderId = "Error, try again")
                }

                else -> Unit
            }
        }
    }

    private fun getPackageList() {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(packageNameList = _uiState.value.packageNameList.loading())

            when (val result = packageListUseCase()) {
                is Resource.Success -> {
                    val mData = result.data.toUi()
                    _uiState.value = _uiState.value.copy(
                        packageNameList = _uiState.value.packageNameList.success(mData)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        packageNameList = _uiState.value.packageNameList.error(result.message)
                    )
                }

                else -> Unit
            }
        }
    }

    fun submitOrder(order: OrderData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                submit = _uiState.value.submit.loading()
            )

            when (val result = submitOrderUseCase(order = order)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        submit = _uiState.value.submit.success(result.data)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        submit = _uiState.value.submit.error(result.message)
                    )
                }

                else -> Unit
            }
        }
    }

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "name" -> _uiState.value.copy(name = value)
            "phone" -> _uiState.value.copy(phone = value)
            "price" -> _uiState.value.copy(price = value)
            "paymentMethod" -> _uiState.value.copy(paymentMethod = value)
            "note" -> _uiState.value.copy(note = value)
            else -> _uiState.value
        }
    }

    fun updateSelectedPackage(item: PackageItem) {
        _uiState.value = _uiState.value.copy(
            selectedPackage = item,
            price = item.displayPrice
        )
    }

    fun onPackageSelected(packageItem: PackageItem) {
        val currentPrice = uiState.price.toIntOrNull() ?: 0
        val minPrice = packageItem.price.toIntOrNull() ?: 1
        val weight = if (minPrice != 0) currentPrice / minPrice else 0

        _uiState.value = _uiState.value.copy(
            selectedPackage = packageItem,
            price = if (currentPrice != 0) currentPrice.toString() else packageItem.price,
            weight = "$weight Kg"
        )
    }

    fun onPriceChanged(price: String) {
        val minPrice = _uiState.value.selectedPackage?.price?.toIntOrNull() ?: 1
        val priceInt = price.toIntOrNull() ?: 0
        val weight = if (minPrice != 0) priceInt / minPrice else 0

        _uiState.value = _uiState.value.copy(
            price = price,
            weight = "$weight Kg"
        )
    }
}