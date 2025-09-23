package com.raylabs.laundryhub.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetLastOrderIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdateOrderUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormat
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormatWithComma
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.inventory.state.PackageItem
import com.raylabs.laundryhub.ui.inventory.state.toUi
import com.raylabs.laundryhub.ui.order.state.OrderUiState
import com.raylabs.laundryhub.ui.order.state.toUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val getLastOrderIdUseCase: GetLastOrderIdUseCase,
    private val submitOrderUseCase: SubmitOrderUseCase,
    private val packageListUseCase: ReadPackageUseCase,
    private val getOrderByIdUseCase: GetOrderUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState

    init {
        fetchLastOrderId()
        getPackageList()
    }

    fun onOrderEditClick(orderId: String, onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(
            editOrder = _uiState.value.editOrder.loading(),
            isEditMode = false
        )
        viewModelScope.launch {
            when (val result = getOrderByIdUseCase(orderID = orderId)) {
                is Resource.Success -> {
                    setEditOrder(result.data)
                    _uiState.value = _uiState.value.copy(
                        editOrder = _uiState.value.editOrder.success(result.data.toUI()),
                        isEditMode = true
                    )
                    getPackageList()
                    onSuccess()
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        editOrder = _uiState.value.editOrder.error(result.message),
                        isEditMode = false
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        editOrder = _uiState.value.editOrder.error("No data found for order ID: $orderId"),
                        isEditMode = false
                    )
                }

                else -> Unit
            }
        }
    }

    fun setEditOrder(order: TransactionData) {
        _uiState.value = _uiState.value.copy(
            orderID = order.orderID,
            name = order.name,
            phone = order.phoneNumber,         // mapping sesuai field TransactionItem
            rawPrice = order.totalPrice.removeRupiahFormat(),
            price = order.totalPrice,
            paymentMethod = order.paidDescription(),
            note = order.remark,
            weight = order.weight,
            isEditMode = true,
            selectedPackage = null,            // SINKRON nanti!
            tempSelectedPackageName = order.packageType // atau .packageName
        )
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
                    val firstItem = mData.firstOrNull()

                    val match =
                        if (_uiState.value.isEditMode && !_uiState.value.tempSelectedPackageName.isNullOrBlank()) {
                            mData.find { it.name == _uiState.value.tempSelectedPackageName }
                        } else null

                    val selected = match ?: firstItem
                    val weight = recalculateWeight(_uiState.value.price.removeRupiahFormatWithComma(), selected)

                    _uiState.value = _uiState.value.copy(
                        packageNameList = _uiState.value.packageNameList.success(mData),
                        selectedPackage = selected,
                        weight = weight,
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

    suspend fun submitOrder(order: OrderData, onComplete: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(
            submitNewOrder = _uiState.value.submitNewOrder.loading(),
            isSubmitting = true
        )

        when (val result = submitOrderUseCase(order = order)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    submitNewOrder = _uiState.value.submitNewOrder.success(result.data),
                    isSubmitting = false
                )
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    submitNewOrder = _uiState.value.submitNewOrder.error(result.message),
                    isSubmitting = false
                )
            }

            else -> {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    fun updateOrder(order: OrderData, onComplete: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(
            updateOrder = _uiState.value.updateOrder.loading(),
            isSubmitting = true
        )

        viewModelScope.launch {
            when (val result = updateOrderUseCase(order = order)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        updateOrder = _uiState.value.updateOrder.success(result.data),
                        isSubmitting = false
                    )
                    onComplete()
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        updateOrder = _uiState.value.updateOrder.error(result.message),
                        isSubmitting = false
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                }
            }
        }
    }

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "name" -> _uiState.value.copy(name = value)
            "paymentMethod" -> _uiState.value.copy(paymentMethod = value)
            "note" -> _uiState.value.copy(note = value)
            else -> _uiState.value
        }
    }

    fun onPhoneChanged(input: String) {
        val cleaned = if (input.startsWith("0")) input.drop(1) else input
        _uiState.value = _uiState.value.copy(phone = cleaned)
    }

    fun onPackageSelected(packageItem: PackageItem) {
        _uiState.value = _uiState.value.copy(
            selectedPackage = packageItem,
            weight = recalculateWeight(_uiState.value.price, packageItem)
        )
    }

    fun onPriceChanged(raw: String) {
        _uiState.value = _uiState.value.copy(
            price = raw,
            weight = recalculateWeight(raw, _uiState.value.selectedPackage)
        )
    }

    private fun recalculateWeight(price: String, packageItem: PackageItem?): String {
        val priceInt = price.filter { it.isDigit() }.toIntOrNull() ?: 0
        val minPrice = packageItem?.price?.filter { it.isDigit() }?.toIntOrNull() ?: 1
        return if (minPrice > 0) (priceInt / minPrice).toString() else ""
    }

    fun resetForm() {
        fetchLastOrderId() // Ditambahkan di sini
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            name = "",
            phone = "",
            price = "",
            note = "",
            weight = ""
        )
    }
}
