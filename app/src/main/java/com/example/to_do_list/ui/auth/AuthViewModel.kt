package com.example.to_do_list.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.to_do_list.data.AuthError
import com.example.to_do_list.data.AuthRepository
import com.example.to_do_list.data.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    val userId: StateFlow<String?> = repository.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.currentUserId)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) = submit(email, password, repository::signIn)

    fun signUp(email: String, password: String) = submit(email, password, repository::signUp)

    fun signOut() = repository.signOut()

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun submit(
        email: String,
        password: String,
        call: suspend (String, String) -> AuthError?
    ) {
        if (_uiState.value.isLoading) return
        if (email.isBlank() || password.isEmpty()) {
            _uiState.value = AuthUiState(error = "Enter your email and password.")
            return
        }
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val error = call(email.trim(), password)
            _uiState.value = AuthUiState(error = error?.toMessage())
        }
    }

    private fun AuthError.toMessage(): String = when (this) {
        AuthError.INVALID_CREDENTIALS -> "Wrong email or password."
        AuthError.INVALID_EMAIL -> "That email address isn't valid."
        AuthError.EMAIL_IN_USE -> "An account with this email already exists."
        AuthError.WEAK_PASSWORD -> "Password must have at least 6 characters."
        AuthError.NETWORK -> "No internet connection."
        AuthError.UNKNOWN -> "Something went wrong. Try again."
    }
}
