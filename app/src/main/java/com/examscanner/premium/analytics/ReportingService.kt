package com.examscanner.premium.analytics

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reporting Service - Generate detailed reports for developer
 * 
 * This service aggregates analytics data and generates comprehensive reports
 * that can be viewed from the Firebase Console or a custom admin dashboard
 */
object ReportingService {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Generate daily summary report
     */
    suspend fun generateDailySummary(date: String = getTodayDate()): Map<String, Any> {
        try {
            val metricsSnapshot = firestore.collection("daily_metrics")
                .whereEqualTo("date", date)
                .get()
                .await()
            
            var totalUsers = 0
            var totalEvents = 0
            val featureUsage = mutableMapOf<String, Int>()
            val errorCounts = mutableMapOf<String, Int>()
            
            metricsSnapshot.documents.forEach { doc ->
                totalUsers++
                val data = doc.data ?: return@forEach
                
                totalEvents += (data["total_events"] as? Long)?.toInt() ?: 0
                
                // Aggregate events
                val events = data["events"] as? Map<String, Long>
                events?.forEach { (event, count) ->
                    featureUsage[event] = (featureUsage[event] ?: 0) + count.toInt()
                }
            }
            
            return mapOf(
                "date" to date,
                "total_users" to totalUsers,
                "total_events" to totalEvents,
                "feature_usage" to featureUsage,
                "error_counts" to errorCounts,
                "generated_at" to System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            return mapOf("error" to e.message.orEmpty())
        }
    }
    
    /**
     * Get recent developer alerts (errors, crashes)
     */
    suspend fun getRecentAlerts(limit: Int = 50): List<Map<String, Any>> {
        try {
            val alertsSnapshot = firestore.collection("developer_alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            return alertsSnapshot.documents.mapNotNull { it.data }
            
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    /**
     * Get user activity report
     */
    suspend fun getUserActivityReport(userId: String, days: Int = 7): Map<String, Any> {
        try {
            val startDate = getDateBefore(days)
            
            val metricsSnapshot = firestore.collection("daily_metrics")
                .whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .get()
                .await()
            
            var totalEvents = 0
            var totalDays = 0
            val dailyActivity = mutableListOf<Map<String, Any>>()
            
            metricsSnapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                totalDays++
                totalEvents += (data["total_events"] as? Long)?.toInt() ?: 0
                
                dailyActivity.add(
                    mapOf(
                        "date" to (data["date"] ?: ""),
                        "events" to (data["total_events"] ?: 0),
                        "categories" to (data["categories"] ?: emptyMap<String, Any>())
                    )
                )
            }
            
            return mapOf(
                "user_id" to userId,
                "period_days" to days,
                "active_days" to totalDays,
                "total_events" to totalEvents,
                "avg_events_per_day" to if (totalDays > 0) totalEvents / totalDays else 0,
                "daily_activity" to dailyActivity,
                "generated_at" to System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            return mapOf("error" to e.message.orEmpty())
        }
    }
    
    /**
     * Get most used features
     */
    suspend fun getTopFeatures(limit: Int = 10): List<Pair<String, Int>> {
        try {
            val today = getTodayDate()
            val metricsSnapshot = firestore.collection("daily_metrics")
                .whereEqualTo("date", today)
                .get()
                .await()
            
            val featureUsage = mutableMapOf<String, Int>()
            
            metricsSnapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val events = data["events"] as? Map<String, Long>
                events?.forEach { (event, count) ->
                    featureUsage[event] = (featureUsage[event] ?: 0) + count.toInt()
                }
            }
            
            return featureUsage.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key to it.value }
            
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    /**
     * Get error rate statistics
     */
    suspend fun getErrorStatistics(days: Int = 7): Map<String, Any> {
        try {
            val startTimestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            
            val errorsSnapshot = firestore.collection("analytics_events")
                .whereEqualTo("category", AnalyticsTracker.Category.ERROR)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .get()
                .await()
            
            val errorsByType = mutableMapOf<String, Int>()
            val errorsByUser = mutableMapOf<String, Int>()
            
            errorsSnapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val errorType = data["event_name"] as? String ?: "unknown"
                val userId = data["user_id"] as? String ?: "anonymous"
                
                errorsByType[errorType] = (errorsByType[errorType] ?: 0) + 1
                errorsByUser[userId] = (errorsByUser[userId] ?: 0) + 1
            }
            
            return mapOf(
                "period_days" to days,
                "total_errors" to errorsSnapshot.size(),
                "errors_by_type" to errorsByType,
                "affected_users" to errorsByUser.size,
                "generated_at" to System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            return mapOf("error" to e.message.orEmpty())
        }
    }
    
    /**
     * Send weekly summary to developer
     */
    suspend fun sendWeeklySummary() {
        try {
            val summary = generateWeeklySummary()
            
            firestore.collection("developer_reports")
                .add(
                    mapOf(
                        "report_type" to "weekly_summary",
                        "data" to summary,
                        "generated_at" to System.currentTimeMillis(),
                        "read" to false
                    )
                )
            
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    /**
     * Generate comprehensive weekly summary
     */
    private suspend fun generateWeeklySummary(): Map<String, Any> {
        val startDate = getDateBefore(7)
        val endDate = getTodayDate()
        
        val metricsSnapshot = firestore.collection("daily_metrics")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .await()
        
        var totalUsers = mutableSetOf<String>()
        var totalEvents = 0
        val topFeatures = mutableMapOf<String, Int>()
        
        metricsSnapshot.documents.forEach { doc ->
            val data = doc.data ?: return@forEach
            totalUsers.add(data["user_id"] as? String ?: "anonymous")
            totalEvents += (data["total_events"] as? Long)?.toInt() ?: 0
            
            val events = data["events"] as? Map<String, Long>
            events?.forEach { (event, count) ->
                topFeatures[event] = (topFeatures[event] ?: 0) + count.toInt()
            }
        }
        
        val errorStats = getErrorStatistics(7)
        
        return mapOf(
            "period" to "weekly",
            "start_date" to startDate,
            "end_date" to endDate,
            "total_active_users" to totalUsers.size,
            "total_events" to totalEvents,
            "top_features" to topFeatures.entries
                .sortedByDescending { it.value }
                .take(10)
                .associate { it.key to it.value },
            "error_statistics" to errorStats
        )
    }
    
    // Helper functions
    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    
    private fun getDateBefore(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}
