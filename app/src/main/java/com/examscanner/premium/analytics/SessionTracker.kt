package com.examscanner.premium.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Session Tracker - Automatically tracks app sessions and user activity
 * 
 * Features:
 * - Automatic session start/end detection
 * - Screen view tracking
 * - Session duration calculation
 * - Activity metrics per session
 */
class SessionTracker(private val userId: String?) : DefaultLifecycleObserver {
    
    private var sessionStartTime: Long = 0
    private var currentScreenStartTime: Long = 0
    private var currentScreen: String = ""
    private var screenViewsCount = 0
    private var actionsCount = 0
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App moved to foreground
        sessionStartTime = System.currentTimeMillis()
        screenViewsCount = 0
        actionsCount = 0
        
        AnalyticsTracker.trackEvent(
            userId = userId,
            eventName = AnalyticsTracker.Event.APP_LAUNCH,
            category = AnalyticsTracker.Category.PERFORMANCE,
            properties = mapOf("timestamp" to sessionStartTime)
        )
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App moved to background
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        
        // Track current screen duration if there's one
        if (currentScreen.isNotEmpty()) {
            trackScreenExit()
        }
        
        // Track session end
        AnalyticsTracker.trackSession(
            userId = userId,
            sessionDuration = sessionDuration,
            screenViews = screenViewsCount,
            actions = actionsCount
        )
        
        // Generate usage report
        AnalyticsTracker.generateUsageReport(userId, "session")
    }
    
    /**
     * Track when user enters a screen
     */
    fun trackScreenEnter(screenName: String) {
        // Track exit from previous screen
        if (currentScreen.isNotEmpty()) {
            trackScreenExit()
        }
        
        currentScreen = screenName
        currentScreenStartTime = System.currentTimeMillis()
        screenViewsCount++
        
        AnalyticsTracker.trackScreenView(userId, screenName)
    }
    
    /**
     * Track when user exits a screen
     */
    private fun trackScreenExit() {
        if (currentScreen.isEmpty()) return
        
        val duration = System.currentTimeMillis() - currentScreenStartTime
        
        AnalyticsTracker.trackScreenView(
            userId = userId,
            screenName = "${currentScreen}_exit",
            duration = duration
        )
        
        currentScreen = ""
    }
    
    /**
     * Track user action (button click, etc.)
     */
    fun trackAction(actionName: String, metadata: Map<String, Any> = emptyMap()) {
        actionsCount++
        
        AnalyticsTracker.trackEvent(
            userId = userId,
            eventName = actionName,
            category = AnalyticsTracker.Category.USER_ACTION,
            properties = metadata
        )
    }
    
    /**
     * Track feature usage
     */
    fun trackFeature(
        featureName: String,
        success: Boolean,
        duration: Long? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val properties = mutableMapOf<String, Any>()
        properties.putAll(metadata)
        duration?.let { properties["duration_ms"] = it }
        
        AnalyticsTracker.trackFeatureUsage(userId, featureName, success, properties)
    }
    
    /**
     * Track error with context
     */
    fun trackError(
        errorType: String,
        errorMessage: String,
        throwable: Throwable? = null,
        context: Map<String, Any> = emptyMap()
    ) {
        val stackTrace = throwable?.stackTraceToString()
        val contextWithScreen = context.toMutableMap()
        contextWithScreen["current_screen"] = currentScreen
        
        AnalyticsTracker.trackError(
            userId = userId,
            errorType = errorType,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            context = contextWithScreen
        )
    }
    
    companion object {
        private var instance: SessionTracker? = null
        
        fun getInstance(userId: String?): SessionTracker {
            if (instance == null || instance?.userId != userId) {
                instance = SessionTracker(userId)
            }
            return instance!!
        }
    }
}
