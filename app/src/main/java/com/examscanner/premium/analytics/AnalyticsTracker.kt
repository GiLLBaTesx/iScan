package com.examscanner.premium.analytics

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Analytics Tracker - Tracks user activity, metrics, and sends reports to developer
 * 
 * Features:
 * - Event tracking with timestamps and metadata
 * - User activity metrics (frequency, duration)
 * - Screen/feature usage tracking
 * - Error and crash reporting
 * - Daily/weekly aggregated reports sent to developer
 * 
 * Privacy: All data is anonymized and users are informed via Privacy Policy
 */
object AnalyticsTracker {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Event Categories
    object Category {
        const val SCREEN_VIEW = "screen_view"
        const val USER_ACTION = "user_action"
        const val FEATURE_USAGE = "feature_usage"
        const val ERROR = "error"
        const val PERFORMANCE = "performance"
        const val SCAN = "scan"
        const val EXPORT = "export"
        const val TEMPLATE = "template"
    }
    
    // Common Event Names
    object Event {
        // Screen Views
        const val HOME_SCREEN = "home_screen_viewed"
        const val SCAN_SCREEN = "scan_screen_viewed"
        const val RESULTS_SCREEN = "results_screen_viewed"
        const val SETTINGS_SCREEN = "settings_screen_viewed"
        const val TEMPLATE_GENERATOR = "template_generator_viewed"
        
        // User Actions
        const val EXAM_CREATED = "exam_created"
        const val EXAM_SCANNED = "exam_scanned"
        const val EXAM_DELETED = "exam_deleted"
        const val ANSWER_KEY_SET = "answer_key_set"
        const val RESULTS_EXPORTED = "results_exported"
        const val BACKUP_CREATED = "backup_created"
        const val BACKUP_RESTORED = "backup_restored"
        
        // Features
        const val SCAN_STARTED = "scan_started"
        const val SCAN_COMPLETED = "scan_completed"
        const val SCAN_FAILED = "scan_failed"
        const val TEMPLATE_GENERATED = "template_generated"
        const val CSV_IMPORTED = "csv_imported"
        const val PDF_EXPORTED = "pdf_exported"
        
        // Performance
        const val SCAN_DURATION = "scan_duration_ms"
        const val PROCESSING_TIME = "processing_time_ms"
        const val APP_LAUNCH = "app_launch"
        
        // Errors
        const val SCAN_ERROR = "scan_error"
        const val EXPORT_ERROR = "export_error"
        const val DATABASE_ERROR = "database_error"
        const val AUTH_ERROR = "auth_error"
    }
    
