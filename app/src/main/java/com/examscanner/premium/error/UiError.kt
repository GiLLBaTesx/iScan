package com.examscanner.premium.error

/**
 * UiError - Presentation-layer error taxonomy (Requirement 27).
 *
 * ViewModels translate a [RepositoryResult.Error] (or other failures) into one of
 * these typed variants so the UI can render an appropriate message and action
 * (e.g. UPGRADE for [LimitReached], RETRY for [FileOperation]). Messages are user-facing
 * and must never contain student PII (Req 27.6).
 */
sealed class UiError {
    data class Network(val message: String) : UiError()
    data class Database(val message: String) : UiError()
    data class Validation(val field: String, val message: String) : UiError()
    data class Permission(val permission: String) : UiError()
    data class LimitReached(val limit: String, val upgrade: Boolean = true) : UiError()
    data class FileOperation(val operation: String, val message: String) : UiError()

    companion object {
        /**
         * Renders a UiError as a human-readable string for snackbars/banners.
         */
        fun message(error: UiError): String = when (error) {
            is Network -> "No internet connection. ${error.message}"
            is Database -> "Database error: ${error.message}"
            is Validation -> "${error.field}: ${error.message}"
            is Permission -> "Permission required: ${error.permission}"
            is LimitReached -> "Free tier limit reached: ${error.limit}"
            is FileOperation -> "File error (${error.operation}): ${error.message}"
        }
    }
}
