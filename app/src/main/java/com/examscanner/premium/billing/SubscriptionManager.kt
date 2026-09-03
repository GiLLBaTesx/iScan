package com.examscanner.premium.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.examscanner.premium.analytics.AnalyticsTracker
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * SubscriptionManager - Google Play Billing subscription and tier gating
 *
 * Responsibilities:
 * - Initialize the Google Play Billing client and check subscription status on launch
 * - Launch the premium subscription purchase flow (₱100/month)
 * - Enforce Free vs. Premium tier limits (count-based and feature-based) via [checkLimit]
 * - Revert to Free when a subscription expires or is no longer active
 * - Integrate with the existing device-based [com.examscanner.premium.utils.TrialGuard]
 *   so that when a subscription becomes active the trial is flagged complete
 *
 * Testability: all tier-limit decision logic is factored into the PURE, side-effect-free
 * companion functions [isWithinLimit] / [isFeatureAllowed] and the [FREE_LIMITS] /
 * [PREMIUM_LIMITS] constants. These can be exercised by property tests (Task 8.2) with no
 * real [BillingClient] or database. The billing-client wiring (init/query/launch) and the
 * repository count reads in [checkLimit] wrap that pure decision.
 *
 * Requirements: 15.1-15.7 (Free tier limits), 16.1-16.7 (Premium subscription).
 */
