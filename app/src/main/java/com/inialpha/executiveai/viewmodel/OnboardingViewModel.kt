package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.data.repository.ConnectAccountResult
import com.inialpha.executiveai.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isConnecting: Boolean = false,
    val connected: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun connectGoogleAccount() {
        val authManager = container.googleAuthManager ?: run {
            _state.value = _state.value.copy(errorMessage = "Sign-in isn't available right now.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isConnecting = true, errorMessage = null)
            when (val result = container.accountRepository.connectNewAccount(authManager)) {
                is ConnectAccountResult.Success -> _state.value = OnboardingUiState(connected = true)
                ConnectAccountResult.Cancelled -> _state.value = _state.value.copy(isConnecting = false)
                is ConnectAccountResult.Failure ->
                    _state.value = _state.value.copy(isConnecting = false, errorMessage = result.message)
            }
        }
    }
}
