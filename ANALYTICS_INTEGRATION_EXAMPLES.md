# Analytics Integration Examples

## How to Add Analytics to Your Screens

The analytics system is already initialized and running! Here are examples of how to track events in your screens.

## Basic Screen Tracking

### Automatic Tracking (Already Done!)
The `SessionTracker` automatically tracks:
- App launches
- Session duration
- Screen enter/exit times

### Manual Screen View Tracking

Add this to your Composable screens:

```kotlin
@Composable
fun YourScreen() {
    val sessionTracker = remember { SessionTracker.getInstance(getCurrentUserId()) }
    
    LaunchedEffect(Unit) {
        sessionTracker.trackScreenEnter("your_screen_name")
    }
    
    // Rest of your screen code...
}
```

## Feature Usage Tracking

### Example: Track Exam Creation

```kotlin
// In your ViewModel or Repository
fun createExam(examData: ExamData) {
    val startTime = System.currentTimeMillis()
    
    try {
        // Your exam creation logic
        val examId = repository.insertExam(examData)
        
        val duration = System.currentTimeMillis() - startTime
        
        // Track success
        SessionTracker.getInstance(userId).trackFeature(
            featureName = "exam_created",
            success = true,
            duration = duration,
            metadata = mapOf(
                "exam_name" to examData.name,
                "questions_count" to examData.questionsCount,
                "has_melc" to (examData.melcId != null)
            )
        )
        
    } catch (e: Exception) {
        // Track failure
        SessionTracker.getInstance(userId).trackError(
            errorType = "exam_creation_error",
            errorMessage = e.message ?: "Unknown error",
            throwable = e
        )
    }
}
```

### Example: Track Scan Operation

```kotlin
fun scanBubbleSheet(imageUri: Uri, examId: String) {
    val startTime = System.currentTimeMillis()
    
    try {
        // Your scanning logic
        val result = bubbleSheetProcessor.process(imageUri)
        
        val processingTime = System.currentTimeMillis() - startTime
        
        // Track scan completion
        AnalyticsTracker.trackScan(
            userId = currentUser?.uid,
            examId = examId,
            questionsCount = result.detectedAnswers.size,
            processingTime = processingTime,
            success = true
        )
        
    } catch (e: Exception) {
        val processingTime = System.currentTimeMillis() - startTime
        
        // Track scan failure
        AnalyticsTracker.trackScan(
            userId = currentUser?.uid,
            examId = examId,
            questionsCount = 0,
            processingTime = processingTime,
            success = false,
            errorMessage = e.message
        )
    }
}
```

### Example: Track Button Clicks

```kotlin
Button(onClick = {
    // Track the action
    SessionTracker.getInstance(userId).trackAction(
        actionName = "export_csv_clicked",
        metadata = mapOf(
            "exam_id" to examId,
            "students_count" to studentsCount
        )
    )
    
    // Then perform the action
    onExportCsv()
}) {
    Text("Export CSV")
}
```

### Example: Track Export Operations

```kotlin
suspend fun exportToCsv(examId: String): Boolean {
    return try {
        // Export logic
        val csvFile = generateCsv(examId)
        
        // Track success
        AnalyticsTracker.trackFeatureUsage(
            userId = currentUser?.uid,
            featureName = AnalyticsTracker.Event.CSV_EXPORTED,
            success = true,
            metadata = mapOf(
                "exam_id" to examId,
                "file_size" to csvFile.length()
            )
        )
        
        true
    } catch (e: Exception) {
        // Track failure
        AnalyticsTracker.trackError(
            userId = currentUser?.uid,
            errorType = AnalyticsTracker.Event.EXPORT_ERROR,
            errorMessage = e.message ?: "CSV export failed",
            stackTrace = e.stackTraceToString()
        )
        
        false
    }
}
```

## Where to Add Tracking

### 1. **MainActivity.kt** (Already Done ✅)
- Session tracking initialized in ExamScannerApplication

### 2. **HomeScreen** 
```kotlin
LaunchedEffect(Unit) {
    SessionTracker.getInstance(userId).trackScreenEnter("home_screen")
}
```

### 3. **ScanScreen / CameraScreen**
```kotlin
LaunchedEffect(Unit) {
    SessionTracker.getInstance(userId).trackScreenEnter("scan_screen")
}

// When scan starts
Button(onClick = {
    SessionTracker.getInstance(userId).trackAction("scan_started")
    startScanning()
})
```

### 4. **ExamDetailScreen**
```kotlin
LaunchedEffect(examId) {
    SessionTracker.getInstance(userId).trackScreenEnter("exam_detail_screen")
    
    AnalyticsTracker.trackEvent(
        userId = userId,
        eventName = "exam_detail_viewed",
        category = AnalyticsTracker.Category.SCREEN_VIEW,
        properties = mapOf("exam_id" to examId)
    )
}
```

