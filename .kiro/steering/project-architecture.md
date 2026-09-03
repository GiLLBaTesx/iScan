---
inclusion: manual
---

# iScan Project Architecture

> **Note**: This is a manual inclusion steering file. Reference it with `#project-architecture` when you need detailed architecture information.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  (Jetpack Compose UI - MVVM Pattern)                        │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Screens    │  │  ViewModels  │  │  Components  │     │
│  │  (UI Layer)  │  │  (Business)  │  │ (Reusable)   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  (Business Logic & Use Cases)                               │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Repositories │  │   Managers   │  │  Processors  │     │
│  │ (Data Access)│  │  (Business)  │  │ (Operations) │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  (Storage & External Services)                              │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Room Database│  │   Firebase   │  │   Storage    │     │
│  │ (SQLCipher)  │  │ (Auth/Cloud) │  │   (Local)    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
app/src/main/java/com/examscanner/premium/
│
├── MainActivity.kt                    # Single Activity (Navigation Host)
├── ExamScannerApplication.kt         # Application class (DI, init)
│
├── ui/                                # Presentation Layer
│   ├── screens/                       # Full-screen Composables
│   │   ├── HomeScreen.kt             # Main exam list
│   │   ├── ScanScreen.kt             # Camera/scanner UI
│   │   ├── ExamDetailScreen.kt       # Single exam view
│   │   ├── ResultsScreen.kt          # Scan results
│   │   ├── SettingsScreen.kt         # App settings
│   │   ├── TemplateGeneratorScreen.kt# Custom templates
│   │   ├── AboutScreen.kt            # App info
│   │   ├── PrivacyPolicyScreen.kt    # Privacy policy
│   │   └── TermsOfServiceScreen.kt   # Terms
│   │
│   ├── components/                    # Reusable UI components
│   │   ├── GlassCard.kt              # Glassmorphism card
│   │   └── ScanKeyComponents.kt      # Answer key UI
│   │
│   └── theme/                         # Design system
│       ├── Color.kt                   # Azure Glass colors
│       ├── Type.kt                    # Typography
│       └── Theme.kt                   # Material3 theme
│
├── data/                              # Data Layer
│   ├── AppDatabase.kt                # Room database (encrypted)
│   ├── ExamDao.kt                    # Data Access Objects
│   ├── ExamRepository.kt             # Repository pattern
│   │
│   ├── entities/                      # Database entities
│   │   ├── ExamEntity.kt             # Exam table
│   │   ├── StudentEntity.kt          # Student table
│   │   ├── ScanEntity.kt             # Scan results
│   │   ├── AnswerKeyEntity.kt        # Answer keys
│   │   ├── MelcEntity.kt             # Philippine MELCs
│   │   └── TemplateEntity.kt         # Custom templates
│   │
│   └── BuiltInData.kt                # Sample/initial data
│
├── auth/                              # Authentication
│   ├── AuthRepository.kt             # Firebase Auth wrapper
│   ├── AuthViewModel.kt              # Auth state management
│   └── AuthState.kt                  # Auth sealed classes
│
├── scanner/                           # Image Processing
│   ├── BubbleSheetProcessor.kt       # Scan algorithm
│   ├── CameraScreen.kt               # Camera UI
│   └── ImagePreprocessor.kt          # Image enhancement
│
├── analytics/                         # Analytics System
│   ├── AnalyticsTracker.kt           # Event tracking
│   ├── SessionTracker.kt             # Session management
│   └── ReportingService.kt           # Report generation
│
└── utils/                             # Utilities
    ├── EncryptionKeyManager.kt       # Encryption keys
    ├── SecureLogger.kt               # Production-safe logging
    ├── TrialGuard.kt                 # Trial/subscription
    ├── BackupManager.kt              # Backup/restore
    └── TemplatePDFGenerator.kt       # PDF generation
```

## Core Components

### 1. Single Activity Architecture
```kotlin
MainActivity.kt
├── NavHost (Jetpack Navigation)
│   ├── Authenticated Routes
│   │   ├── home → HomeScreen
│   │   ├── scan → ScanScreen
│   │   ├── exam_detail/{id} → ExamDetailScreen
│   │   └── settings → SettingsScreen
│   │
│   └── Public Routes
│       ├── login → LoginScreen
│       ├── about → AboutScreen
│       └── privacy_policy → PrivacyPolicyScreen
```

### 2. Room Database (Encrypted)
```kotlin
AppDatabase
├── Tables:
│   ├── exams (ExamEntity)
│   ├── students (StudentEntity)
│   ├── scans (ScanEntity)
│   ├── answer_keys (AnswerKeyEntity)
│   ├── melcs (MelcEntity)
│   ├── student_melc_mastery (StudentMelcMasteryEntity)
│   └── templates (TemplateEntity)
│
└── Encryption: SQLCipher
    └── Passphrase: Managed by EncryptionKeyManager
```

### 3. Firebase Integration
```kotlin
Firebase Services:
├── Authentication (Email/Password)
│   └── Used for: User accounts, trial tracking
│
├── Firestore
│   ├── users/{userId} → User profiles
│   ├── device_trials/{trialId} → Trial tracking
│   ├── analytics_events/{eventId} → Analytics
│   ├── daily_metrics/{metricId} → Usage stats
│   └── developer_alerts/{alertId} → Error reports
│
└── Security Rules
    └── Require authentication for all operations
```

### 4. Data Flow (MVVM)

```
┌──────────────┐
│    Screen    │ ← User interacts
└──────┬───────┘
       │ observes State
       ↓
