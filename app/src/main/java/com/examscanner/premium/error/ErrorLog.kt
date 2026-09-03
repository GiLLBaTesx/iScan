package com.examscanner.premium.error

import android.content.Context
import com.examscanner.premium.utils.SecureLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ErrorLog - Local, PII-redacted error journal for the "Report Issue" flow (Req 27.6).
 *
 * Every recoverable error routes through [SecureLogger] (which is disabled in production
 * release builds and never emits sensitive values) AND is appended to a small rotating
 * local file. That local file is what [IssueReporter] bundles into a support report.
 *
 * Before anything is written to the file, likely PII (emails, long digit runs that could
 * be student IDs / phone numbers) is redacted (Req 27.6). The log never records student
 * names, scores, or exam content.
 */
object ErrorLog {

    private const val TAG = "ErrorLog"
    private const val LOG_FILE_NAME = "error_log.txt"
    private const val MAX_LOG_BYTES = 64 * 1024 // 64 KB rotating cap

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Redaction patterns for common PII shapes. Deliberately conservative.
    private val emailPattern = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val longDigitPattern = Regex("\\d{4,}")

    /**
     * Records a recoverable error. Logs through SecureLogger (never PII) and appends a
     * redacted summary to the local log file (Req 27.6).
     */
    fun record(context: Context, tag: String, throwable: Throwable) {
        SecureLogger.e(tag, "Recoverable error", throwable) // never logs PII
        appendRedacted(context, tag, redactedSummary(throwable))
    }

    /**
     * Records a redacted, free-form message (no throwable). The [message] is redacted
     * before being written.
     */
    fun record(context: Context, tag: String, message: String) {
        SecureLogger.e(tag, message)
        appendRedacted(context, tag, redact(message))
    }

    /**
     * Returns the current redacted log contents for inclusion in a support report,
     * or an empty string if no log exists yet.
     */
    fun readLog(context: Context): String = try {
        logFile(context).takeIf { it.exists() }?.readText() ?: ""
    } catch (e: Exception) {
        SecureLogger.e(TAG, "Failed to read error log", e)
        ""
    }

    /** Clears the local error log. */
    fun clear(context: Context) {
        try {
            logFile(context).delete()
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to clear error log", e)
        }
    }

    /**
     * Produces a redacted one-line summary of a throwable: its type and redacted message.
     * The stack trace is intentionally NOT persisted to avoid leaking PII embedded in it.
     */
    fun redactedSummary(throwable: Throwable): String {
        val type = throwable.javaClass.simpleName
        val msg = throwable.message?.let { redact(it) } ?: "no message"
        return "$type: $msg"
    }

    /** Applies PII redaction to an arbitrary string (Req 27.6). */
    fun redact(text: String): String =
        text
            .replace(emailPattern, "[redacted-email]")
            .replace(longDigitPattern, "[redacted-number]")

    private fun appendRedacted(context: Context, tag: String, redactedSummary: String) {
        try {
            val file = logFile(context)
            rotateIfNeeded(file)
            val line = "${timestampFormat.format(Date())} [$tag] $redactedSummary\n"
            file.appendText(line)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to append to error log", e)
        }
    }

    /** Trims the log to stay under [MAX_LOG_BYTES] by truncating from the start. */
    private fun rotateIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_LOG_BYTES) {
            val kept = file.readText().takeLast(MAX_LOG_BYTES / 2)
            file.writeText(kept)
        }
    }

    private fun logFile(context: Context): File =
        File(context.filesDir, LOG_FILE_NAME)
}
