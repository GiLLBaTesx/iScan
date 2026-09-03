package com.examscanner.premium.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState
    
    init {
        checkAuthStatus()
    }
    
    fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val currentUser = repository.currentUser
            if (currentUser != null) {
                val userProfile = repository.getCurrentUserProfile()
                if (userProfile != null) {
                    _authState.value = AuthState.Authenticated(userProfile)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
    
    fun signUp(signUpData: SignUpData) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.signUp(signUpData)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated(result.getOrThrow())
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated(result.getOrThrow())
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }
    
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun isTrialActive(userProfile: UserProfile): Boolean {
        return repository.isTrialActive(userProfile)
    }
    
    fun getTrialDaysRemaining(userProfile: UserProfile): Int {
        return repository.getTrialDaysRemaining(userProfile)
    }
    
    fun hasActiveSubscription(userProfile: UserProfile): Boolean {
        return when (userProfile.subscriptionStatus) {
            SubscriptionStatus.ACTIVE -> true
            SubscriptionStatus.TRIAL -> isTrialActive(userProfile)
            else -> false
        }
    }
}
