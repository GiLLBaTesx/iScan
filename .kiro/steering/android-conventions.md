---
inclusion: always
---

# iScan Android Development Conventions

## Project Overview
**iScan** is an Android exam scanning app built with Jetpack Compose, targeting educators who need to grade multiple-choice exams quickly using their phone camera.

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room with SQLCipher encryption
- **Authentication**: Firebase Auth
- **Analytics**: Custom Firebase Firestore implementation
- **Image Processing**: CameraX + ML Kit Text Recognition
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Color Palette - Azure Glass Theme

### ✅ ALWAYS USE THESE COLORS:
```kotlin
// Primary Colors
ElectricBlue = Color(0xFF03045E)     // Ultramarine midnight - primary brand
IcyCyan = Color(0xFF00B4D8)          // Icy azure - secondary/accent
LuminousAzure = Color(0xFF38BDF8)    // Bright azure - highlights

// Background Colors
IceWhite = Color(0xFFF4F9FF)         // Ice white - main background
SoftIce = Color(0xFFFAFAFF)          // Soft white - card backgrounds
FrostedWhite = Color(0xFFFFFFFF)     // Pure white - surfaces

// Glass Effect
GlassBase = Color(0xB8F0F9FF)        // Frosted glass effect
IceBlue = Color(0xFFEBF5FF)          // Light blue tint - info cards

// Text Colors
TextPrimaryIce                        // Dark text on light backgrounds
TextSecondaryIce                      // Secondary/muted text
TextTertiaryIce                       // Tertiary/subtle text

// Accent Colors
SuccessAzure = Color(0xFF00B4D8)     // Success states - same as IcyCyan
WarningAmber = Color(0xFFFFA726)     // Warnings (use sparingly)
ErrorRed = Color(0xFFEF5350)         // Errors
```

### ❌ NEVER USE THESE (Old/Inconsistent):
- ~~PrimaryBlue~~ → Use **ElectricBlue**
- ~~LightBlue~~ → Use **IceBlue**
- ~~SurfaceWhite~~ → Use **SoftIce** or **FrostedWhite**
- ~~BackgroundWhite~~ → Use **IceWhite**
- ~~SuccessGreen~~ → Use **SuccessAzure**
- ~~WarningOrange~~ → Use **WarningAmber** (sparingly)
- ~~TextPrimary~~ → Use **TextPrimaryIce**
- ~~TextSecondary~~ → Use **TextSecondaryIce**
- ~~MaterialTheme.colorScheme.primaryContainer~~ → Use explicit colors

### Color Usage Guidelines:
1. **Backgrounds**: IceWhite (screens), SoftIce (cards), FrostedWhite (surfaces)
2. **Text**: TextPrimaryIce (main), TextSecondaryIce (labels), TextTertiaryIce (hints)
3. **Buttons**: ElectricBlue (primary), IcyCyan (secondary), SuccessAzure (positive actions)
4. **Accents**: LuminousAzure (highlights), IcyCyan (interactive elements)
5. **Cards**: IceBlue (info cards), SoftIce (content cards)

## File Naming Conventions

### Screens
```
*Screen.kt - All UI screens
Examples:
- HomeScreen.kt
- ScanScreen.kt
- ExamDetailScreen.kt
- SettingsScreen.kt
```

### ViewModels
```
*ViewModel.kt - Business logic and state management
Examples:
- AuthViewModel.kt
- ExamViewModel.kt
```

### Repositories
```
*Repository.kt - Data access layer
Examples:
- ExamRepository.kt
- AuthRepository.kt
```

### Data Models
```
*Entity.kt - Room database entities
*Data.kt - Data classes
Examples:
- ExamEntity.kt
- StudentEntity.kt
- ExamData.kt
```

### UI Components
```
*Components.kt - Reusable Composables
Examples:
- GlassCard.kt
- ScanKeyComponents.kt
```

### Utilities
```
*Manager.kt - Complex utilities
*Helper.kt - Simple utilities
*Processor.kt - Data processors
Examples:
- EncryptionKeyManager.kt
- BubbleSheetProcessor.kt
```