┌──────────────┐
│  ViewModel   │ ← Holds UI state
└──────┬───────┘
       │ calls methods
       ↓
┌──────────────┐
│  Repository  │ ← Business logic
└──────┬───────┘
       │ queries data
       ↓
┌──────────────┐
│   Database   │ ← Data persistence
└──────────────┘
```

Example:
```kotlin
// Screen observes state
val exams by viewModel.exams.collectAsState()

// ViewModel exposes state
val exams = repository.getAllExams()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// Repository accesses database
fun getAllExams() = examDao.getAllExams()

// DAO queries Room
@Query("SELECT * FROM exams ORDER BY created_at DESC")
fun getAllExams(): Flow<List<ExamEntity>>
```

## Key Design Patterns

### 1. Repository Pattern
Centralizes data access, hides implementation details:

```kotlin
class ExamRepository(private val dao: ExamDao) {
    fun getAllExams(): Flow<List<ExamEntity>> = dao.getAllExams()
    
    suspend fun insertExam(exam: ExamEntity): Long {
        return try {
            dao.insertExam(exam)
        } catch (e: Exception) {
            AnalyticsTracker.trackError(...)
            throw e
        }
    }
}
```

### 2. State Hoisting
UI state lives in ViewModels, not Composables:

```kotlin
// ViewModel holds state
class ExamViewModel : ViewModel() {
    private val _exams = MutableStateFlow<List<ExamEntity>>(emptyList())
    val exams = _exams.asStateFlow()
}

// Screen observes state
@Composable
fun ExamListScreen(viewModel: ExamViewModel) {
    val exams by viewModel.exams.collectAsState()
    ExamList(exams = exams)
}
```

### 3. Sealed Classes for States
Type-safe state representation:

```kotlin
sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Unauthenticated(val error: String? = null) : AuthState()
}
```

### 4. Flow-based Reactive Streams
Real-time updates from database:

```kotlin
// Database emits Flow
@Query("SELECT * FROM exams")
fun getAllExams(): Flow<List<ExamEntity>>

// ViewModel collects and transforms
val exams = repository.getAllExams()
    .map { list -> list.sortedByDescending { it.createdAt } }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// UI reacts automatically
LazyColumn {
    items(exams) { exam ->
        ExamCard(exam)
    }
}
```

## Security Architecture

### Encryption Layers
```
User Data
    ↓ (Room entities)
SQLCipher Encryption
    ↓ (Encrypted database file)
Android Keystore (TODO)
    ↓ (Protected keys)
Device Secure Storage
```

### Authentication Flow
```
User Login
    ↓
Firebase Auth
    ↓
Check Trial Status
    ↓ (device_trials in Firestore)
Grant/Deny Access
    ↓
Initialize Session
```

### Trial System
```
New User
    ↓
Create Auth Account
    ↓
Check Device ID in Firestore
    ↓
├─ Not Found → Grant 14-day trial
└─ Found → Trial expired or active
```

## Analytics Architecture

### Event Flow
```
User Action
    ↓
SessionTracker.trackAction()
    ↓
AnalyticsTracker.trackEvent()
    ↓
Firebase Firestore
    ↓
Developer Dashboard
```

### Data Collections
```
analytics_events     → Raw events (individual actions)
daily_metrics        → Aggregated daily stats
developer_alerts     → Errors and critical issues
usage_reports        → Periodic summaries
developer_reports    → Weekly/monthly reports
```

## Performance Considerations

### Database Queries
- ✅ Use Flow for reactive queries
- ✅ Use indexes on foreign keys
- ✅ Limit query results (pagination)
- ❌ Avoid blocking the main thread

### Image Processing
- ✅ Process images on background thread
- ✅ Scale images before processing
- ✅ Cache processed results
- ❌ Don't load full-size images in memory

### Compose Performance
- ✅ Use `remember` for expensive calculations
- ✅ Use `derivedStateOf` for computed values
- ✅ Use `key()` in LazyColumn items
- ❌ Don't create new objects in composition

## Testing Strategy

### Unit Tests
- Repositories (data access logic)
- ViewModels (business logic)
- Utilities (encryption, validation)

### Integration Tests
- Database operations (Room)
- Authentication flow (Firebase)
- Scan processing (ML Kit)

### UI Tests
- Critical user flows (create exam, scan, export)
- Navigation between screens
- Error states and edge cases

## Build Configuration

### Debug Build
- Debuggable: true
- Minification: false
- Logging: enabled
- Firebase: development project

### Release Build
- Debuggable: false
- Minification: true (R8/ProGuard)
- Logging: disabled (SecureLogger)
- Firebase: production project
- Signed: with release keystore

## Dependencies

### Core
- Kotlin 1.9.0
- Compose BOM 2024.06.00
- Material3

### Database
- Room 2.6.1
- SQLCipher 4.5.4

### Firebase
- Firebase BOM 32.7.0
- Auth, Firestore

### Camera/ML
- CameraX 1.3.1
- ML Kit Text Recognition 16.0.0

### Others
- Coil (image loading)
- ZXing (QR codes)
- Accompanist (permissions)

## Future Enhancements

### Planned
- [ ] Move encryption keys to Android Keystore
- [ ] Implement certificate pinning
- [ ] Add offline sync (if needed)
- [ ] Implement A/B testing
- [ ] Add user feedback system

### Considered
- [ ] Multi-language support
- [ ] Dark mode (already has light ice theme)
- [ ] Tablet optimization
- [ ] Cloud backup (currently local only)
- [ ] Advanced analytics dashboard

---

**Remember**: This is a living document. Update it as the architecture evolves!
