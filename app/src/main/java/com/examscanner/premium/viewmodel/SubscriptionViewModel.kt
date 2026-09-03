package com.examscanner.premium.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examscanner.premium.billing.SubscriptionManager
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * SubscriptionViewModel - Binds [SubscriptionManager] state to the subscription/upgrade UI
 * (Requirements 16.3, 16.7).
 *
 * The [SubscriptionManager] owns the Google Play Billing lifecycle, tier-limit enforcement,
 * and revert-on-expiry logic. This ViewModel is a thin binding layer: it exposes the manager's
 * [SubscriptionManager.subscriptionState] stream and forwards the purchase/status actions,
 * running suspend calls on [viewModelScope]. The manager is supplied by the DI/startup layer
 * (Task 15) because it needs a [android.content.Context], a [com.android.billingclient.api.BillingClient],
 * and the exam repository.
 */
class SubscriptionViewModel(
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    /** Current subscription state (Free / Premium / PendingPurchase / Error) for the UI to bind. */
    val subscriptionState: StateFlow<SubscriptionManager.SubscriptionState> =
        subscriptionManager.subscriptionState

    /**
     * Launches the premium subscription purchase flow (Req 16.3). The purchase result arrives
     * asynchronously via the billing client's PurchasesUpdatedListener, which promotes the
     * state to Premium; state transitions are surfaced through [subscriptionState]. The
     * [activity] is passed through from the UI because the billing flow must be hosted by an
     * Activity.
     */
    fun subscribeToPremium(activity: Activity) {
        viewModelScope.launch {
            val result = subscriptionManager.subscribeToPremium(activity)
            if (result.isFailure) {
                SecureLogger.e(TAG, "subscribeToPremium failed", result.exceptionOrNull())
            }
        }
    }

    /**
     * Re-queries Google Play for the current subscription status, reverting to Free when no
     * active premium purchase is found (Req 16.7). Updates [subscriptionState].
     */
    fun checkStatus() {
        viewModelScope.launch {
            subscriptionManager.checkSubscriptionStatus()
        }
    }

    /** Convenience synchronous read: is the current user on the premium tier? */
    fun isPremium(): Boolean = subscriptionManager.isPremium()

    companion object {
        private const val TAG = "SubscriptionViewModel"
    }
}

/**
 * Factory for [SubscriptionViewModel]. The [SubscriptionManager] is constructed by the
 * DI/startup layer (Task 15) and injected here so the ViewModel stays free of billing-client
 * construction.
 */
class SubscriptionViewModelFactory(
    private val subscriptionManager: SubscriptionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(subscriptionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
