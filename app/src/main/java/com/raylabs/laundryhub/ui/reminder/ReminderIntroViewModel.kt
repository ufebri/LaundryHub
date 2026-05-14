package com.raylabs.laundryhub.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetDailyReminderNotificationEnabledUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetDailyReminderNotificationTimeUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetReminderEnabledUseCase
import com.raylabs.laundryhub.ui.reminder.state.ReminderIntroUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderIntroViewModel @Inject constructor(
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase,
    private val setReminderEnabledUseCase: SetReminderEnabledUseCase,
    private val setDailyReminderNotificationEnabledUseCase: SetDailyReminderNotificationEnabledUseCase,
    private val setDailyReminderNotificationTimeUseCase: SetDailyReminderNotificationTimeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderIntroUiState())
    val uiState: StateFlow<ReminderIntroUiState> = _uiState

    init {
        viewModelScope.launch {
            observeReminderSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(reminderSettings = settings) }
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setReminderEnabledUseCase(enabled)
        }
    }

    fun setDailyNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setDailyReminderNotificationEnabledUseCase(enabled)
        }
    }

    fun setDailyNotificationTime(hourOfDay: Int, minute: Int) {
        viewModelScope.launch {
            setDailyReminderNotificationTimeUseCase(hourOfDay, minute)
        }
    }
}
