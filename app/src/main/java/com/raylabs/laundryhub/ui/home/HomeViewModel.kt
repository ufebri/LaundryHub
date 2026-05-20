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
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LaundryRepository,
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

    // Trigger khusus untuk Paging agar tidak terpengaruh oleh update status (Optimistic UI)
    // yang sering menyebabkan layar loncat ke atas.
    private val _pagingTrigger = MutableStateFlow(PendingOrderQuery("", SortOption.ORDER_DATE_DESC))

    val grossPagingData: Flow<PagingData<GrossData>> =
        grossUseCase.getPagingData()
            .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pendingOrdersPagingData: Flow<PagingData<UnpaidOrderItem>> =
        _pagingTrigger
            .map { query ->
                query.copy(searchQuery = query.searchQuery.toRemotePendingOrderSearchQuery())
            }
            .debounce { query ->
                if (query.searchQuery.isBlank()) 0L else PENDING_ORDER_SEARCH_DEBOUNCE_MILLIS
            }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                readIncomeUseCase.getPagingData(
                    filter = FILTER.SHOW_UNPAID_DATA,
                    searchQuery = query.searchQuery,
                    sort = query.sortOption.name
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

    suspend fun fetchTodayIncome(isSilent: Boolean = false) {
        if (!isSilent) {
            _uiState.update {
                it.copy(todayIncome = it.todayIncome.loading())
            }
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
                    it.copy(todayIncome = it.todayIncome.copy(isLoading = false, errorMessage = result.message))
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

    suspend fun fetchSummary(isSilent: Boolean = false) {
        if (!isSilent) {
            _uiState.update {
                it.copy(summary = it.summary.loading())
            }
        }

        val grossResult = grossUseCase()
        val currentSummaryData = _uiState.value.summary.data
        val grossItem = if (grossResult is Resource.Success && grossResult.data.isNotEmpty()) {
            grossResult.data.firstOrNull()?.toUi()
        } else {
            // Keep last known gross item from UI if fetch fails
            currentSummaryData?.find { it.title == "Gross Income" }?.let { existingItem ->
                // Reverse map from SummaryItem back to GrossItem is hard without full data,
                // but we only need it for the UI construction later.
                // We'll pass null and let `toUI` handle the missing grossItem,
                // but wait, `toUI` creates a new list. We need a way to pass the old item.
                null
            }
        }

        when (val result = summaryUseCase()) {
            is Resource.Success -> {
                val newUiData = result.data.toUI(
                    grossItem ?: currentSummaryData?.find { it.title == "Gross Income" }?.let {
                        // Re-construct GrossItem from SummaryItem if we couldn't fetch it
                        com.raylabs.laundryhub.ui.home.state.GrossItem(
                            id = 0,
                            month = "",
                            totalNominal = it.body,
                            orderCount = it.footer.replace(" order", "").trim(),
                            tax = ""
                        )
                    }
                )
                _uiState.update {
                    it.copy(summary = it.summary.success(newUiData))
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(summary = it.summary.copy(isLoading = false, data = currentSummaryData, errorMessage = result.message))
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
        triggerFullReactiveSync()
    }

    private fun triggerFullReactiveSync(isSilent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSummaryRefreshing = true) }

            // Trigger backend sync
            repository.triggerManualSync()

            // Poll sync status
            var isSyncing = true
            while (isSyncing) {
                delay(1500)
                val statusResult = repository.getSyncStatus()
                if (statusResult is Resource.Success && !statusResult.data.isSyncing) {
                    isSyncing = false
                }
            }

            // Settlement delay
            delay(1000)

            // Final fetch
            coroutineScope {
                listOf(
                    async { fetchTodayIncome(isSilent) },
                    async { fetchSummary(isSilent) }
                ).awaitAll()
            }

            _uiState.update { it.copy(isRefreshing = false, isSummaryRefreshing = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _pagingTrigger.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update {
            val nextActive = !it.isSearchActive
            val nextQuery = if (it.isSearchActive) "" else it.searchQuery
            it.copy(
                isSearchActive = nextActive,
                searchQuery = nextQuery
            )
        }
        _pagingTrigger.update { 
            it.copy(searchQuery = if (_uiState.value.isSearchActive) _uiState.value.searchQuery else "")
        }
    }

    fun changeSortOrder(option: SortOption) {
        _uiState.update { it.copy(currentSortOption = option) }
        _pagingTrigger.update { it.copy(sortOption = option) }
    }

    fun refreshAfterOrderChanged() {
        _uiState.update { it.copy(optimisticOrders = emptyList()) }
        refreshAllData()
    }

    fun refreshAfterOrderChangedSilent() {
        // Silent refresh: don't clear optimistic orders immediately so they stay on screen until real data arrives
        // We also avoid setting isRefreshing = true so the pull-to-refresh spinner doesn't show
        _uiState.update { it.copy(refreshCounter = it.refreshCounter + 1) }
        triggerFullReactiveSync(isSilent = true)
    }

    fun refreshAfterOutcomeChanged() {
        refreshAllData()
    }

    fun addOptimisticOrder(order: UnpaidOrderItem) {
        _uiState.update { it.copy(optimisticOrders = listOf(order) + it.optimisticOrders) }
    }

    fun updateOptimisticOrderStatus(fakeId: String, status: com.raylabs.laundryhub.ui.home.state.SyncStatus, realId: String? = null) {
        _uiState.update { state ->
            val updatedList = state.optimisticOrders.map {
                if (it.orderID == fakeId) it.copy(
                    syncStatus = status,
                    orderID = realId ?: it.orderID
                ) else it
            }
            state.copy(optimisticOrders = updatedList)
        }
    }

    fun removeOptimisticOrder(fakeId: String) {
        _uiState.update { state ->
            state.copy(optimisticOrders = state.optimisticOrders.filter { it.orderID != fakeId })
        }
    }

    fun retryOptimisticOrder(fakeId: String, orderViewModel: com.raylabs.laundryhub.ui.order.OrderViewModel, scope: kotlinx.coroutines.CoroutineScope, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val orderToRetry = _uiState.value.optimisticOrders.find { it.orderID == fakeId } ?: return
        val payload = orderToRetry.rawPayload ?: return

        updateOptimisticOrderStatus(fakeId, com.raylabs.laundryhub.ui.home.state.SyncStatus.PENDING)

        scope.launch {
            orderViewModel.submitOrder(
                payload,
                onComplete = { createdOrderId ->
                    updateOptimisticOrderStatus(fakeId, com.raylabs.laundryhub.ui.home.state.SyncStatus.SYNCED, createdOrderId)
                    refreshAfterOrderChangedSilent()
                    onSuccess(createdOrderId)
                },
                onError = { errorMessage ->
                    updateOptimisticOrderStatus(fakeId, com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED)
                    onError(errorMessage)
                }
            )
        }
    }
}

private data class PendingOrderQuery(
    val searchQuery: String,
    val sortOption: SortOption
)

private fun String.toRemotePendingOrderSearchQuery(): String {
    val sanitized = trim()
    return sanitized.takeIf { it.length >= MIN_PENDING_ORDER_REMOTE_SEARCH_LENGTH }.orEmpty()
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

private const val MIN_PENDING_ORDER_REMOTE_SEARCH_LENGTH = 2
private const val PENDING_ORDER_SEARCH_DEBOUNCE_MILLIS = 450L
