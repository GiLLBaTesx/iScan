package com.examscanner.premium.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationService (Requirement 30)
 *
 * Central entry point for user-facing notices:
 *  - Sync success (Req 30.1) / sync failed (Req 30.2)
 *  - Recycle-bin expiry using the template
 *    "{count} items will be permanently deleted in {days} days" (Req 30.3)
 *  - Low storage below 100 MB (Req 30.4)
 *
 * Honors per-type preferences stored in SharedPreferences (Req 30.5), respects
 * system Do Not Disturb (Req 30.6), and routes non-critical notices to in-app
 * banners instead of system notifications (Req 30.7).
 *
 * Security: never logs or embeds PII. Recycle-bin notices carry only aggregate
 * counts; sync/storage notices are static strings.
 */
class NotificationService(private val context: Context) {

    enum class NotificationType {
        SYNC_SUCCESS, SYNC_FAILED, RECYCLE_BIN_EXPIRY, LOW_STORAGE
    }

    /**
     * In-app banner stream for non-critical notices (Req 30.7). The UI observes
     * this and renders an Azure-Glass themed banner. Holds the latest banner so
     * a freshly-composed screen can show a pending notice; dismiss via [dismissBanner].
     */
    private val _banner = MutableStateFlow<BannerMessage?>(null)
    val banner: StateFlow<BannerMessage?> = _banner.asStateFlow()

