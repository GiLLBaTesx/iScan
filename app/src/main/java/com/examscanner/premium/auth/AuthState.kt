package com.examscanner.premium.auth

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val schoolName: String? = null,
    val gradeLevel: String? = null,
    val subjects: List<String> = emptyList(),
    val photoUrl: String? = null,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val subscriptionExpiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SubscriptionStatus {
    TRIAL,      // 14-day free trial
    ACTIVE,     // Paid subscription active
    EXPIRED,    // Subscription expired
    CANCELLED   // Subscription cancelled
}

data class SignUpData(
    val email: String,
    val password: String,
    val displayName: String,
    val schoolName: String? = null,
    val gradeLevel: String? = null,
    val subjects: List<String> = emptyList()
)