class SubscriptionManager(
    private val context: Context,
    private val billingClient: BillingClient,
    private val examRepository: ExamRepository,
    private val currentUserId: String? = null
) {

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    /** Current subscription state exposed for synchronous reads (e.g. tier-limit lookups). */
    sealed class SubscriptionState {
        object Free : SubscriptionState()
        data class Premium(val expiryDate: Long) : SubscriptionState()
        object PendingPurchase : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }

    /** Immutable description of what a tier allows. */
    data class TierLimits(
        val maxSubjects: Int,
        val maxExamsPerSubject: Int,
        val maxScansPerExam: Int,
        val allowsAdvancedAnalytics: Boolean,
        val allowsSchoolReports: Boolean,
        val allowsCustomTemplates: Boolean,
        val allowsCurriculumTracking: Boolean
    )

    /** Operations that are gated by the current tier. */
    sealed class LimitedOperation {
        object CreateSubject : LimitedOperation()
        data class CreateExam(val subjectId: Long) : LimitedOperation()
        data class ScanSheet(val examId: Long) : LimitedOperation()
        object AdvancedAnalytics : LimitedOperation()
        object SchoolReport : LimitedOperation()
        object CustomTemplate : LimitedOperation()
        object CurriculumTracking : LimitedOperation()
    }

    // ---------------------------------------------------------------------------------------
    // Billing lifecycle
    // ---------------------------------------------------------------------------------------

    /**
     * Initializes the billing client connection and, once connected, checks the current
     * subscription status. Safe to call on launch (Req 15.6, 16.6).
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val billingResult = suspendCoroutine<BillingResult> { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        continuation.resume(result)
                    }

                    override fun onBillingServiceDisconnected() {
                        // Connection lost; a subsequent call to initialize()/checkSubscriptionStatus()
                        // will attempt to reconnect. We do not resume the continuation here.
                        SecureLogger.w(TAG, "Billing service disconnected")
                    }
                })
            }

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                checkSubscriptionStatus()
            } else {
                SecureLogger.w(TAG, "Billing setup failed: code=${billingResult.responseCode}")
                // Default to the last-known persisted state; on a fresh install this is Free.
                _subscriptionState.value = restorePersistedState()
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Billing initialization error", e)
            _subscriptionState.value = restorePersistedState()
            AnalyticsTracker.trackError(
                userId = currentUserId,
                errorType = "billing_init_error",
                errorMessage = e.message ?: "Unknown billing init error"
            )
        }
    }

    /**
     * Queries Google Play for active subscriptions and updates [subscriptionState].
     * Reverts to [SubscriptionState.Free] when no active/acknowledged premium purchase is
     * found (revert-on-expiry, Req 16.7).
     */
    suspend fun checkSubscriptionStatus() = withContext(Dispatchers.IO) {
        try {
            val purchasesResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val activePurchase = purchasesResult.purchasesList.firstOrNull { purchase ->
                purchase.products.contains(PREMIUM_SKU) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.isAcknowledged
            }

            if (activePurchase != null) {
                _subscriptionState.value = SubscriptionState.Premium(
                    expiryDate = getExpiryDate(activePurchase)
                )
                savePersistedState(isPremium = true)
                // Flag the device trial as complete so it can't be re-abused (TrialGuard integration).
                flagTrialCompleteIfPossible()
                AnalyticsTracker.trackFeatureUsage(
                    userId = currentUserId,
                    featureName = "subscription_active",
                    success = true
                )
            } else {
                _subscriptionState.value = SubscriptionState.Free
                savePersistedState(isPremium = false)
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to query subscription status", e)
            AnalyticsTracker.trackError(
                userId = currentUserId,
                errorType = "billing_query_error",
                errorMessage = e.message ?: "Unknown billing query error"
            )
        }
    }

    /**
     * Launches the premium subscription purchase flow. The final purchase result arrives via
     * the [com.android.billingclient.api.PurchasesUpdatedListener] configured on the injected
     * [billingClient]; that listener should acknowledge the purchase and call
     * [checkSubscriptionStatus] to promote the state to [SubscriptionState.Premium].
     */
    suspend fun subscribeToPremium(activity: Activity): Result<Unit> {
        _subscriptionState.value = SubscriptionState.PendingPurchase

        return try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PREMIUM_SKU)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = withContext(Dispatchers.IO) {
                billingClient.queryProductDetails(params)
            }

            val productDetails = productDetailsResult.productDetailsList?.firstOrNull()
                ?: run {
                    _subscriptionState.value = SubscriptionState.Error("Product not found")
                    return Result.failure(Exception("Product not found"))
                }

            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: run {
                    _subscriptionState.value = SubscriptionState.Error("Offer not found")
                    return Result.failure(Exception("Offer not found"))
                }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                AnalyticsTracker.trackFeatureUsage(
                    userId = currentUserId,
                    featureName = "subscription_flow_launched",
                    success = true
                )
                Result.success(Unit)
            } else {
                _subscriptionState.value = SubscriptionState.Error("Purchase failed")
                SecureLogger.w(TAG, "Billing flow failed: ${billingResult.debugMessage}")
                Result.failure(Exception("Billing flow failed: ${billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            _subscriptionState.value = SubscriptionState.Error(e.message ?: "Unknown error")
            SecureLogger.e(TAG, "subscribeToPremium error", e)
            AnalyticsTracker.trackError(
                userId = currentUserId,
                errorType = "billing_purchase_error",
                errorMessage = e.message ?: "Unknown purchase error"
            )
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Tier gating (impure wrappers over the pure decision functions)
    // ---------------------------------------------------------------------------------------

    /** Returns the tier limits that apply to the current subscription state. */
    fun getCurrentLimits(): TierLimits = when (subscriptionState.value) {
        is SubscriptionState.Premium -> PREMIUM_LIMITS
        else -> FREE_LIMITS
    }

    /** Convenience: is the current user on the premium tier? */
    fun isPremium(): Boolean = subscriptionState.value is SubscriptionState.Premium

    /**
     * Checks whether an [operation] is permitted under the current tier.
     *
     * Count-based operations read the current count from [examRepository] and delegate the
     * decision to the pure [isWithinLimit]. Feature-based operations delegate to the pure
     * [isFeatureAllowed]. Keeping the decision pure means Task 8.2's property test can verify
     * enforcement without touching billing or the database.
     */
    suspend fun checkLimit(operation: LimitedOperation): Boolean {
        val limits = getCurrentLimits()

        return when (operation) {
            is LimitedOperation.CreateSubject -> {
                val currentCount = examRepository.getAllSubjectFolders().first().size
                isWithinLimit(operation, currentCount, limits)
            }
            is LimitedOperation.CreateExam -> {
                val currentCount = examRepository.getExamsByFolder(operation.subjectId).first().size
                isWithinLimit(operation, currentCount, limits)
            }
            is LimitedOperation.ScanSheet -> {
                val currentCount = examRepository.getStudents(operation.examId).first().size
                isWithinLimit(operation, currentCount, limits)
            }
            LimitedOperation.AdvancedAnalytics,
            LimitedOperation.SchoolReport,
            LimitedOperation.CustomTemplate,
            LimitedOperation.CurriculumTracking -> isFeatureAllowed(operation, limits)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------------------

    private fun getExpiryDate(purchase: Purchase): Long {
        // Google Play does not expose the renewal date in the Purchase object for subscriptions;
        // it manages auto-renewal server-side. We record the purchase time as a reference point.
        // Revert-to-Free is driven by checkSubscriptionStatus() no longer finding an active
        // acknowledged purchase, not by comparing against this timestamp.
        return purchase.purchaseTime
    }

    private fun flagTrialCompleteIfPossible() {
        // TrialGuard.flagTrialComplete is a suspend Firestore write requiring an authenticated
        // user id. It is invoked from the auth/startup layer which holds the userId + context;
        // here we only record intent locally so that layer can complete the flag. This avoids
        // duplicating trial logic while keeping SubscriptionManager decoupled from Firebase auth.
        currentUserId ?: return
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_TRIAL_FLAG_PENDING, true)
                .apply()
        } catch (e: Exception) {
            SecureLogger.w(TAG, "Unable to record trial-complete flag", e)
        }
    }

    private fun savePersistedState(isPremium: Boolean) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_PREMIUM, isPremium)
                .apply()
        } catch (e: Exception) {
            SecureLogger.w(TAG, "Unable to persist subscription state", e)
        }
    }

    private fun restorePersistedState(): SubscriptionState {
        return try {
            val isPremium = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_PREMIUM, false)
            if (isPremium) SubscriptionState.Premium(expiryDate = 0L) else SubscriptionState.Free
        } catch (e: Exception) {
            SubscriptionState.Free
        }
    }

    companion object {
        private const val TAG = "SubscriptionManager"
        private const val PREFS_NAME = "subscription_manager"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_TRIAL_FLAG_PENDING = "trial_flag_pending"

        const val PREMIUM_SKU = "premium_monthly_100php"

        val FREE_LIMITS = TierLimits(
            maxSubjects = 3,
            maxExamsPerSubject = 5,
            maxScansPerExam = 30,
            allowsAdvancedAnalytics = false,
            allowsSchoolReports = false,
            allowsCustomTemplates = false,
            allowsCurriculumTracking = false
        )

        val PREMIUM_LIMITS = TierLimits(
            maxSubjects = Int.MAX_VALUE,
            maxExamsPerSubject = Int.MAX_VALUE,
            maxScansPerExam = Int.MAX_VALUE,
            allowsAdvancedAnalytics = true,
            allowsSchoolReports = true,
            allowsCustomTemplates = true,
            allowsCurriculumTracking = true
        )

        /**
         * PURE tier-limit decision for count-based operations.
         *
         * Returns true if and only if [currentCount] is strictly below the applicable limit for
         * [operation] under [limits]. For Premium (limits == [Int.MAX_VALUE]) this is always true
         * for any realistic count. Non-count operations return false here — use
         * [isFeatureAllowed] for feature gating.
         *
         * This function has no dependency on the billing client or the database and is the
         * enforcement point verified by Property 12 (Task 8.2).
         *
         * Validates: Requirements 15.1, 15.2, 15.3, 16.4
         */
        fun isWithinLimit(
            operation: LimitedOperation,
            currentCount: Int,
            limits: TierLimits
        ): Boolean = when (operation) {
            is LimitedOperation.CreateSubject -> currentCount < limits.maxSubjects
            is LimitedOperation.CreateExam -> currentCount < limits.maxExamsPerSubject
            is LimitedOperation.ScanSheet -> currentCount < limits.maxScansPerExam
            else -> false
        }

        /**
         * PURE feature-gate decision for feature-based operations. Returns the corresponding
         * boolean flag on [limits]; count-based operations return false here.
         */
        fun isFeatureAllowed(
            operation: LimitedOperation,
            limits: TierLimits
        ): Boolean = when (operation) {
            LimitedOperation.AdvancedAnalytics -> limits.allowsAdvancedAnalytics
            LimitedOperation.SchoolReport -> limits.allowsSchoolReports
            LimitedOperation.CustomTemplate -> limits.allowsCustomTemplates
            LimitedOperation.CurriculumTracking -> limits.allowsCurriculumTracking
            else -> false
        }
    }
}
