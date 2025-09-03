package com.friendat.ui.viewmodel // Passe den Paketnamen ggf. an

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.data.repository.AuthRepository
import com.friendat.data.repository.UserAuthResult // Importiere deinen Result-Wrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel //Wichtig für Hilt
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository // Hier dependency inject
) : ViewModel() {

    //Eingabefelder
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    //Zustände
    private val _authUiState = MutableStateFlow<UserAuthResult?>(null) // Null bedeutet initial, kein Vorgang gestartet
    val authUiState: StateFlow<UserAuthResult?> = _authUiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    //Event Handling
    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }


    fun signIn() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _authUiState.value = null

            val result = authRepository.signInWithEmailPassword(email.value, password.value)
            _authUiState.value = result
            _isLoading.value = false
        }
    }

    fun signUp() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _authUiState.value = null

            val result = authRepository.signUpWithEmailPassword(email.value, password.value)
            _authUiState.value = result
            _isLoading.value = false
        }
    }


    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authUiState.value = null
            _email.value = ""
            _password.value = ""
        }
    }

    fun clearAuthUiState() {
        _authUiState.value = null
    }

}

