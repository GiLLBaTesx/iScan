package com.examscanner.premium.utils

import android.util.Log

/**
 * Secure Logger - Only logs in debug builds
 * In production, sensitive information is not logged
 */
object SecureLogger {
    // Note: BuildConfig.DEBUG is generated during build
    // For first-time builds, we check dynamically
    private fun isDebugMode(): Boolean {
        return try {
            val buildConfigClass = Class.forName("com.examscanner.premium.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            false // Default to production mode if unable to determine
        }
    }
    
    fun d(tag: String, message: String) {
        if (isDebugMode()) {
            Log.d(tag, message)
        }
    }
    
    fun i(tag: String, message: String) {
        if (isDebugMode()) {
            Log.i(tag, message)
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugMode()) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugMode()) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } else {
            // In production, you could send to crash reporting service
            // FirebaseCrashlytics.getInstance().log("$tag: $message")
            // throwable?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }
    
    /**
     * Log sensitive data - ONLY in debug builds
     * Automatically redacted in production
     */
    fun sensitive(tag: String, message: String) {
        if (isDebugMode()) {
            Log.d(tag, "[SENSITIVE] $message")
        }
    }

    /**
     * Measure and log the execution time of a database query (or any block),
     * supporting the performance monitoring called for by Requirement 19.7.
     *
     * The [block] is always executed and its result returned; the elapsed
     * duration is logged (debug builds only, via [d]) with the query [name] so
     * slow queries can be spotted during development without affecting release
     * behaviour. No query arguments are logged, so no sensitive data leaks.
     *
     * @param tag log tag.
     * @param name human-readable query name, e.g. "getExamsBySubjectPaged".
     * @param block the query to execute and time.
     */
    inline fun <T> logQueryTime(tag: String, name: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        val result = block()
        val elapsed = System.currentTimeMillis() - start
        d(tag, "Query '$name' took ${elapsed}ms")
        return result
    }
}
