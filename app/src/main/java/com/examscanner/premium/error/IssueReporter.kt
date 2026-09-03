package com.examscanner.premium.error

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.examscanner.premium.utils.SecureLogger

/**
 * IssueReporter - "Report Issue" flow surfaced on critical errors (Req 27.7).
 *
 * Bundles the redacted local [ErrorLog] together with non-sensitive device/app metadata
 * (app version, Android level, free storage) into a pre-filled support email. It never
 * attaches student data; the log it includes is already PII-redacted by [ErrorLog].
 */
class IssueReporter(
    private val context: Context,
    private val storageMonitor: StorageMonitor = StorageMonitor(context),
    private val supportEmail: String = DEFAULT_SUPPORT_EMAIL
) {

    /**
     * Builds a redacted issue report and launches a share/email intent (Req 27.7).
     * [summary] is a short, user-provided or system-generated description of the problem;
     * it is redacted before being included.
     */
    fun reportIssue(summary: String) {
        val body = buildReportBody(summary)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                putExtra(Intent.EXTRA_SUBJECT, "iScan Issue Report")
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to launch issue report intent", e)
        }
    }

    /**
     * Builds the full redacted report body: metadata + redacted summary + redacted log.
     * Exposed for testing and for callers that want to preview or route the report
     * elsewhere (e.g. Crashlytics).
     */
    fun buildReportBody(summary: String): String {
        val redactedSummary = ErrorLog.redact(summary)
        val log = ErrorLog.readLog(context)
        return buildString {
            appendLine("=== iScan Issue Report ===")
            appendLine(deviceMetadata())
            appendLine()
            appendLine("Summary:")
            appendLine(redactedSummary)
            appendLine()
            appendLine("Recent errors (redacted):")
            appendLine(if (log.isBlank()) "(none)" else log)
        }
    }

    private fun deviceMetadata(): String {
        val freeMb = storageMonitor.checkStorage().availableBytes / (1024 * 1024)
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        return buildString {
            appendLine("App version: $versionName")
            appendLine("Android API: ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            append("Free storage: ${freeMb} MB")
        }
    }

    companion object {
        private const val TAG = "IssueReporter"
        private const val DEFAULT_SUPPORT_EMAIL = "support@iscan.app"
    }
}
