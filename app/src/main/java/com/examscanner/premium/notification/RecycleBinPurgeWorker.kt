package com.examscanner.premium.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.examscanner.premium.data.AppDatabase
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * RecycleBinPurgeWorker (Requirement 10.6, 30.3)
 *
 * Runs daily via WorkManager. It:
 *  1. Scans soft-deleted exams, subject folders, and sections that are within
 *     [EXPIRY_WARNING_DAYS] of the 30-day retention limit (Req 10.2) and emits a
 *     recycle-bin expiry notification with the aggregate count (Req 30.3).
 *  2. Permanently purges items older than the retention window via
 *     [ExamRepository.purgeExpiredDeleted] (Req 10.6).
 *
 * The warning is computed BEFORE purging so the user is told about items that
 * are about to be removed. Only aggregate counts leave this worker - no PII.
 */
class RecycleBinPurgeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = ExamRepository(db.examDao())
            val notificationService = NotificationService(applicationContext)

            // 1. Warn about items nearing the retention limit (Req 30.3), before purge.
            val nearingExpiry = countItemsNearingExpiry(repository)
            if (nearingExpiry > 0) {
                notificationService.notifyRecycleBinExpiry(
                    count = nearingExpiry,
                    days = EXPIRY_WARNING_DAYS
                )
            }

            // 2. Purge everything past the 30-day retention window (Req 10.6).
            val purged = repository.purgeExpiredDeleted(RETENTION_DAYS)
            SecureLogger.i(
                TAG,
                "Recycle-bin purge complete: purged=$purged, nearingExpiry=$nearingExpiry"
            )

            Result.success()
        } catch (e: Exception) {
            SecureLogger.e(TAG, "RecycleBinPurgeWorker failed", e)
            // Transient failure - let WorkManager retry with backoff.
            Result.retry()
        }
    }

    /**
     * Count soft-deleted items whose remaining retention is within
     * [EXPIRY_WARNING_DAYS] of the 30-day limit. An item deleted at time D
     * expires at D + 30d; it is "nearing expiry" when now >= (expiry - 3d),
     * i.e. deletedAt <= now - (30 - 3) days.
     *
     * Uses the retention-scoped recycle-bin flows (they already exclude items
     * older than 30 days), so this yields items in the final warning window.
     */
    private suspend fun countItemsNearingExpiry(repository: ExamRepository): Int {
        val warnCutoff = System.currentTimeMillis() -
            ((RETENTION_DAYS - EXPIRY_WARNING_DAYS).toLong() * DAY_MILLIS)

        val exams = repository.getDeletedExams().first()
        val folders = repository.getDeletedSubjectFolders().first()
        val sections = repository.getDeletedSections().first()

        val examCount = exams.count { (it.deletedAt ?: Long.MAX_VALUE) <= warnCutoff }
        val folderCount = folders.count { (it.deletedAt ?: Long.MAX_VALUE) <= warnCutoff }
        val sectionCount = sections.count { (it.deletedAt ?: Long.MAX_VALUE) <= warnCutoff }

        return examCount + folderCount + sectionCount
    }

    companion object {
        private const val TAG = "RecycleBinPurgeWorker"

        /** Unique work name so re-scheduling is idempotent. */
        const val WORK_NAME = "recycle_bin_purge_worker"

        /** Retention window in days (Req 10.2). */
        const val RETENTION_DAYS = 30

        /** Warn when items are within this many days of the retention limit (Req 30.3). */
        const val EXPIRY_WARNING_DAYS = 3

        private const val DAY_MILLIS = 24L * 60 * 60 * 1000

        /**
         * Schedule the worker to run once per day. Invoked from app startup
         * (Task 15). Uses KEEP so an already-scheduled job is not disrupted.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecycleBinPurgeWorker>(
                1, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            SecureLogger.d(TAG, "RecycleBinPurgeWorker scheduled (daily)")
        }
    }
}