    /**
     * Track an event with optional metadata
     */
    fun trackEvent(
        userId: String?,
        eventName: String,
        category: String,
        properties: Map<String, Any> = emptyMap()
    ) {
        scope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                
                val eventData = hashMapOf(
                    "event_name" to eventName,
                    "category" to category,
                    "timestamp" to timestamp,
                    "datetime" to dateFormat.format(Date(timestamp)),
                    "user_id" to (userId ?: "anonymous"),
                    "properties" to properties
                )
                
                // Store in user's analytics collection
                firestore.collection("analytics_events")
                    .add(eventData)
                
                // Update daily metrics
                updateDailyMetrics(userId, category, eventName)
                
            } catch (e: Exception) {
                // Silently fail - analytics should never crash the app
            }
        }
    }
    
    /**
     * Track screen view
     */
    fun trackScreenView(userId: String?, screenName: String, duration: Long? = null) {
        val properties = mutableMapOf<String, Any>(
            "screen_name" to screenName
        )
        duration?.let { properties["duration_ms"] = it }
        
        trackEvent(userId, screenName, Category.SCREEN_VIEW, properties)
    }
    
    /**
     * Track feature usage with metrics
     */
    fun trackFeatureUsage(
        userId: String?,
        featureName: String,
        success: Boolean,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val properties = mutableMapOf<String, Any>(
            "feature" to featureName,
            "success" to success
        )
        properties.putAll(metadata)
        
        trackEvent(userId, featureName, Category.FEATURE_USAGE, properties)
    }
    
    /**
     * Track scan activity with details
     */
    fun trackScan(
        userId: String?,
        examId: String,
        questionsCount: Int,
        processingTime: Long,
        success: Boolean,
        errorMessage: String? = null
    ) {
        val properties = mutableMapOf<String, Any>(
            "exam_id" to examId,
            "questions_count" to questionsCount,
            "processing_time_ms" to processingTime,
            "success" to success
        )
        errorMessage?.let { properties["error"] = it }
        
        val eventName = if (success) Event.SCAN_COMPLETED else Event.SCAN_FAILED
        trackEvent(userId, eventName, Category.SCAN, properties)
    }
    
    /**
     * Track errors for debugging
     */
    fun trackError(
        userId: String?,
        errorType: String,
        errorMessage: String,
        stackTrace: String? = null,
        context: Map<String, Any> = emptyMap()
    ) {
        val properties = mutableMapOf<String, Any>(
            "error_type" to errorType,
            "error_message" to errorMessage
        )
        stackTrace?.let { properties["stack_trace"] = it }
        properties.putAll(context)
        
        trackEvent(userId, errorType, Category.ERROR, properties)
        
        // Also send to developer alerts collection for immediate notification
        sendDeveloperAlert(userId, errorType, errorMessage, properties)
    }
    
    /**
     * Update daily aggregated metrics
     */
    private fun updateDailyMetrics(userId: String?, category: String, eventName: String) {
        scope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val docId = "${userId ?: "anonymous"}_$today"
                
                val metricsRef = firestore.collection("daily_metrics").document(docId)
                
                val updates = hashMapOf<String, Any>(
                    "user_id" to (userId ?: "anonymous"),
                    "date" to today,
                    "last_updated" to System.currentTimeMillis(),
                    "events.$eventName" to com.google.firebase.firestore.FieldValue.increment(1),
                    "categories.$category" to com.google.firebase.firestore.FieldValue.increment(1),
                    "total_events" to com.google.firebase.firestore.FieldValue.increment(1)
                )
                
                metricsRef.set(updates, SetOptions.merge())
                
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    /**
     * Send developer alert for critical events
     */
    private fun sendDeveloperAlert(
        userId: String?,
        alertType: String,
        message: String,
        metadata: Map<String, Any>
    ) {
        scope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                
                val alertData = hashMapOf(
                    "alert_type" to alertType,
                    "message" to message,
                    "user_id" to (userId ?: "anonymous"),
                    "timestamp" to timestamp,
                    "datetime" to dateFormat.format(Date(timestamp)),
                    "metadata" to metadata,
                    "severity" to determineSeverity(alertType),
                    "read" to false
                )
                
                firestore.collection("developer_alerts")
                    .add(alertData)
                
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    /**
     * Generate and send usage report to developer
     */
    fun generateUsageReport(userId: String?, reportType: String = "daily") {
        scope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val today = dateFormat.format(Date())
                
                // Fetch user's daily metrics
                val metricsSnapshot = firestore.collection("daily_metrics")
                    .whereEqualTo("user_id", userId ?: "anonymous")
                    .whereEqualTo("date", today)
                    .get()
                    .await()
                
                if (!metricsSnapshot.isEmpty) {
                    val metrics = metricsSnapshot.documents[0].data ?: return@launch
                    
                    val reportData = hashMapOf(
                        "report_type" to reportType,
                        "user_id" to (userId ?: "anonymous"),
                        "date" to today,
                        "generated_at" to System.currentTimeMillis(),
                        "metrics" to metrics,
                        "read" to false
                    )
                    
                    firestore.collection("usage_reports")
                        .add(reportData)
                }
                
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    /**
     * Track app session
     */
    fun trackSession(userId: String?, sessionDuration: Long, screenViews: Int, actions: Int) {
        val properties = mapOf(
            "session_duration_ms" to sessionDuration,
            "screen_views" to screenViews,
            "actions_count" to actions
        )
        
        trackEvent(userId, "session_ended", Category.PERFORMANCE, properties)
    }
    
    /**
     * Determine severity level for alerts
     */
    private fun determineSeverity(alertType: String): String {
        return when {
            alertType.contains("crash", ignoreCase = true) -> "critical"
            alertType.contains("error", ignoreCase = true) -> "high"
            alertType.contains("warning", ignoreCase = true) -> "medium"
            else -> "low"
        }
    }
}
