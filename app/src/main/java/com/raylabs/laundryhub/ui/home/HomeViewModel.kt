package com.raylabs.laundryhub.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryFilter
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetAvailableMachineUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetHistoryUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadOrderStatusUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdateMarkStepHistoryUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.LaundryStepUiModel
import com.raylabs.laundryhub.ui.home.state.toUI
import com.raylabs.laundryhub.ui.home.state.toUi
import com.raylabs.laundryhub.ui.home.state.withAvailableMachines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryUseCase: ReadSpreadsheetDataUseCase,
    private val readIncomeUseCase: ReadIncomeTransactionUseCase,
    private val userUseCase: UserUseCase,
    private val readOrderStatusUseCase: ReadOrderStatusUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val updateMarkStepHistoryUseCase: UpdateMarkStepHistoryUseCase,
    private val getAvailableMachineUseCase: GetAvailableMachineUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        fetchUser()
        fetchTodayIncomeFromInit()
        fetchSummaryFromInit()
        fetchOrderFromInit()
    }

    private fun fetchUser() {
        val user = userUseCase.getCurrentUser()
        _uiState.update {
            it.copy(user = SectionState(data = user?.toUI()))
        }
    }

    private fun fetchTodayIncomeFromInit() {
        viewModelScope.launch { fetchTodayIncome() }
    }

    suspend fun fetchTodayIncome() {
        _uiState.update {
            it.copy(todayIncome = it.todayIncome.loading())
        }

        when (val result = readIncomeUseCase(filter = FILTER.TODAY_TRANSACTION_ONLY)) {
            is Resource.Success -> {
                val uiData = result.data.toUI()
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.success(uiData))
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.error(result.message))
                }
            }

            Resource.Empty -> {
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.error("Tidak ada data hari ini"))
                }
            }

            else -> Unit
        }
    }

    private fun fetchSummaryFromInit() {
        viewModelScope.launch { fetchSummary() }
    }

    suspend fun fetchSummary() {
        _uiState.update {
            it.copy(summary = SectionState(isLoading = true))
        }
        when (val result = summaryUseCase()) {
            is Resource.Success -> {
                val uiData = result.data.toUI()
                _uiState.update {
                    it.copy(summary = it.summary.success(uiData))
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(summary = it.summary.error(result.message))
                }
            }

            Resource.Empty -> {
                _uiState.update {
                    it.copy(summary = it.summary.error("Data Kosong"))
                }
            }

            else -> Unit
        }
    }

    private fun fetchOrderFromInit() {
        viewModelScope.launch { fetchOrder() }
    }

    suspend fun fetchOrder() {
        _uiState.update {
            it.copy(orderStatus = SectionState(isLoading = true))
        }
        when (val result =
            readOrderStatusUseCase(filterHistory = HistoryFilter.SHOW_UNDONE_ORDER)) {
            is Resource.Success -> {
                val uiData = result.data.toUI()
                _uiState.update {
                    it.copy(
                        orderStatus = SectionState(
                            data = uiData, isLoading = false, errorMessage = null
                        ), orderUpdateKey = System.currentTimeMillis()
                    )
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(orderStatus = it.orderStatus.error(result.message))
                }
            }

            is Resource.Empty -> {
                _uiState.update {
                    it.copy(orderStatus = it.orderStatus.error("Data Kosong"))
                }
            }

            else -> Unit
        }
    }

    fun setSelectedOrderId(id: String) {
        _uiState.update { it.copy(selectedOrderID = id) }
    }

    fun clearSelectedOrder() {
        _uiState.update { it.copy(selectedOrderID = null) }
    }

    fun getOrderById(orderId: String) {
        Log.d("HomeViewModel", "getOrderById called with orderId=$orderId")
        _uiState.update { it.copy(historyOrder = it.historyOrder.loading()) }

        viewModelScope.launch {
            when (val result = getHistoryUseCase(orderID = orderId)) {
                is Resource.Success -> {
                    Log.d("HomeViewModel", "getOrderById success, data=${result.data}")
                    var uiData = result.data.toUi()
                    val currentStep =
                        uiData.steps.firstOrNull { it.isCurrent && it.selectedMachine.isBlank() }
                    val machineSteps = listOf("Washing Machine", "Drying Machine", "Ironing Machine", "Folding")
                    if (currentStep != null && machineSteps.contains(currentStep.label)) {
                        val availableMachinesRes =
                            getAvailableMachinesForCurrentStep(currentStep.label)
                        Log.d(
                            "HomeViewModel",
                            "Available machines for ${currentStep.label}: $availableMachinesRes"
                        )
                        if (availableMachinesRes.isNotEmpty()) {
                            uiData = uiData.copy(
                                steps = uiData.steps.withAvailableMachines(availableMachinesRes)
                            )
                        }
                    }
                    _uiState.update {
                        Log.d(
                            "HomeViewModel", "Updating uiState.historyOrder with: $uiData"
                        )
                        it.copy(historyOrder = it.historyOrder.success(uiData))
                    }
                }

                is Resource.Error -> {
                    Log.e("HomeViewModel", "getOrderById error: ${result.message}")
                    _uiState.update {
                        it.copy(historyOrder = it.historyOrder.error(result.message))
                    }
                }

                else -> Unit
            }
        }
    }

    private suspend fun getAvailableMachinesForCurrentStep(stationType: String): List<String> {
        // Untuk step yang tidak butuh mesin, langsung return emptyList tanpa log error
        if (stationType == "Packing" || stationType == "Ready") {
            return emptyList()
        }
        val inventoryRes = getAvailableMachineUseCase(stationType = stationType)
        Log.d("HomeViewModel", "getAvailableMachinesForCurrentStep: stationType=$stationType, inventoryRes=$inventoryRes")
        return if (inventoryRes is Resource.Success) {
            val machines = inventoryRes.data.map { it.stationName }
            Log.d("HomeViewModel", "Available machines for $stationType: $machines")
            machines
        } else {
            Log.d("HomeViewModel", "No machines found for $stationType")
            emptyList()
        }
    }

    fun markStepStarted(step: LaundryStepUiModel) {
        val orderId = uiState.value.historyOrder.data?.orderId ?: return
        val machineName = step.selectedMachine
        val noMachineStep = step.label == "Packing" || step.label == "Ready"
        if (!noMachineStep && machineName.isBlank()) return // Tidak ada mesin yang dipilih untuk step yang butuh mesin

        viewModelScope.launch {
            Log.d("HomeViewModel", "markStepStarted called with step=$step")
            _uiState.update { it.copy(isMarkingStep = true) }

            if (noMachineStep) {
                val startedAt = DateUtil.getTodayDate(dateFormat = "dd-MM-yyyy HH:mm")
                Log.d(
                    "HomeViewModel",
                    "Calling updateMarkStepHistoryUseCase (no machine) with orderId=$orderId, step=${step.label}, startedAt=$startedAt"
                )
                val result = updateMarkStepHistoryUseCase(
                    orderId = orderId,
                    step = step.label.replace("Machine", "").trim(),
                    startedAt = startedAt,
                    machineId = "", // string kosong, bukan null
                    machineName = "-"
                )
                Log.d("HomeViewModel", "updateMarkStepHistoryUseCase result: $result")
                if (result is Resource.Success) {
                    getOrderById(orderId)
                }
                _uiState.update {
                    Log.d("HomeViewModel", "isMarkingStep set to false")
                    it.copy(isMarkingStep = false)
                }
                return@launch
            }

            val inventoryRes = getAvailableMachineUseCase(stationType = step.label)
            val machineId = if (inventoryRes is Resource.Success) {
                inventoryRes.data.firstOrNull { it.stationName == machineName }?.id
            } else null
            Log.d(
                "HomeViewModel", "Resolved machineId=$machineId for machineName=$machineName"
            )
            if (machineId == null) {
                Log.e(
                    "HomeViewModel", "MachineId not found for machineName=$machineName"
                )
                _uiState.update { it.copy(isMarkingStep = false) }
                return@launch
            }

            val startedAt = DateUtil.getTodayDate(dateFormat = "dd-MM-yyyy HH:mm")
            Log.d(
                "HomeViewModel",
                "Calling updateMarkStepHistoryUseCase with orderId=$orderId, step=${step.label}, startedAt=$startedAt, machineId=$machineId, machineName=$machineName"
            )

            val result = updateMarkStepHistoryUseCase(
                orderId = orderId,
                step = step.label.replace("Machine", "").trim(),
                startedAt = startedAt,
                machineId = machineId,
                machineName = machineName
            )

            Log.d("HomeViewModel", "updateMarkStepHistoryUseCase result: $result")
            if (result is Resource.Success) {
                getOrderById(orderId)
            }

            _uiState.update {
                Log.d("HomeViewModel", "isMarkingStep set to false")
                it.copy(isMarkingStep = false)
            }
        }
    }
}