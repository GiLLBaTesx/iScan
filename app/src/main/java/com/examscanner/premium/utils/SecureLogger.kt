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
}
