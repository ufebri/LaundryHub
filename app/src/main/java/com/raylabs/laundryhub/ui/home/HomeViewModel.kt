package com.raylabs.laundryhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.*
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.home.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryUseCase: ReadSpreadsheetDataUseCase,
    private val grossUseCase: ReadGrossDataUseCase,
    private val readIncomeUseCase: ReadIncomeTransactionUseCase,
    private val userUseCase: UserUseCase,
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase,
    private val observeReminderLocalStatesUseCase: ObserveReminderLocalStatesUseCase,
    private val evaluateReminderCandidatesUseCase: EvaluateReminderCandidatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val grossPagingData: Flow<PagingData<GrossData>> = 
        grossUseCase.getPagingData()
            .cachedIn(viewModelScope)

    val pendingOrdersPagingData: Flow<PagingData<UnpaidOrderItem>> = 
        _uiState.map { it.searchQuery to it.currentSortOption }
            .distinctUntilChanged()
            .flatMapLatest { (query, sort) ->
                readIncomeUseCase.getPagingData(filter = FILTER.SHOW_UNPAID_DATA)
                    .map { pagingData -> 
                        pagingData.map { it.toUnpaidOrderItem() }
                    }
            }.cachedIn(viewModelScope)

    private var reminderEnabled: Boolean = false
    private var reminderLocalStates: Map<String, ReminderLocalState> = emptyMap()

    init {
        fetchUser()
        observeReminderInputs()
        fetchTodayIncomeFromInit()
        fetchSummaryFromInit()
        fetchReminderDiscoveryFromInit()
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

            is Resource.Empty -> {
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.success(emptyList()))
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
            it.copy(summary = it.summary.loading())
        }

        val grossResult = grossUseCase()
        val grossItem = if (grossResult is Resource.Success) grossResult.data.firstOrNull()?.toUi() else null

        when (val result = summaryUseCase()) {
            is Resource.Success -> {
                _uiState.update {
                    it.copy(summary = it.summary.success(result.data.toUI(grossItem)))
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(summary = it.summary.error(result.message))
                }
            }

            else -> Unit
        }
    }

    private fun fetchReminderDiscoveryFromInit() {
        viewModelScope.launch {
            combine(
                observeReminderSettingsUseCase(),
                observeReminderLocalStatesUseCase(),
                flow { emit(readIncomeUseCase(filter = FILTER.SHOW_UNPAID_DATA)) }
            ) { settings, localStates, unpaidResource ->
                val unpaidOrders = (unpaidResource as? Resource.Success<List<TransactionData>>)?.data.orEmpty()
                val candidates = evaluateReminderCandidatesUseCase(unpaidOrders, localStates)
                
                ReminderDiscoveryUiState(
                    eligibleCount = candidates.size,
                    headline = "Punya \${candidates.size} Tagihan Pending",
                    supportingText = "Kirim pengingat WhatsApp sekarang?",
                    isReminderEnabled = settings.isReminderEnabled,
                    ctaLabel = "Lihat Detail"
                )
            }.collect { discoveryState ->
                _uiState.update { it.copy(reminderDiscovery = discoveryState) }
            }
        }
    }

    private fun observeReminderInputs() {
        viewModelScope.launch {
            combine(
                observeReminderSettingsUseCase(),
                observeReminderLocalStatesUseCase()
            ) { settings, localStates ->
                reminderEnabled = settings.isReminderEnabled
                reminderLocalStates = localStates
            }.collect()
        }
    }

    fun refreshAllData() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            coroutineScope {
                listOf(
                    async { fetchTodayIncome() },
                    async { fetchSummary() }
                ).awaitAll()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(
                isSearchActive = !it.isSearchActive,
                searchQuery = if (it.isSearchActive) "" else it.searchQuery
            )
        }
    }

    fun changeSortOrder(option: SortOption) {
        _uiState.update { it.copy(currentSortOption = option) }
    }
    
    fun refreshAfterOrderChanged() {
        refreshAllData()
    }

    fun refreshAfterOutcomeChanged() {
        refreshAllData()
    }
}

private fun TransactionData.toUnpaidOrderItem(): UnpaidOrderItem {
    return UnpaidOrderItem(
        orderID = orderID,
        customerName = name,
        packageType = packageType,
        nowStatus = paidDescription(),
        dueDate = dueDate,
        orderDate = date
    )
}
