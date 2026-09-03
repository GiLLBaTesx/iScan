package com.examscanner.premium.notification

/**
 * BannerMessage - lightweight in-app banner payload (Req 30.7)
 *
 * Non-critical notifications (sync success, recycle-bin expiry, low storage)
 * are surfaced as in-app banners the UI can observe via
 * [NotificationService.banner] rather than as intrusive system notifications.
 *
 * Contains NO PII per workspace security conventions - only aggregate counts
 * and static/templated messages.
 */
data class BannerMessage(
    val type: NotificationService.NotificationType,
    val message: String,
    val severity: Severity = Severity.INFO,
    val id: Long = System.currentTimeMillis()
) {
    enum class Severity { INFO, WARNING, ERROR }
}