### 5. **SettingsScreen**
```kotlin
// Track settings changes
fun updateSetting(key: String, value: Any) {
    AnalyticsTracker.trackEvent(
        userId = userId,
        eventName = "setting_changed",
        category = AnalyticsTracker.Category.USER_ACTION,
        properties = mapOf(
            "setting_key" to key,
            "new_value" to value.toString()
        )
    )
    
    // Save setting
    saveSettingToPreferences(key, value)
}
```

### 6. **TemplateGeneratorScreen**
```kotlin
LaunchedEffect(Unit) {
    SessionTracker.getInstance(userId).trackScreenEnter("template_generator_screen")
}

Button(onClick = {
    SessionTracker.getInstance(userId).trackFeature(
        featureName = "template_generated",
        success = true,
        metadata = mapOf(
            "questions" to totalQuestions,
            "choices" to choicesPerQuestion
        )
    )
    
    onGenerate(totalQuestions, choicesPerQuestion, templateName)
})
```

### 7. **BackupManagementScreen**
```kotlin
// Track backup creation
fun createBackup() {
    try {
        backupManager.createBackup()
        
        SessionTracker.getInstance(userId).trackFeature(
            featureName = "backup_created",
            success = true
        )
    } catch (e: Exception) {
        SessionTracker.getInstance(userId).trackError(
            errorType = "backup_error",
            errorMessage = e.message ?: "Backup failed",
            throwable = e
        )
    }
}
```

## Helper Function for getCurrentUserId()

```kotlin
// Add this helper function in your MainActivity or as an extension
fun getCurrentUserId(): String? {
    return FirebaseAuth.getInstance().currentUser?.uid
}
```

## Performance Tracking

Track performance-critical operations:

```kotlin
fun processLargeDataset() {
    val startTime = System.currentTimeMillis()
    
    // Your processing logic
    processData()
    
    val duration = System.currentTimeMillis() - startTime
    
    // Track performance
    AnalyticsTracker.trackEvent(
        userId = currentUser?.uid,
        eventName = AnalyticsTracker.Event.PROCESSING_TIME,
        category = AnalyticsTracker.Category.PERFORMANCE,
        properties = mapOf(
            "operation" to "data_processing",
            "duration_ms" to duration,
            "items_count" to itemsCount
        )
    )
}
```

## Error Boundary Pattern

Wrap critical operations:

```kotlin
suspend fun <T> trackOperation(
    operationName: String,
    operation: suspend () -> T
): Result<T> {
    return try {
        val startTime = System.currentTimeMillis()
        val result = operation()
        val duration = System.currentTimeMillis() - startTime
        
        AnalyticsTracker.trackFeatureUsage(
            userId = getCurrentUserId(),
            featureName = operationName,
            success = true,
            metadata = mapOf("duration_ms" to duration)
        )
        
        Result.success(result)
    } catch (e: Exception) {
        AnalyticsTracker.trackError(
            userId = getCurrentUserId(),
            errorType = "${operationName}_error",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
        )
        
        Result.failure(e)
    }
}

// Usage:
val result = trackOperation("import_csv") {
    importCsvFile(fileUri)
}
```

## Best Practices

### ✅ DO:
- Track important user actions (create, delete, export)
- Track errors with full context
- Track performance of critical operations
- Use descriptive event names
- Include relevant metadata

### ❌ DON'T:
- Track personal data (student names, scores, answers)
- Track every single UI interaction
- Track sensitive information
- Create too many unique event names
- Track in tight loops (will spam analytics)

## Testing Analytics

```kotlin
// In debug builds, you can verify events are sent:
if (BuildConfig.DEBUG) {
    Log.d("Analytics", "Event tracked: $eventName")
}

// Check Firebase Console → Firestore → analytics_events collection
// You should see events appearing in real-time
```

## Privacy Compliance

Always ensure:
1. Privacy Policy mentions analytics collection ✅ (Already added)
2. No personal student data is tracked ✅
3. Users can opt-out if required (add later if needed)
4. Data is anonymized ✅
5. Compliance with GDPR/COPPA ✅

---

## Quick Start Checklist

- [x] Analytics system initialized
- [x] Session tracking active
- [x] Error reporting configured
- [ ] Add screen tracking to main screens (optional but recommended)
- [ ] Add feature tracking to key actions (optional but recommended)
- [ ] Test events in Firebase Console
- [ ] Review reports weekly

The system is ready to go! Events are already being tracked automatically. You can add more specific tracking as needed.