## Architecture Patterns

### MVVM Structure
```
app/src/main/java/com/examscanner/premium/
├── ui/
│   ├── screens/          # Composable screens
│   ├── components/       # Reusable UI components
│   └── theme/           # Colors, Typography, Theme
├── data/                # Room entities, DAOs, repositories
├── auth/                # Authentication logic
├── scanner/             # Camera and image processing
├── utils/               # Utilities and helpers
├── analytics/           # Analytics tracking
├── MainActivity.kt      # Single activity
└── ExamScannerApplication.kt  # Application class
```

### Screen Pattern
```kotlin
@Composable
fun YourScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    // 1. State and ViewModels
    val viewModel: YourViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    
    // 2. Analytics tracking
    LaunchedEffect(Unit) {
        SessionTracker.getInstance(userId).trackScreenEnter("your_screen")
    }
    
    // 3. Scaffold with TopBar
    Scaffold(
        topBar = { /* TopAppBar */ }
    ) { padding ->
        // 4. Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(IceWhite)  // Always use theme colors
        ) {
            // Your UI
        }
    }
}
```

### Repository Pattern
```kotlin
class YourRepository(private val dao: YourDao) {
    suspend fun getData(): Result<Data> {
        return try {
            val data = dao.getData()
            Result.success(data)
        } catch (e: Exception) {
            // Track errors
            AnalyticsTracker.trackError(
                userId = currentUserId,
                errorType = "repository_error",
                errorMessage = e.message ?: "Unknown error"
            )
            Result.failure(e)
        }
    }
}
```

## Code Style

### Kotlin Best Practices
```kotlin
// ✅ Use val over var
val immutable = "value"

// ✅ Use data classes
data class ExamData(val id: String, val name: String)

// ✅ Use when expressions
val result = when (status) {
    Status.SUCCESS -> "Success"
    Status.FAILURE -> "Failed"
    else -> "Unknown"
}

// ✅ Use named parameters for clarity
createExam(
    name = "Math Quiz",
    questions = 20,
    timeLimit = 60
)

// ✅ Use trailing commas
val list = listOf(
    "Item 1",
    "Item 2",
    "Item 3",  // trailing comma
)
```

### Compose Best Practices
```kotlin
// ✅ Use remember for state
var isExpanded by remember { mutableStateOf(false) }

// ✅ Use LaunchedEffect for side effects
LaunchedEffect(key1) {
    // Side effect
}

// ✅ Hoist state up
@Composable
fun Parent() {
    var state by remember { mutableStateOf(false) }
    Child(state = state, onStateChange = { state = it })
}

// ✅ Use Modifier.fillMaxWidth(), not hardcoded sizes
Box(modifier = Modifier.fillMaxWidth())

// ✅ Use theme colors, never hardcoded
Text(color = TextPrimaryIce) // ✅
Text(color = Color.Black)    // ❌
```

## Security Guidelines

### Database Encryption
```kotlin
// ✅ All databases must be encrypted with SQLCipher
val database = Room.databaseBuilder(context, AppDatabase::class.java, "exam_db")
    .openHelperFactory(SupportFactory(passphrase))
    .build()
```

### Logging
```kotlin
// ✅ Use SecureLogger - auto-disables in production
SecureLogger.d("TAG", "Debug message")
SecureLogger.e("TAG", "Error message", throwable)

// ❌ Never use Log directly
Log.d("TAG", "message")  // ❌ Disabled in production anyway
```

### Sensitive Data
```kotlin
// ✅ Never log sensitive data
SecureLogger.sensitive("TAG", "User password: $password")  // Only in debug

// ❌ Never commit secrets
const val API_KEY = "abc123"  // ❌ Use BuildConfig or encrypted storage
```

### Authentication
```kotlin
// ✅ Always check auth before sensitive operations
if (FirebaseAuth.getInstance().currentUser != null) {
    // Proceed
}

// ✅ Use TrialGuard for trial/subscription checks
TrialGuard.isEligible(userId) { isEligible ->
    if (isEligible) {
        // Allow feature
    }
}
```

