package com.raylabs.laundryhub.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.toUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userUseCase: UserUseCase
) : ViewModel() {


    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: ProfileUiState get() = _uiState.value


    init {
        fetchUser()
    }

    private fun fetchUser() {
        val user = userUseCase.getCurrentUser()
        _uiState.value = _uiState.value.copy(
            user = SectionState(data = user?.toUI())
        )
    }

    fun logOut() {
        viewModelScope.launch {
            val logout = userUseCase.signOut()
            _uiState.value = _uiState.value.copy(
                logout = SectionState(data = logout)
            )
        }
    }
}