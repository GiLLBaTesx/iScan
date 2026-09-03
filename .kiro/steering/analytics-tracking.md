---
inclusion: fileMatch
fileMatchPattern: '*Screen.kt'
---

# Analytics Tracking Standards

## Overview
This guide is automatically included when working with Screen files. All screens should implement proper analytics tracking.

## Automatic Tracking (Already Working)
- ✅ App launches
- ✅ Session duration
- ✅ App foreground/background events

## Required Screen Tracking

Every screen MUST track screen entry:

```kotlin
@Composable
fun YourScreen() {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    
    LaunchedEffect(Unit) {
        SessionTracker.getInstance(userId).trackScreenEnter("your_screen_name")
    }
    
    // Rest of screen...
}
```

### Screen Naming Convention
Use snake_case for screen names:
- `home_screen`
- `exam_detail_screen`
- `settings_screen`
- `camera_screen`
- `results_screen`
- `template_generator_screen`

## Event Tracking

### User Actions (Button Clicks, Menu Items)
```kotlin
Button(onClick = {
    SessionTracker.getInstance(userId).trackAction(
        actionName = "create_exam_clicked",
        metadata = mapOf("source" to "home_screen")
    )
    onCreateExam()
}) {
    Text("Create Exam")
}
```

### Feature Usage (Operations with Success/Failure)
```kotlin
fun performOperation() {
    val startTime = System.currentTimeMillis()
    
    try {
        // Operation logic
        doSomething()
        
        val duration = System.currentTimeMillis() - startTime
        
        SessionTracker.getInstance(userId).trackFeature(
            featureName = "operation_completed",
            success = true,
            duration = duration,
            metadata = mapOf("items_count" to itemsCount)
        )
    } catch (e: Exception) {
        SessionTracker.getInstance(userId).trackError(
            errorType = "operation_error",
            errorMessage = e.message ?: "Unknown error",
            throwable = e
        )
    }
}
```

### Error Tracking (Always Track Errors)
```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    SessionTracker.getInstance(userId).trackError(
        errorType = "operation_failure",
        errorMessage = e.message ?: "Unknown error",
        throwable = e,
        context = mapOf(
            "screen" to "current_screen_name",
            "action" to "specific_action"
        )
    )
    
    // Show error to user
    showErrorDialog(e.message)
}
```

## What to Track

### ✅ ALWAYS Track:
- Screen views (entry/exit)
- User actions (button clicks, menu selections)
- Feature usage (create/delete/edit operations)
- Errors and exceptions
- Performance metrics (slow operations)
- Export/import operations
- Settings changes

### ✅ SOMETIMES Track:
- Navigation patterns (where users go)
- Search queries (aggregated, not individual)
- Filter/sort preferences
- Time spent on complex tasks

### ❌ NEVER Track:
- Student names, IDs, or personal info
- Exam questions or answers
- Test scores or grades
- User passwords or credentials
- Personal identifiable information (PII)
- Device location

## Event Categories

Use predefined categories from `AnalyticsTracker.Category`:

```kotlin
Category.SCREEN_VIEW      // Screen navigation
Category.USER_ACTION      // Button clicks, interactions
Category.FEATURE_USAGE    // Feature operations
Category.ERROR            // Errors and exceptions
Category.PERFORMANCE      // Performance metrics
Category.SCAN             // Scanning operations
Category.EXPORT           // Export operations
Category.TEMPLATE         // Template generation
```

## Event Names

Use predefined event names from `AnalyticsTracker.Event`:

### Screen Views
- `Event.HOME_SCREEN`
- `Event.SCAN_SCREEN`
- `Event.RESULTS_SCREEN`
- `Event.SETTINGS_SCREEN`
- `Event.TEMPLATE_GENERATOR`

### User Actions
- `Event.EXAM_CREATED`
- `Event.EXAM_SCANNED`
- `Event.EXAM_DELETED`
- `Event.ANSWER_KEY_SET`
- `Event.RESULTS_EXPORTED`
- `Event.BACKUP_CREATED`
- `Event.BACKUP_RESTORED`

### Features
- `Event.SCAN_STARTED`
- `Event.SCAN_COMPLETED`
- `Event.SCAN_FAILED`
- `Event.TEMPLATE_GENERATED`
- `Event.CSV_IMPORTED`
- `Event.PDF_EXPORTED`

