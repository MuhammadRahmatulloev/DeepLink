package com.deeplink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeplink.app.data.model.UserProfile
import com.deeplink.app.data.repository.AuthRepository
import com.deeplink.app.data.repository.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val profile: UserProfile? = null,
    val pendingVerifyEmail: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn()

    init {
        if (authRepository.isLoggedIn()) {
            loadProfile()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun register(
        email: String,
        username: String,
        password: String,
        password2: String,
        language: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(email, username, password, password2, language)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = message,
                            pendingVerifyEmail = email
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.verifyEmail(email, code)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = message)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email, password)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(isLoading = false, profile = profile)
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
            onComplete()
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            authRepository.getProfile()
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile) }
                }
        }
    }
}
