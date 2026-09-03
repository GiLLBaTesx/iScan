package com.examscanner.premium

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.examscanner.premium.analytics.SessionTracker
import com.examscanner.premium.billing.SubscriptionManager
import com.examscanner.premium.data.AppDatabase
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.localization.LocalizationManager
import com.examscanner.premium.notification.RecycleBinPurgeWorker
import com.examscanner.premium.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ExamScannerApplication - Application entry point and the app's manual DI / startup layer
 * (Task 15, Requirements 13.1, 16.6, 20.7, 27.4).
 *
 * There is deliberately no DI framework (no Hilt/Koin): long-lived singletons are exposed as
 * simple `lazy` properties and constructed once here, consistent with the existing code style.
 * The pre-existing encrypted-DB init and analytics/MELC seeding are preserved and extended,
 * never duplicated.
 *
 * Startup responsibilities added by Task 15:
 *  - Apply the saved app locale so the UI renders in the persisted language (Req 20.7).
 *  - Schedule the daily recycle-bin purge worker (Req 27.4 / 10.6), idempotently (KEEP).
 *  - Construct [SubscriptionManager] once and expose it so its ViewModel can be built by the
 *    UI (Req 16.6). The app is offline-only; data portability is via on-device Backup/Restore.
 */
class ExamScannerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    /** Shared repository over the encrypted DB, reused by the startup-scoped managers. */
    val examRepository: ExamRepository by lazy { ExamRepository(database.examDao()) }

    /** Locale switching (Req 20.x). Exposed so SettingsScreen can reuse the same instance. */
    val localizationManager: LocalizationManager by lazy { LocalizationManager(this) }

    // Application-scoped coroutine scope for startup/background work that should outlive any
    // single screen. Uses SupervisorJob so one failing job does not cancel the others.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Google Play Billing client. The purchase result is delivered asynchronously via this
     * [PurchasesUpdatedListener]; on any purchase update we re-query the authoritative status
     * through [SubscriptionManager.checkSubscriptionStatus], which acknowledges/promotes state.
     */
    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { _, _ ->
        // Re-query Google Play for the authoritative subscription status rather than trusting
        // the callback payload; SubscriptionManager handles acknowledge/revert logic.
        appScope.launch {
            try {
                subscriptionManager.checkSubscriptionStatus()
            } catch (e: Exception) {
                SecureLogger.e(TAG, "checkSubscriptionStatus after purchase update failed", e)
            }
        }
    }

    /** Tier gating + Google Play Billing (Req 15.x, 16.x). Built once at startup. */
    val subscriptionManager: SubscriptionManager by lazy {
        SubscriptionManager(
            context = this,
            billingClient = billingClient,
            examRepository = examRepository,
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Apply the persisted locale early so the UI renders in the saved language (Req 20.7).
        // LocalizationManager uses AppCompat per-app locales; applying here covers process start.
        localizationManager.applySavedLanguage()

        // Initialize analytics and session tracking (pre-existing).
        initializeAnalytics()

        // Schedule the daily recycle-bin purge worker (Req 27.4 / 10.6). Idempotent: the worker
        // uses ExistingPeriodicWorkPolicy.KEEP, so repeated startups do not disrupt the schedule.
        RecycleBinPurgeWorker.schedule(this)

        // Initialize billing so subscription status is known on launch (Req 16.6). Safe to call
        // eagerly; SubscriptionManager tolerates billing being unavailable and defaults to Free.
        appScope.launch {
            try {
                subscriptionManager.initialize()
            } catch (e: Exception) {
                SecureLogger.e(TAG, "SubscriptionManager initialization failed", e)
            }
        }

        // Seed initial data including MELCs (pre-existing).
        appScope.launch {
            seedDatabase()
        }
    }

    private fun initializeAnalytics() {
        // Get current user ID (or null if not logged in)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        // Initialize session tracker
        val sessionTracker = SessionTracker.getInstance(userId)
        sessionTracker.initialize()
        
        android.util.Log.d("ExamScannerApp", "✅ Analytics initialized for user: ${userId ?: "anonymous"}")
    }
    
    private suspend fun seedDatabase() {
        val dao = database.examDao()
        
        // Initialize MELCs on first launch
        val existingMelcs = dao.getAllMelcsSync()
        android.util.Log.d("ExamScannerApp", "Checking MELC database: ${existingMelcs.size} MELCs found")
        
        if (existingMelcs.isEmpty()) {
            val melcs = com.examscanner.premium.data.SampleMelcsData.getAllSampleMelcs()
            android.util.Log.d("ExamScannerApp", "Database empty, inserting ${melcs.size} MELCs...")
            dao.insertMelcs(melcs)
            
            // Verify insertion
            val verifyCount = dao.getAllMelcsSync().size
            android.util.Log.d("ExamScannerApp", "✅ Successfully inserted MELCs. Verified count: $verifyCount")
        } else {
            android.util.Log.d("ExamScannerApp", "✅ MELCs already in database: ${existingMelcs.size} found")
        }
    }

    companion object {
        private const val TAG = "ExamScannerApp"
    }
}