### Errors
- `Event.SCAN_ERROR`
- `Event.EXPORT_ERROR`
- `Event.DATABASE_ERROR`
- `Event.AUTH_ERROR`

## Metadata Best Practices

### ✅ Good Metadata:
```kotlin
mapOf(
    "exam_id" to examId,              // Anonymous identifier
    "questions_count" to 20,           // Aggregate data
    "has_answer_key" to true,          // Boolean flags
    "duration_ms" to 2500,             // Performance metrics
    "success" to true                  // Operation result
)
```

### ❌ Bad Metadata:
```kotlin
mapOf(
    "student_name" to "John Doe",      // ❌ PII
    "exam_content" to "What is 2+2?",  // ❌ Exam data
    "score" to 95,                     // ❌ Personal data
    "email" to "user@example.com"      // ❌ PII
)
```

## Performance Tracking

Track operations that might be slow:

```kotlin
fun performSlowOperation() {
    val startTime = System.currentTimeMillis()
    
    // Your operation
    val result = processData()
    
    val duration = System.currentTimeMillis() - startTime
    
    // Track if operation took more than 1 second
    if (duration > 1000) {
        AnalyticsTracker.trackEvent(
            userId = userId,
            eventName = "slow_operation",
            category = AnalyticsTracker.Category.PERFORMANCE,
            properties = mapOf(
                "operation" to "data_processing",
                "duration_ms" to duration,
                "items_count" to result.size
            )
        )
    }
}
```

## Scan Tracking (Special Case)

Scan operations have a dedicated tracking method:

```kotlin
fun scanBubbleSheet() {
    val startTime = System.currentTimeMillis()
    
    try {
        val result = processor.scan(imageUri)
        val processingTime = System.currentTimeMillis() - startTime
        
        AnalyticsTracker.trackScan(
            userId = userId,
            examId = examId,
            questionsCount = result.answers.size,
            processingTime = processingTime,
            success = true
        )
    } catch (e: Exception) {
        val processingTime = System.currentTimeMillis() - startTime
        
        AnalyticsTracker.trackScan(
            userId = userId,
            examId = examId,
            questionsCount = 0,
            processingTime = processingTime,
            success = false,
            errorMessage = e.message
        )
    }
}
```

## Common Patterns

### Dialog Actions
```kotlin
AlertDialog(
    onDismissRequest = {
        SessionTracker.getInstance(userId).trackAction("dialog_dismissed")
        onDismiss()
    },
    onConfirmClick = {
        SessionTracker.getInstance(userId).trackAction(
            actionName = "delete_exam_confirmed",
            metadata = mapOf("exam_id" to examId)
        )
        onConfirm()
    }
)
```

### Navigation Actions
```kotlin
Button(onClick = {
    SessionTracker.getInstance(userId).trackAction(
        actionName = "navigate_to_settings",
        metadata = mapOf("from_screen" to currentScreen)
    )
    navController.navigate("settings")
})
```

### Settings Changes
```kotlin
fun updateSetting(key: String, value: Any) {
    AnalyticsTracker.trackEvent(
        userId = userId,
        eventName = "setting_changed",
        category = AnalyticsTracker.Category.USER_ACTION,
        properties = mapOf(
            "setting_key" to key,
            "setting_value" to value.toString()
        )
    )
    
    // Save setting
    preferences.edit().putString(key, value.toString()).apply()
}
```

## Testing Analytics

In debug builds, verify events are sent:

```kotlin
if (BuildConfig.DEBUG) {
    Log.d("Analytics", "Event: $eventName, Metadata: $metadata")
}
```

Check Firebase Console:
1. Open Firestore Database
2. Navigate to `analytics_events` collection
3. Verify your events appear with correct data

## Quick Reference

| What | How |
|------|-----|
| Screen view | `SessionTracker.getInstance(userId).trackScreenEnter("screen_name")` |
| Button click | `trackAction("action_name", metadata)` |
| Feature usage | `trackFeature("feature_name", success, duration, metadata)` |
| Error | `trackError("error_type", message, throwable, context)` |
| Scan | `AnalyticsTracker.trackScan(userId, examId, count, time, success)` |

---

**Remember**: Track behaviors, not personal data. When in doubt, don't track it!
