package com.friendat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendat.firebase.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<FirebaseUser?>(null)
    val userState: StateFlow<FirebaseUser?> = _userState

    init {
        viewModelScope.launch {
            val user = repository.signInAnonymously()
            _userState.value = user
        }
    }

    fun logout() {
        repository.signOut()
        _userState.value = null
    }
}