## Analytics Guidelines

### Track Important Events
```kotlin
// ✅ Track user actions
SessionTracker.getInstance(userId).trackAction(
    actionName = "exam_created",
    metadata = mapOf("questions_count" to 20)
)

// ✅ Track feature usage
AnalyticsTracker.trackFeatureUsage(
    userId = userId,
    featureName = "scan_completed",
    success = true
)

// ✅ Track errors
AnalyticsTracker.trackError(
    userId = userId,
    errorType = "scan_error",
    errorMessage = e.message ?: "Unknown error",
    stackTrace = e.stackTraceToString()
)
```

### Don't Track Personal Data
```kotlin
// ❌ Never track student names, scores, or exam content
AnalyticsTracker.trackEvent(
    eventName = "student_graded",
    properties = mapOf(
        "student_name" to "John Doe",  // ❌ PII
        "score" to 95                   // ❌ Personal data
    )
)

// ✅ Track aggregated/anonymous data only
AnalyticsTracker.trackEvent(
    eventName = "exam_graded",
    properties = mapOf(
        "exam_id" to examId,           // ✅ Anonymous
        "students_count" to 30         // ✅ Aggregated
    )
)
```

## Testing Guidelines

### When to Add Tests
- ❌ Don't add tests unless explicitly requested by user
- ✅ Test critical business logic (grading algorithms, encryption)
- ✅ Test data repositories
- ❌ Don't test simple UI components unless complex

### Example Test Pattern
```kotlin
@Test
fun `calculateScore returns correct percentage`() {
    val correct = 18
    val total = 20
    val score = calculateScore(correct, total)
    assertEquals(90.0, score, 0.01)
}
```

## Build & Deployment

### Build Variants
```
debug:   debuggable, no minification
release: minified, ProGuard enabled, signed
```

### ProGuard Rules
```proguard
# Keep analytics classes from obfuscation
-keep class com.examscanner.premium.analytics.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
```

## Common Patterns

### Navigation
```kotlin
// ✅ Use NavController for navigation
navController.navigate("exam_detail/$examId")

// ✅ Use type-safe routes
object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    fun examDetail(id: String) = "exam_detail/$id"
}
```

### Error Handling
```kotlin
// ✅ Use try-catch with analytics
try {
    performOperation()
} catch (e: Exception) {
    SessionTracker.getInstance(userId).trackError(
        errorType = "operation_error",
        errorMessage = e.message ?: "Failed",
        throwable = e
    )
    // Show error to user
}
```

### Async Operations
```kotlin
// ✅ Use coroutines with proper scope
viewModelScope.launch {
    try {
        val result = repository.getData()
        _state.value = State.Success(result)
    } catch (e: Exception) {
        _state.value = State.Error(e.message)
    }
}
```

## Documentation

### Code Comments
```kotlin
/**
 * Scans a bubble sheet and extracts student answers
 * 
 * @param imageUri URI of the captured image
 * @param answerKey Expected answers for validation
 * @return ScanResult with detected answers and score
 * @throws ScanException if image processing fails
 */
suspend fun scanBubbleSheet(imageUri: Uri, answerKey: List<String>): ScanResult
```

### File Headers
```kotlin
/**
 * HomeScreen - Main landing screen showing exams list
 * 
 * Features:
 * - Displays all exams with search/filter
 * - Quick scan button
 * - Recent activity summary
 */
```

## Git Commit Conventions

```
✅ feat: Add new feature
✅ fix: Fix bug
✅ refactor: Refactor code
✅ style: UI/styling changes
✅ docs: Documentation
✅ chore: Maintenance tasks

Examples:
"✅ feat: Add CSV export for exam results"
"🐛 fix: Resolve camera crash on Android 12"
"🎨 style: Update button colors to Azure Glass theme"
"📊 analytics: Track scan completion events"
```

---

**Remember**: Always use Azure Glass theme colors, never hardcode values, and track important events with analytics!