    /**
     * Optional event stream for callers that prefer transient (one-shot) banner
     * delivery over the retained [banner] state.
     */
    private val _bannerEvents = MutableSharedFlow<BannerMessage>(extraBufferCapacity = 8)
    val bannerEvents: SharedFlow<BannerMessage> = _bannerEvents.asSharedFlow()

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createChannel()
    }

    // ---- Public API ----

    /**
     * Post a notification of [type]. Non-critical notices become in-app banners
     * (Req 30.7); critical notices become system notifications. Both respect the
     * per-type preference (Req 30.5) and system DND (Req 30.6).
     */
    fun notify(type: NotificationType, message: String, critical: Boolean = false) {
        if (!isTypeEnabled(type)) {
            SecureLogger.d(TAG, "Notification suppressed by preference: $type")
            return
        }
        if (critical) {
            postSystemNotification(type, message)
        } else {
            emitInAppBanner(type, message)
        }
    }

    /** Sync completed successfully (Req 30.1). Non-critical -> in-app banner. */
    fun notifySyncSuccess() {
        notify(NotificationType.SYNC_SUCCESS, MSG_SYNC_SUCCESS, critical = false)
    }

    /** Sync failed after retries (Req 30.2). Critical -> system notification. */
    fun notifySyncFailed() {
        notify(NotificationType.SYNC_FAILED, MSG_SYNC_FAILED, critical = true)
    }

    /**
     * Recycle-bin expiry notice (Req 30.3). Builds the message from the fixed
     * template with aggregate [count] and remaining [days]. No PII involved.
     * Non-critical -> in-app banner. No-op when [count] is zero.
     */
    fun notifyRecycleBinExpiry(count: Int, days: Int) {
        if (count <= 0) return
        val message = recycleBinExpiryMessage(count, days)
        notify(NotificationType.RECYCLE_BIN_EXPIRY, message, critical = false)
    }

    /** Low-storage notice (Req 30.4). Non-critical -> in-app banner. */
    fun notifyLowStorage() {
        notify(NotificationType.LOW_STORAGE, MSG_LOW_STORAGE, critical = false)
    }

    /**
     * Check free internal storage and warn when it drops below 100 MB
     * (Req 27.5, 30.4). Returns true when a low-storage notice was raised.
     * A lightweight fallback for the concurrently-authored StorageMonitor.
     */
    fun checkLowStorageAndNotify(): Boolean {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val availableBytes = stat.availableBytes
            if (availableBytes < LOW_STORAGE_THRESHOLD_BYTES) {
                notifyLowStorage()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "checkLowStorageAndNotify failed", e)
            false
        }
    }

    /** Clear the retained in-app banner once the UI has shown it. */
    fun dismissBanner() {
        _banner.value = null
    }

    // ---- Preferences (Req 30.5) ----

    /** Whether a notification [type] is enabled in Settings. Defaults to enabled. */
    fun isTypeEnabled(type: NotificationType): Boolean {
        return prefs.getBoolean(prefKey(type), true)
    }

    /** Persist the per-type notification preference (Req 30.5). */
    fun setTypeEnabled(type: NotificationType, enabled: Boolean) {
        prefs.edit().putBoolean(prefKey(type), enabled).apply()
    }

    // ---- Internals ----

    /**
     * Route a non-critical notice to the in-app banner stream (Req 30.7). This
     * path is exempt from DND because it does not interrupt the user.
     */
    private fun emitInAppBanner(type: NotificationType, message: String) {
        val banner = BannerMessage(
            type = type,
            message = message,
            severity = severityFor(type)
        )
        _banner.value = banner
        _bannerEvents.tryEmit(banner)
        SecureLogger.d(TAG, "In-app banner emitted: $type")
    }

    /**
     * Post a system notification for a critical notice. Respects system DND
     * (Req 30.6): when the interruption filter is not "all", the notice is
     * downgraded to an in-app banner instead of interrupting the user. Also
     * respects the runtime POST_NOTIFICATIONS permission on Android 13+.
     */
    private fun postSystemNotification(type: NotificationType, message: String) {
        if (isDoNotDisturbActive()) {
            SecureLogger.d(TAG, "DND active - routing $type to in-app banner instead")
            emitInAppBanner(type, message)
            return
        }
        if (!hasPostPermission()) {
            SecureLogger.w(TAG, "POST_NOTIFICATIONS not granted - falling back to banner for $type")
            emitInAppBanner(type, message)
            return
        }
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(SMALL_ICON)
                .setContentTitle(titleFor(type))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(type.ordinal, notification)
            SecureLogger.d(TAG, "System notification posted: $type")
        } catch (e: SecurityException) {
            // Permission revoked between check and post - degrade gracefully.
            SecureLogger.e(TAG, "SecurityException posting notification for $type", e)
            emitInAppBanner(type, message)
        }
    }

    /**
     * Respect system-level Do Not Disturb (Req 30.6). Returns true when the
     * current interruption filter suppresses notifications.
     */
    private fun isDoNotDisturbActive(): Boolean {
        return try {
            when (notificationManager.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_NONE,
                NotificationManager.INTERRUPTION_FILTER_ALARMS,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> true
                else -> false // INTERRUPTION_FILTER_ALL / UNKNOWN
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Unable to read interruption filter", e)
            false
        }
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    // DEFAULT importance so the OS honors DND / channel settings (Req 30.6)
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESCRIPTION
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun prefKey(type: NotificationType): String = "notif_enabled_${type.name}"

    private fun titleFor(type: NotificationType): String = when (type) {
        NotificationType.SYNC_SUCCESS -> "Sync complete"
        NotificationType.SYNC_FAILED -> "Sync failed"
        NotificationType.RECYCLE_BIN_EXPIRY -> "Recycle bin"
        NotificationType.LOW_STORAGE -> "Storage warning"
    }

    private fun severityFor(type: NotificationType): BannerMessage.Severity = when (type) {
        NotificationType.SYNC_SUCCESS -> BannerMessage.Severity.INFO
        NotificationType.SYNC_FAILED -> BannerMessage.Severity.ERROR
        NotificationType.RECYCLE_BIN_EXPIRY -> BannerMessage.Severity.WARNING
        NotificationType.LOW_STORAGE -> BannerMessage.Severity.WARNING
    }

    companion object {
        private const val TAG = "NotificationService"

        const val PREFS_NAME = "notification_preferences"

        const val CHANNEL_ID = "iscan_general"
        private const val CHANNEL_NAME = "iScan notifications"
        private const val CHANNEL_DESCRIPTION =
            "Sync status, recycle bin, and storage alerts"

        // Reuse the launcher icon; the project ships no dedicated status icon.
        private val SMALL_ICON = com.examscanner.premium.R.drawable.ic_launcher_foreground

        /** Low-storage threshold: 100 MB (Req 30.4). */
        const val LOW_STORAGE_THRESHOLD_BYTES = 100L * 1024 * 1024

        // Fixed message strings from Requirement 30.
        const val MSG_SYNC_SUCCESS = "Data synced successfully"
        const val MSG_SYNC_FAILED = "Sync failed. Check connection."
        const val MSG_LOW_STORAGE = "Low storage. Back up and clear old data."

        /**
         * Build the recycle-bin expiry message from the Req 30.3 template
         * "{count} items will be permanently deleted in {days} days".
         */
        fun recycleBinExpiryMessage(count: Int, days: Int): String =
            "$count items will be permanently deleted in $days days"
    }
}
