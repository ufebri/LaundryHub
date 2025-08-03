package com.raylabs.laundryhub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.toUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userUseCase: UserUseCase
) : ViewModel() {


    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState


    init {
        fetchUser()
    }

    private fun fetchUser() {
        val user = userUseCase.getCurrentUser()
        _uiState.value = _uiState.value.copy(
            user = SectionState(data = user?.toUI())
        )
    }

    fun logOut(onSuccess: () -> Unit) {
        _uiState.update { it.copy(logout = it.logout.loading()) }
        viewModelScope.launch {
            val logout = userUseCase.signOut()
            _uiState.update {
                it.copy(logout = it.logout.success(logout))
            }
            if (logout)
                onSuccess()
        }
    }
}