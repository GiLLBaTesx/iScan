package com.examscanner.premium.error

import android.content.Context
import com.examscanner.premium.utils.SecureLogger
import java.io.File

/**
 * DraftRecoveryManager - Crash / process-death recovery for in-progress work (Req 27.4).
 *
 * Persists in-progress scan or edit state (e.g. an exam or answer key being created) to
 * a temp JSON file so it survives a crash or process death. Drafts are keyed by screen
 * so pending work can be detected on next launch and offered for recovery.
 *
 * The manager only writes what the user has already entered on-screen; callers are
 * responsible for not serializing student PII into the draft payload beyond what the
 * user is actively editing (Req 27.6).
 */
class DraftRecoveryManager(private val context: Context) {

    /**
     * Persists an in-progress edit under [key] so it survives a crash (Req 27.4).
     * Returns true if the draft was written.
     */
    fun saveDraft(key: String, json: String): Boolean = try {
        File(draftDir(), fileNameFor(key)).writeText(json)
        true
    } catch (e: Exception) {
        SecureLogger.e(TAG, "Failed to save draft for key=${sanitizeKey(key)}", e)
        false
    }

    /**
     * Loads a previously saved draft, or null if none exists / cannot be read.
     */
    fun loadDraft(key: String): String? = try {
        File(draftDir(), fileNameFor(key)).takeIf { it.exists() }?.readText()
    } catch (e: Exception) {
        SecureLogger.e(TAG, "Failed to load draft for key=${sanitizeKey(key)}", e)
        null
    }

    /**
     * Removes a draft once the user has saved or discarded the underlying work.
     */
    fun clearDraft(key: String): Boolean = try {
        File(draftDir(), fileNameFor(key)).delete()
    } catch (e: Exception) {
        SecureLogger.e(TAG, "Failed to clear draft for key=${sanitizeKey(key)}", e)
        false
    }

    /**
     * Called on app launch to detect recoverable drafts and prompt the user (Req 27.4).
     * Returns the list of draft keys that currently have persisted content.
     */
    fun pendingDrafts(): List<String> =
        draftDir().listFiles()
            ?.filter { it.isFile && it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

    /** True if a recoverable draft exists for [key]. */
    fun hasDraft(key: String): Boolean = File(draftDir(), fileNameFor(key)).exists()

    private fun fileNameFor(key: String): String = "${sanitizeKey(key)}.json"

    /** Prevents path traversal / illegal file characters in draft keys. */
    private fun sanitizeKey(key: String): String =
        key.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private fun draftDir(): File =
        File(context.filesDir, DRAFT_DIR_NAME).apply { mkdirs() }

    companion object {
        private const val TAG = "DraftRecoveryManager"
        private const val DRAFT_DIR_NAME = "drafts"
    }
}
