package com.examscanner.premium.error

import android.content.Context
import android.os.StatFs
import com.examscanner.premium.utils.SecureLogger

/**
 * StorageMonitor - Low-storage detection and banner state (Req 27.5, 30.4).
 *
 * Reads the number of available bytes on internal storage and warns when free space
 * drops below [LOW_STORAGE_THRESHOLD_BYTES] (100 MB). The threshold check itself is a
 * pure function ([isLowStorage]) so it can be unit-tested without a device, while
 * [checkStorage] performs the actual filesystem read.
 */
class StorageMonitor(private val context: Context) {

    /**
     * Reads current free space and returns a [StorageStatus] describing whether a
     * low-storage banner should be shown (Req 27.5).
     */
    fun checkStorage(): StorageStatus = try {
        val free = availableBytes()
        StorageStatus(
            availableBytes = free,
            isLow = isLowStorage(free)
        )
    } catch (e: Exception) {
        SecureLogger.e(TAG, "Failed to read storage stats", e)
        // Fail safe: assume not-low so we never spuriously block the user.
        StorageStatus(availableBytes = Long.MAX_VALUE, isLow = false)
    }

    /**
     * Reads the available bytes on the internal files directory's filesystem.
     */
    fun availableBytes(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBytes
    }

    /** Convenience: a user-facing warning message when storage is low. */
    fun lowStorageMessage(status: StorageStatus): String =
        "Storage is running low (${status.availableBytes / (1024 * 1024)} MB free). " +
            "Free up space to keep saving exams and scans."

    /**
     * Snapshot of storage state consumed by the UI banner.
     */
    data class StorageStatus(
        val availableBytes: Long,
        val isLow: Boolean
    )

    companion object {
        private const val TAG = "StorageMonitor"

        /** Free-space threshold below which the low-storage banner appears (Req 27.5). */
        const val LOW_STORAGE_THRESHOLD_BYTES: Long = 100L * 1024 * 1024 // 100 MB

        /**
         * Pure threshold check: storage is "low" when free space is below 100 MB (Req 27.5).
         * Negative inputs are treated as low (defensive).
         */
        fun isLowStorage(
            availableBytes: Long,
            thresholdBytes: Long = LOW_STORAGE_THRESHOLD_BYTES
        ): Boolean = availableBytes < thresholdBytes
    }
}
