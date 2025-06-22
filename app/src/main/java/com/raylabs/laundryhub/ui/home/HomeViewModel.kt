package com.raylabs.laundryhub.ui.home

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
                            data = uiData,
                            isLoading = false,
                            errorMessage = null
                        ),
                        orderUpdateKey = System.currentTimeMillis()
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
        _uiState.update { it.copy(historyOrder = it.historyOrder.loading()) }

        viewModelScope.launch {
            when (val result = getHistoryUseCase(orderID = orderId)) {
                is Resource.Success -> {
                    var uiData = result.data.toUi()
                    val currentStep =
                        uiData.steps.firstOrNull { it.isCurrent && it.selectedMachine.isBlank() }
                    if (currentStep != null) {
                        // Ambil semua mesin available dari repository
                        val availableMachinesRes =
                            getAvailableMachinesForCurrentStep(currentStep.label)
                        if (availableMachinesRes.isNotEmpty()) {
                            uiData = uiData.copy(
                                steps = uiData.steps.withAvailableMachines(availableMachinesRes)
                            )
                        }
                    }
                    _uiState.update {
                        it.copy(historyOrder = it.historyOrder.success(uiData))
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(historyOrder = it.historyOrder.error(result.message))
                    }
                }

                else -> Unit
            }
        }
    }

    private suspend fun getAvailableMachinesForCurrentStep(stationType: String): List<String> {
        // Ambil semua mesin available dari repository
        val inventoryRes = getAvailableMachineUseCase(stationType = stationType)
        return if (inventoryRes is Resource.Success) {
            inventoryRes.data.map { it.stationName }
        } else emptyList()
    }

    fun markStepStarted(step: LaundryStepUiModel) {
        val orderId = uiState.value.historyOrder.data?.orderId ?: return
        val machineName = step.selectedMachine
        if (machineName.isBlank()) return // Tidak ada mesin yang dipilih

        viewModelScope.launch {
            _uiState.update { it.copy(isMarkingStep = true) }

            // Cari id mesin dari inventory jika perlu (misal: untuk updateMarkStepHistoryUseCase)
            val inventoryRes = getAvailableMachineUseCase(stationType = step.label)
            val machineId = if (inventoryRes is Resource.Success) {
                inventoryRes.data.firstOrNull { it.stationName == machineName }?.id
            } else null
            if (machineId == null) {
                _uiState.update { it.copy(isMarkingStep = false) }
                return@launch
            }

            val startedAt = DateUtil.getTodayDate(dateFormat = "dd-MM-yyyy HH:mm")

            val result = updateMarkStepHistoryUseCase(
                orderId = orderId,
                step = step.label,
                startedAt = startedAt,
                machineId = machineId,
                machineName = machineName
            )

            if (result is Resource.Success) {
                getOrderById(orderId)
            }

            _uiState.update { it.copy(isMarkingStep = false) }
        }
    }
}