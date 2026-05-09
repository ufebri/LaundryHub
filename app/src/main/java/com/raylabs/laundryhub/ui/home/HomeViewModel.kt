package com.raylabs.laundryhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.ReminderDiscoveryUiState
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem
import com.raylabs.laundryhub.ui.home.state.toUI
import com.raylabs.laundryhub.ui.home.state.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val pendingOrdersPagingData: Flow<PagingData<UnpaidOrderItem>> = 
        _uiState.map { Triple(it.searchQuery, it.currentSortOption, it.refreshCounter) }
            .distinctUntilChanged()
            .flatMapLatest { (query, sort, _) ->
                readIncomeUseCase.getPagingData(
                    filter = FILTER.SHOW_UNPAID_DATA,
                    searchQuery = query,
                    sort = sort.name
                )
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

        val today = com.raylabs.laundryhub.shared.util.PlatformDate.getTodayDate("yyyy-MM-dd")
        when (val result = readIncomeUseCase(
            filter = FILTER.RANGE_TRANSACTION_DATA,
            rangeDate = RangeDate(startDate = today, endDate = today)
        )) {
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

                if (candidates.isEmpty()) {
                    null
                } else {
                    val topCandidate = candidates.first()
                    ReminderDiscoveryUiState(
                        eligibleCount = candidates.size,
                        headline = if (candidates.size == 1) {
                            "1 order needs a cross-check"
                        } else {
                            "${candidates.size} orders need a cross-check"
                        },
                        supportingText = when {
                            settings.isReminderEnabled && topCandidate.overdueDays <= 0 ->
                                "Open Reminder Inbox to review orders that reached their due date."
                            settings.isReminderEnabled ->
                                "Open Reminder Inbox. The oldest one is already ${topCandidate.overdueDays} days past the due date."
                            topCandidate.overdueDays <= 0 ->
                                "Turn on reminders to review orders that may need a status update after the due date."
                            else ->
                                "Turn on reminders. The oldest one is already ${topCandidate.overdueDays} days past the due date."
                        },
                        isReminderEnabled = settings.isReminderEnabled,
                        ctaLabel = if (settings.isReminderEnabled) {
                            "Open Reminder Inbox"
                        } else {
                            "Turn on reminder"
                        }
                    )
                }
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
        _uiState.update { it.copy(isRefreshing = true, refreshCounter = it.refreshCounter + 1) }
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
        _uiState.update { it.copy(optimisticOrders = emptyList()) }
        refreshAllData()
    }

    fun refreshAfterOutcomeChanged() {
        refreshAllData()
    }

    fun addOptimisticOrder(order: UnpaidOrderItem) {
        _uiState.update { it.copy(optimisticOrders = listOf(order) + it.optimisticOrders) }
    }
}

fun TransactionData.toUnpaidOrderItem(): UnpaidOrderItem {
    return UnpaidOrderItem(
        orderID = orderID,
        customerName = name,
        packageType = packageType,
        nowStatus = paidDescription(),
        dueDate = dueDate,
        orderDate = date
    )
}
