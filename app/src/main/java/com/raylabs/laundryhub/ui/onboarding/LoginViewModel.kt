package com.raylabs.laundryhub.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.usecase.auth.CheckUserLoggedInUseCase
import com.raylabs.laundryhub.core.domain.usecase.auth.SignInWithGoogleUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authUseCase: SignInWithGoogleUseCase,
    private val checkUserLoggedInUseCase: CheckUserLoggedInUseCase,
    private val userUseCase: UserUseCase
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState = _userState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState = _errorState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        if (checkUserLoggedInUseCase()) {
            _userState.value = userUseCase.getCurrentUser()
        }
    }

    fun signInGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = authUseCase(idToken)
                if (user != null) {
                    _userState.value = user
                    _errorState.value = null
                } else {
                    _errorState.value = "Failed to sign in"
                }
            } catch (e: Exception) {
                _errorState.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }
}