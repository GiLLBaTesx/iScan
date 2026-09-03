# Design Document: Offline Assessment Transformation

## Overview

This document provides the comprehensive technical design for transforming "Exam Scanner" into "Offline Assessment" - a complete teacher assessment platform for Filipino K-12 educators. The transformation expands from basic exam scanning to a full-featured assessment system with DepEd MELCs integration, subject organization, professional reporting, advanced analytics, and subscription monetization.

### Design Goals

1. **Expand Without Breaking**: Maintain all existing scanner functionality while adding new features
2. **Performance First**: Ensure smooth operation on low-end Android devices (2GB RAM, Android 8.0+)
3. **Offline-First**: All core operations work without internet connectivity
4. **Scalability**: Support growth from 3 subjects/5 exams (free) to unlimited (premium)
5. **Data Integrity**: Prevent data loss through robust database design and transactions
6. **User Experience**: Maintain premium glassmorphism UI while adding complex features

### Scope of Transformation

> **Baseline correction (important):** An earlier version of this design assumed the app was still shipping a pristine "v1" 4-entity Room database and that the transformation would build a single `v1 → v2` migration. That assumption is **out of date**. Substantial schema and repository work has already shipped. This section — and the Data Models, Correctness Properties, and Testing sections below — describe the **actual current state (v5)** and scope only the work that genuinely remains.

**Current State (already shipped — Room DB v5):**
- Encrypted Room database (SQLCipher `SupportFactory` + `EncryptionKeyManager.getDatabasePassphrase`), DB name `exam_scanner_database`, `version = 5`, `exportSchema = false`.
- Migrations `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5` are implemented and registered.
- **11 registered entities** (defined inline in `AppDatabase.kt`, not as separate files): `SubjectFolderEntity`, `SectionEntity`, `MelcEntity`, `TemplateEntity`, `GradingScaleEntity`, `ExamEntity`, `AnswerKeyEntity`, `QuestionMelcMappingEntity`, `StudentEntity`, `StudentAnswerEntity`, `StudentMelcMasteryEntity`.
- A single `ExamDao` and a single large `ExamRepository(dao: ExamDao)` already implement: subject-folder CRUD (soft delete), built-in grading scales + templates initialization, MELC read + question→MELC mapping (with validation), exam CRUD, answer-key save, student-results save + score calculation, student roster management, section management, post-scan section organization, per-section item analysis (`QuestionAnalysis`), recycle bin (`getDeletedExams`/`restoreExam`/`permanentlyDeleteExam`/`emptyRecycleBin`), and `clearAllData` with auto-backup.
- Existing scanner (CameraX + ML Kit), MVVM (`ExamViewModel`), premium glassmorphism UI, and most screens listed in the file-structure appendix.

**Net-new database work still remaining:**
- Four entity files have already been created as **separate files** but are **NOT yet registered** in `@Database`: `StudentEnrollmentEntity`, `MelcCoverageEntity`, `StudentNoteEntity`, `SyncLogEntity`.
- These are wired in via a **new `MIGRATION_5_6`** (bump `version` to 6), which creates the four tables and their indices/foreign keys. Existing tables are **not** renamed or recreated. Optionally set `exportSchema = true` to enable Room migration testing.

**Feature work still remaining (net-new domain services + screens):**
- DepEd MELCs dataset expansion (bundled asset covering every subject × grade 1-12 × quarter 1-4).
- QR generate/parse services, answer-sheet pretty printer + round-trip, bubble-detection parser.
- Analytics engine (difficulty/discrimination, learning gaps), mastery calculator, pacing engine.
- Professional PDF report system (individual/class/school), CSV/Excel import-export (XLSX).
- Freemium subscription/billing, notifications, internationalization. (No cloud sync — the app is offline-only; data portability is via on-device Backup/Restore.)

These new services **integrate with** the existing `ExamRepository`, DAO, entities, screens, and utilities described above — they do **not** replace them.


## Architecture

### High-Level Architecture

The system follows a layered MVVM (Model-View-ViewModel) architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  (Jetpack Compose UI + Glassmorphism Design System)         │
│  • Screens • Components • Theme • Navigation                │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                     ViewModel Layer                          │
│  (State Management + Business Logic Orchestration)          │
│  • ExamViewModel • SubjectViewModel • AnalyticsViewModel    │
│  • StudentViewModel • ReportViewModel                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                    Domain/Business Layer                     │
│  (Use Cases + Business Rules + Domain Models)               │
│  • AnalyticsEngine • QRCodeGenerator • PDFGenerator         │
│  • MasteryCalculator • PacingEngine • ImportExportService   │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                     Data Layer                               │
│  (Repository Pattern + Data Sources)                        │
│  • ExamRepository • SubjectRepository • MelcRepository      │
│  • StudentRepository                                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
┌───────▼─────┐ ┌──────▼──────┐ ┌────▼────────┐
│   Room DB   │ │ File System │ │  Firebase   │
│  (Primary)  │ │  (PDFs/Img) │ │  (Optional) │
└─────────────┘ └─────────────┘ └─────────────┘
```

### Architectural Layers Detailed

#### 1. Presentation Layer (UI)
- **Technology**: Jetpack Compose with Material3
- **Design System**: Premium glassmorphism components (FloatingGlassCard, GlassButton)
- **Navigation**: Jetpack Navigation Compose with bottom navigation + modal sheets
- **State**: Unidirectional data flow with StateFlow
- **Screens**: 20+ screens organized by feature module


#### 2. ViewModel Layer
- **Technology**: AndroidX ViewModel + Kotlin Coroutines + StateFlow
- **Responsibilities**:
  - Expose UI state via StateFlow
  - Handle user actions/events
  - Coordinate between repositories and use cases
  - Manage screen-level business logic
  - Handle navigation events
- **Current reality**: Only `ExamViewModel` exists today and already backs the shipped subject/exam/section/roster/scan flows. The additional ViewModels below are **net-new** and would be added incrementally for new feature areas.
- **Key ViewModels**:
  - `ExamViewModel` (EXISTING): exam management, grading, and the currently shipped screens
  - `SubjectViewModel` (NET-NEW): optional split of subject-folder concerns out of `ExamViewModel`
  - `StudentViewModel` (NET-NEW): student profiles and performance tracking
  - `AnalyticsViewModel` (NET-NEW): statistical calculations and visualizations
  - `ReportViewModel` (NET-NEW): PDF generation orchestration
  - `SubscriptionViewModel` (NET-NEW): billing and tier management

#### 3. Domain/Business Layer
- **Technology**: Pure Kotlin classes with minimal dependencies
- **Pattern**: Use case per major operation
- **Key Components**:
  - **AnalyticsEngine**: Calculates difficulty index, discrimination index, learning gaps
  - **QRCodeGenerator**: Generates QR codes with exam metadata
  - **QRCodeParser**: Decodes QR codes from scanned images
  - **PDFGenerator**: Creates formatted PDF reports
  - **MasteryCalculator**: Computes MELC mastery levels
  - **PacingEngine**: Tracks curriculum coverage and identifies behind-schedule MELCs
  - **ImportExportService**: Handles CSV/Excel file operations
  - **BubbleDetectionEngine**: Image processing for answer detection
  - **ValidationEngine**: Data integrity checks

#### 4. Data Layer (Repository Pattern)
- **Technology**: Kotlin Coroutines + Flow
- **Responsibilities**:
  - Abstract data sources
  - Provide clean API to ViewModels
  - Handle data transformation
  - Coordinate between local DB and cloud
- **Current reality**: There is a **single** `ExamRepository(dao: ExamDao)` backed by a single `ExamDao`. It already covers subjects, sections, exams, answer keys, students, roster, MELC mapping, post-scan organization, item analysis, and the recycle bin. There are **no** separate `SubjectRepository`/`SectionRepository`/`MelcRepository`/`StudentRepository`/`RecycleBinRepository` classes today, and the interfaces sketched below are **conceptual groupings** of responsibilities — not a mandate to split the shipped repository.
- **Existing responsibilities (in `ExamRepository`)**:
  - Subject folders (create/update/soft-delete), grading scales + templates init, MELC read + question→MELC mapping, exam CRUD, answer-key save, student results + scoring, section management + roster, post-scan section organization, per-section item analysis, recycle bin, and `clearAllData` with auto-backup.
- **Net-new data-layer work**:
  - Optional narrow helper repositories/DAO queries for new features (e.g., pagination query, enrollment/notes/coverage access) may be added **alongside** `ExamRepository` rather than replacing it. Whether to keep extending `ExamRepository` or introduce focused repositories is an implementation choice; the design does not require a rewrite.


#### 5. Data Sources
- **Primary: Room Database**
  - SQLite persistence
  - Type-safe queries
  - Flow-based reactive updates
  - Transaction support
  - Migration strategy
  - Optional SQLCipher encryption

- **Secondary: File System**
  - PDFs (generated reports, uploaded templates)
  - Images (scanned sheets, profile photos)
  - Exports (CSV/Excel files)
  - Location: Internal storage + Downloads folder

- **Tertiary: Firebase Authentication**
  - Firebase Authentication for the auth gate and subscription/tier identity
  - No Cloud Firestore data sync and no remote Cloud Storage backup — the app is offline-only
    and data portability is via on-device Backup/Restore (see Data Portability section)

### Navigation Architecture

The app uses a three-level navigation hierarchy:

```
Level 1: Subject Folders List (Entry Point)
    │
    ├─> Level 2: Subject Dashboard (per subject)
    │       │
    │       ├─> Exams Tab
    │       │   └─> Exam Detail Screen (tabs: Results | Analytics | Reports)
    │       │       ├─> Answer Key Editor (create/edit exam; question→MELC mapping lives here)
    │       │       ├─> Scanner Screen
    │       │       ├─> Results tab: student scores + Export/Clear results
    │       │       ├─> Analytics tab: item analysis & insights
    │       │       └─> Reports tab: Individual / Class / School-level
    │       │
    │       ├─> Sections Tab
    │       │   └─> Section Detail Screen
    │       │       └─> Student Roster Management
    │       │
    │       ├─> Students Tab
    │       │   └─> Student Profile Screen
    │       │       └─> Performance History
    │       │
    │       └─> Analytics Tab
    │           └─> Learning Gaps Detail
    │
    ├─> Dashboard (Global)
    ├─> MELCs Browser
    ├─> Recycle Bin
    └─> Settings
        ├─> Subscription Management
        ├─> Backup Management (Backup/Restore)
        ├─> Language Settings
        └─> About/Privacy Policy
```

> Privacy Policy is reached via **Settings → About iScan → Privacy Policy** (the About screen's Legal section links Privacy Policy, Terms of Service, and Licenses). There is no longer a separate top-level "Privacy & Security" section in Settings.

### Exam Detail screen tabs

The Exam Detail screen is organized around a segmented toggle with **three tabs — Results | Analytics | Reports** (Results is the default):

- **Empty state:** The tab bar appears **only after the first answer sheet has been scanned** (`students.isNotEmpty()`). Before any scans, the screen shows the "Ready to grade!" empty state with **Start Scanning** and **Generate Answer Sheet** buttons.
- **Results tab:** Class stats card (student count + class average) and the ranked student score list, plus actions **Scan More**, **Export results**, and **Clear all results**.
- **Analytics tab:** The item-analysis experience — item response curve, answer-distribution bars, mastery pie + score line — rendered inline via `ItemAnalysisPanel` (embedded), with a SmartDashboard fallback. This replaces the old "Item analysis" / "See Insights" entry points.
- **Reports tab:** Individual student report (with student picker), Class summary report, and School-level report (tier-gated).

The overflow (⋮) menu is limited to exactly three actions: **Edit answer key**, **Rename exam**, and **Generate answer sheet**.

> **Removed from this screen's menu:** the "Map competencies (MELC)" action and the "Delete exam" action. These features still exist in the app — **MELC question→competency mapping remains in the Answer Key Editor / create-edit-exam flow** (consistent with Requirement 2.2), and **Delete-exam remains available in the folder/exam list screen**. Only their entry points on the Exam Detail screen were removed.

### Startup & platform notes

- **`MainActivity` extends `AppCompatActivity`** (still a `FragmentActivity` subclass, so `BiometricPrompt` works) and the XML app theme parent is `Theme.AppCompat.DayNight.NoActionBar`. Both are required so AppCompat per-app locales back-port correctly to API 26–32.
- **`AppLockGate`** persists its unlocked state via `rememberSaveable`, so a configuration change or locale switch does not spuriously re-prompt biometrics; genuine backgrounding still re-locks.
- The exam-detail **`ReportViewModel` is constructed with the app-scoped `SubscriptionManager`**, so school-level report tier-gating works for Premium users.


## Components and Interfaces

### 1. Subject Management System

#### SubjectFolderEntity (EXISTING — shipped, registered in v5)
The actual shipped entity is deliberately minimal. Subject-specific settings (default grading scale, default question count, color theme) are stored inside the `settingsJson` blob rather than as dedicated columns — so Requirement 1.3 is satisfied without a schema change.
```kotlin
@Entity(tableName = "subject_folders")
data class SubjectFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val settingsJson: String = "{}",          // JSON: color theme, default grading scale, default question count, etc.
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,           // Soft delete flag
    val deletedAt: Long? = null               // Deletion timestamp for recycle bin
)
```
> There are **no** `colorTheme`, `defaultGradingScale`, `defaultQuestionCount`, or `sortOrder` columns — do not add them. Any UI theming/defaults/ordering must be encoded in `settingsJson` (or handled client-side).

#### Subject-folder access (EXISTING — in `ExamRepository`)
Subject folders are managed by the shipped `ExamRepository`, **not** a separate `SubjectRepository`. The methods that already exist are:
```kotlin
// ExamRepository (existing)
fun getAllSubjectFolders(): Flow<List<SubjectFolderEntity>>       // isDeleted = 0
suspend fun createSubjectFolder(name: String): Long
suspend fun updateSubjectFolder(folderId: Long, newName: String)
suspend fun deleteSubjectFolder(folderId: Long)                   // soft delete
```
**Net-new (still to add):** subject restore/permanent-delete helpers (recycle-bin symmetry for folders) and an optional `SubjectStats` aggregation. These can be added to `ExamRepository` or a thin helper; a full `SubjectRepository` split is optional, not required.
```kotlin
data class SubjectStats(
    val totalExams: Int,
    val totalStudents: Int,
    val totalAssessments: Int,
    val averageScore: Float,
    val melcCoveragePercentage: Float
)
```

### 2. DepEd MELCs Database System

#### MelcEntity (EXISTING — shipped, registered in v5)
The shipped entity stores `gradeLevel` as a **`String`** (not `Int`) and has **no** `category`, `isActive`, or indices. It is intentionally lean.
```kotlin
@Entity(tableName = "melcs")
data class MelcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,                          // "M7NS-Ia-1"
    val description: String,                   // Full competency description
    val gradeLevel: String,                    // NOTE: String in the shipped schema (e.g. "7")
    val subject: String,                       // "Mathematics", "Science", etc.
    val quarter: Int                           // 1-4
)
```
> **Do not** change `gradeLevel` to `Int` or add `category`/`isActive` unless a deliberate `MIGRATION_6_7` is scoped — that is out of scope here. Any code that filters by grade must use the `String` representation (see the existing `dao.getMelcs(subject, gradeLevel: String)` query). Adding `code`/`(subject, gradeLevel, quarter)` indices is an optional, safe performance enhancement that would require its own migration; it is not part of `MIGRATION_5_6`.

#### MELC access + mapping (EXISTING — in `ExamRepository`)
There is **no** separate `MelcRepository`. MELC reads and question→MELC mapping already live in `ExamRepository`:
```kotlin
// ExamRepository (existing)
fun getMelcs(subject: String, gradeLevel: String): Flow<List<MelcEntity>>
fun getMelcsBySubject(subject: String, gradeLevel: String): Flow<List<MelcEntity>>
fun getAllMelcs(): Flow<List<MelcEntity>>
suspend fun initializeSampleMelcs()                                   // seeds from SampleMelcsData
suspend fun saveQuestionMelcMappings(examId: Long, mappings: Map<Int, Long>)   // validates + replaces
suspend fun getQuestionMelcMappings(examId: Long): Map<Int, MelcEntity>
```
**Net-new (still to add):** mastery aggregation/read helpers (`updateStudentMastery`, `getStudentMastery`, class mastery stats) — currently only the storage entity `StudentMelcMasteryEntity` and a single insert/read DAO query exist. These belong in the net-new `MasteryCalculator` (see §8), reading through the existing DAO.


#### QuestionMelcMappingEntity (EXISTING — shipped, registered in v5)
Matches the shipped schema (FKs to `exams` and `melcs`, cascade delete). The shipped inline definition does not declare an explicit `(examId, questionNumber)` index; adding one is an optional performance enhancement requiring its own migration.
```kotlin
@Entity(
    tableName = "question_melc_mappings",
    foreignKeys = [
        ForeignKey(entity = ExamEntity::class, parentColumns = ["id"], childColumns = ["examId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MelcEntity::class, parentColumns = ["id"], childColumns = ["melcId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class QuestionMelcMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val questionNumber: Int,
    val melcId: Long
)
```

#### StudentMelcMasteryEntity (EXISTING — shipped, registered in v5)
The shipped entity is a **lean summary row** — it stores only the classification and percentage, **not** cumulative points, assessment count, or a unique `(studentId, melcId)` index.
```kotlin
@Entity(
    tableName = "student_melc_mastery",
    foreignKeys = [
        ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MelcEntity::class, parentColumns = ["id"], childColumns = ["melcId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class StudentMelcMasteryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val melcId: Long,
    val masteryLevel: String,               // Developing, Approaching, Proficient, Advanced
    val percentage: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)
```
> The mastery aggregation math (cumulative earned/possible points, assessment count) is computed **in the net-new `MasteryCalculator`** and reduced to `(percentage, masteryLevel)` before being persisted through the existing `dao.insertStudentMelcMastery(...)` (`OnConflictStrategy.REPLACE`). If per-MELC cumulative counters need to be persisted, that is a **net-new schema change** (a future migration adding `totalPoints`/`maxPoints`/`assessmentCount` + a unique index) and is not part of `MIGRATION_5_6`.

#### MELC mastery access (net-new helpers over the EXISTING DAO)
No `MelcRepository` interface exists or is required. The net-new mastery/reporting helpers below are implemented in `MasteryCalculator` / `AnalyticsEngine` (see §7-§8), reading and writing through the **existing** `ExamDao`:
```kotlin
// Conceptual helpers (net-new logic, existing DAO)
suspend fun getMelcsForExam(examId: Long): List<MelcWithQuestions>     // from getQuestionMelcMappings + melcs
suspend fun updateStudentMastery(studentId: Long)                      // MasteryCalculator, persists StudentMelcMasteryEntity
suspend fun getStudentMastery(studentId: Long): List<StudentMelcMasteryEntity>  // dao.getStudentMelcMastery
suspend fun getClassMastery(sectionId: Long, melcId: Long): ClassMasteryStats

data class MelcWithQuestions(val melc: MelcEntity, val questions: List<Int>)
data class QuestionReference(val examId: Long, val examName: String, val questionNumber: Int)
data class ClassMasteryStats(
    val melcId: Long, val totalStudents: Int,
    val developing: Int, val approaching: Int, val proficient: Int, val advanced: Int,
    val averagePercentage: Float
)
```


### 3. Section and Class Management

#### SectionEntity (EXISTING — shipped, registered in v5)
The shipped entity has **no** `gradeLevel` or `schoolYear` columns and no explicit index declaration.
```kotlin
@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = SubjectFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectFolderId: Long,
    val name: String,                          // "Grade 7-A", "Section 1", etc.
    val capacity: Int = 50,                    // Maximum students
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
```
> Grade-level filtering for a section derives from its students (`StudentEntity.gradeLevel`, a `String`) rather than a section column. School year, if needed, should be encoded in the folder `settingsJson` or added via a future migration — it is out of scope here.

#### StudentEnrollmentEntity (NET-NEW — file exists, NOT yet registered)
This entity file already exists in `data/StudentEnrollmentEntity.kt` but is **not** in `@Database`. It is registered by `MIGRATION_5_6`. The shape matches the shipped file exactly:
```kotlin
@Entity(
    tableName = "student_enrollments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId", "sectionId"], unique = true)]
)
data class StudentEnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val sectionId: Long,
    val enrolledAt: Long = System.currentTimeMillis(),
    val status: String = "Active"              // Active, Dropped, Transferred
)
```

#### StudentEntity (EXISTING — shipped, registered in v5)
The shipped entity already carries `sectionId`, `examId`, `gradeLevel` (a **`String`**), `contactInfo`, and `photoPath`. It has a foreign key to `sections`. It does **not** have `firstName`/`lastName`/`middleName`, `notes`, `createdAt`, `profilePhotoPath`, `isDeleted`, or `deletedAt`.
```kotlin
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,                     // School ID
    val name: String,                          // Full name (single field)
    val sectionId: Long = 0,
    val examId: Long = 0,                      // Backward compatibility with per-exam scans
    val gradeLevel: String = "",               // NOTE: String, not Int
    val contactInfo: String = "",              // free text / JSON
    val photoPath: String = "",                // note: `photoPath`, not `profilePhotoPath`
    val scannedAt: Long = System.currentTimeMillis()
)
```
> Requirement 8 fields not present as columns are handled elsewhere: **profile notes** live in the net-new `StudentNoteEntity` (multiple timestamped notes, satisfies Req 8.5) rather than a single `notes` column; a split first/last/middle name is **not** in the schema (the single `name` field is used). Student soft-delete (`isDeleted`/`deletedAt`) is **not** present on `StudentEntity` — the recycle bin currently operates at the exam/folder/section level. Adding student-level soft-delete or name splitting would be a **future migration**, not `MIGRATION_5_6`.

#### Section + enrollment access (EXISTING repo + NET-NEW enrollment)
Section CRUD and roster management already exist in `ExamRepository`; there is **no** `SectionRepository`:
```kotlin
// ExamRepository (existing)
fun getSections(folderId: Long): Flow<List<SectionEntity>>
suspend fun createSection(folderId: Long, name: String, capacity: Int): Long
suspend fun updateSection(section: SectionEntity, name: String, capacity: Int)
suspend fun deleteSection(section: SectionEntity)                 // soft delete
suspend fun getStudentsBySection(sectionId: Long): List<StudentEntity>
suspend fun addStudentToSection(studentId: String, name: String, /* ... */): Long
suspend fun bulkInsertStudents(students: List<StudentEntity>)     // roster import target
suspend fun assignStudentsToSection(studentIds: List<Long>, sectionId: Long)
suspend fun autoOrganizeSections(examId: Long, folderId: Long)    // post-scan organization
```
**Net-new (still to add):** explicit `StudentEnrollmentEntity` reads/writes (`enrollStudent`/`unenrollStudent`) once the table is registered by `MIGRATION_5_6`, a `SectionStats` aggregation, and CSV roster export. Today, student→section association is stored directly on `StudentEntity.sectionId`; the enrollment join table is the net-new mechanism for many-section membership.
```kotlin
data class SectionStats(
    val enrolledCount: Int,
    val averagePerformance: Float,
    val assessmentCount: Int,
    val lastAssessmentDate: Long?
)
```


### 4. QR Code System

#### QRCodeGenerator Service
```kotlin
class QRCodeGenerator {
    data class ExamMetadata(
        val examId: Long,
        val examName: String,
        val totalQuestions: Int,
        val subjectId: Long,
        val createdAt: Long,
        val version: Int = 1  // For future compatibility
    )
    
    /**
     * Generates QR code bitmap containing exam metadata
     * Uses JSON + Base64 encoding for reliability
     * Size: 200x200 pixels (suitable for A4 printing)
     */
    fun generateQRCode(metadata: ExamMetadata, size: Int = 200): Bitmap {
        // 1. Serialize to JSON
        val json = Json.encodeToString(metadata)
        
        // 2. Encode using ZXing
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = MultiFormatWriter().encode(
            json,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        
        // 3. Convert to Bitmap
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }
    
    /**
     * Generates QR code as base64 string for PDF embedding
     */
    fun generateQRCodeBase64(metadata: ExamMetadata): String {
        val bitmap = generateQRCode(metadata)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
    }
}
```

#### QRCodeParser Service
```kotlin
class QRCodeParser {
    /**
     * Scans image for QR code and extracts exam metadata
     * Returns null if no QR code detected
     * Uses ML Kit Barcode Scanning
     */
    suspend fun parseQRCode(bitmap: Bitmap): QRCodeGenerator.ExamMetadata? = 
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()
            
            try {
                val barcodes = suspendCoroutine<List<Barcode>> { continuation ->
                    scanner.process(image)
                        .addOnSuccessListener { barcodes -> continuation.resume(barcodes) }
                        .addOnFailureListener { e -> continuation.resumeWithException(e) }
                }
                
                // Find QR code
                val qrCode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                val rawValue = qrCode?.rawValue ?: return@withContext null
                
                // Parse JSON
                return@withContext Json.decodeFromString<QRCodeGenerator.ExamMetadata>(rawValue)
            } catch (e: Exception) {
                Log.e("QRCodeParser", "Failed to parse QR code", e)
                return@withContext null
            }
        }
    
    /**
     * Detects if image contains a QR code (fast check)
     */
    suspend fun hasQRCode(bitmap: Bitmap): Boolean {
        return parseQRCode(bitmap) != null
    }
}
```


### 5. Answer Sheet Generation System

#### TemplateEntity (EXISTING — shipped, registered in v5)
The shipped entity is much richer than the earlier `{id, name, filePath, fileType, createdAt}` sketch. It already models question count, choice count, template type, per-section layout JSON, built-in flag, header/logo/QR options, and file storage.
```kotlin
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalQuestions: Int,
    val numberOfChoices: Int = 4,              // 2-6 (A-F)
    val templateType: String = "STANDARD",     // STANDARD, MULTI_SECTION, TRUE_FALSE
    val sectionsJson: String = "[]",           // JSON array of layout sections
    val isBuiltIn: Boolean = false,
    val headerText: String = "",
    val includeSchoolLogo: Boolean = false,
    val qrCodePosition: String = "TOP_RIGHT",  // fixed QR placement (Req 4.6)
    val filePath: String = "",                 // for uploaded templates
    val fileType: String = "PDF",
    val createdAt: Long = System.currentTimeMillis()
)
```
Template CRUD and built-in seeding already exist in `ExamRepository` (`getAllTemplates`, `getBuiltInTemplates`, `createTemplate`, `updateTemplate`, `deleteTemplate`, `initializeBuiltInTemplates`). The `AnswerSheetPrettyPrinter` (§5) consumes this entity; the pretty-printer's `SheetConfig` is a **render-time DTO**, not a schema change.

#### GradingScaleEntity (EXISTING — shipped, registered in v5; not previously documented)
An entity the earlier design never mentioned. It backs DepEd/traditional/custom grading presets and is seeded via `ExamRepository.initializeBuiltInGradingScales()`.
```kotlin
@Entity(tableName = "grading_scales")
data class GradingScaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                          // "DepEd K-12", "Traditional", "IB", "Custom"
    val scaleType: String,                     // "DEPED_K12", "TRADITIONAL", "CUSTOM"
    val minGrade: Int = 60,
    val maxGrade: Int = 100,
    val passingGrade: Int = 75,
    val transmutationJson: String = "[]",      // JSON array of grade brackets
    val isBuiltIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### ExamEntity + AnswerKeyEntity (EXISTING — shipped, registered in v5)
`ExamEntity` already includes subject-folder/section/template foreign keys, grading customization, exam settings, and soft-delete — it is **not** the original 4-field v1 exam. No further migration is needed for these columns.
```kotlin
@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(entity = SubjectFolderEntity::class, parentColumns = ["id"], childColumns = ["subjectFolderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SectionEntity::class, parentColumns = ["id"], childColumns = ["sectionId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectFolderId: Long = 0,
    val sectionId: Long? = null,
    val templateId: Long? = null,
    val name: String,
    val totalQuestions: Int,
    val qrCode: String = "",
    val gradingScale: String = "DEPED_K12",
    val passingGrade: Int = 75,
    val useNegativeMarking: Boolean = false,
    val negativeMarkValue: Float = 0f,
    val examDate: Long? = null,
    val timeLimit: Int? = null,
    val allowLateScans: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "answer_keys")
data class AnswerKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val questionNumber: Int,
    val correctAnswer: String,
    val alternativeAnswers: String = "",       // comma-separated
    val points: Int = 1
)
```

#### AnswerSheetPrettyPrinter Service
```kotlin
class AnswerSheetPrettyPrinter(
    private val qrGenerator: QRCodeGenerator,
    private val context: Context
) {
    data class SheetConfig(
        val examName: String,
        val totalQuestions: Int,
        val optionsCount: Int = 4,             // A-D by default
        val optionsLabels: List<String> = listOf("A", "B", "C", "D"),
        val includeNameField: Boolean = true,
        val includeIdField: Boolean = true,
        val includeDateField: Boolean = true,
        val includeClassField: Boolean = true,
        val bubbleSize: Float = 8f,            // mm
        val bubbleSpacing: Float = 5f,         // mm
        val columns: Int = 2                   // Questions per row
    )
    
    /**
     * Generates printable answer sheet PDF with QR code
     * Uses iText7 for PDF generation
     * Returns file path to generated PDF
     */
    suspend fun generateAnswerSheet(
        exam: ExamEntity,
        metadata: QRCodeGenerator.ExamMetadata,
        config: SheetConfig,
        template: TemplateEntity? = null
    ): String = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "answer_sheets")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "sheet_${exam.id}_${System.currentTimeMillis()}.pdf")
        
        PdfWriter(outputFile).use { writer ->
            PdfDocument(PdfWriter(writer)).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    // 1. Add QR Code at top-right
                    val qrBitmap = qrGenerator.generateQRCode(metadata, 150)
                    val qrImage = convertBitmapToImage(qrBitmap)
                    qrImage.setFixedPosition(450f, 750f)
                    document.add(qrImage)
                    
                    // 2. Add Header Section
                    addHeader(document, exam.name)
                    
                    // 3. Add Student Information Fields
                    if (config.includeNameField) addField(document, "NAME:", 400f)
                    if (config.includeIdField) addField(document, "STUDENT ID:", 200f)
                    if (config.includeClassField) addField(document, "SECTION:", 200f)
                    if (config.includeDateField) addField(document, "DATE:", 150f)
                    
                    // 4. Add Instructions
                    addInstructions(document)
                    
                    // 5. Add Bubble Grid
                    addBubbleGrid(document, config)
                    
                    // 6. Add Footer
                    addFooter(document, exam.id)
                }
            }
        }
        
        return@withContext outputFile.absolutePath
    }
    
    private fun addBubbleGrid(document: Document, config: SheetConfig) {
        val table = Table(UnitValue.createPercentArray(
            floatArrayOf(10f) + FloatArray(config.optionsCount) { 15f }
        ))
        table.setWidth(UnitValue.createPercentValue(100f))
        
        // Header row
        table.addCell(Cell().add(Paragraph("#").setBold()))
        config.optionsLabels.forEach { label ->
            table.addCell(Cell().add(Paragraph(label).setBold().setTextAlignment(TextAlignment.CENTER)))
        }
        
        // Question rows
        for (q in 1..config.totalQuestions) {
            table.addCell(Cell().add(Paragraph(q.toString())))
            repeat(config.optionsCount) {
                table.addCell(createBubbleCell(config.bubbleSize))
            }
        }
        
        document.add(table)
    }
    
    private fun createBubbleCell(size: Float): Cell {
        // Creates cell with circular border for bubble
        val cell = Cell()
        cell.setHeight(UnitValue.createPointValue(size * 2.83f))  // mm to pt
        cell.setWidth(UnitValue.createPointValue(size * 2.83f))
        cell.setBorder(Border.NO_BORDER)
        cell.setNextRenderer(object : CellRenderer(cell) {
            override fun drawBorder(drawContext: DrawContext) {
                val canvas = drawContext.canvas
                val area = occupiedAreaBBox
                canvas.circle(
                    area.x + area.width / 2,
                    area.y + area.height / 2,
                    size * 1.415f  // mm to pt radius
                )
                canvas.setStrokeColor(ColorConstants.BLACK)
                canvas.setLineWidth(1f)
                canvas.stroke()
            }
        })
        return cell
    }
}
```


### 6. PDF Report Generation System

> **UI entry point:** Reports (Individual / Class / School-level) are generated from the **Reports tab** of the Exam Detail screen — not from an overflow menu. The related item-analysis view is shown inline on the **Analytics tab** rather than as a full-screen overlay. See "Exam Detail screen tabs" above.

#### ReportGenerator Service
```kotlin
class ReportGenerator(
    private val context: Context,
    private val repository: ExamRepository,
    private val melcRepository: MelcRepository
) {
    sealed class ReportType {
        data class IndividualStudent(val studentId: Long, val examId: Long) : ReportType()
        data class ClassSummary(val examId: Long, val sectionId: Long) : ReportType()
        data class SchoolLevel(val subjectId: Long, val quarter: Int, val schoolYear: String) : ReportType()
    }
    
    /**
     * Generates professional PDF report
     * Returns file path to Downloads folder
     */
    suspend fun generateReport(
        type: ReportType,
        schoolName: String = "",
        logoPath: String? = null
    ): String = withContext(Dispatchers.IO) {
        when (type) {
            is ReportType.IndividualStudent -> generateIndividualReport(type, schoolName, logoPath)
            is ReportType.ClassSummary -> generateClassReport(type, schoolName, logoPath)
            is ReportType.SchoolLevel -> generateSchoolReport(type, schoolName, logoPath)
        }
    }
    
    private suspend fun generateIndividualReport(
        type: ReportType.IndividualStudent,
        schoolName: String,
        logoPath: String?
    ): String {
        val student = repository.getStudent(type.studentId) ?: throw IllegalArgumentException("Student not found")
        val exam = repository.examDao.getExam(type.examId) ?: throw IllegalArgumentException("Exam not found")
        val answers = repository.getStudentAnswers(type.studentId)
        val keys = repository.getAnswerKeys(type.examId).first()
        val score = repository.calculateScore(student, keys)
        val totalPoints = keys.sumOf { it.points }
        val percentage = (score.toFloat() / totalPoints * 100).roundToInt()
        
        // Get MELC mastery
        val masteryData = melcRepository.getStudentMastery(type.studentId)
        
        val outputFile = createOutputFile("individual_report_${student.studentId}_${exam.name}.pdf")
        
        PdfWriter(outputFile).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    // Header with logo
                    if (logoPath != null) {
                        addLogo(document, logoPath)
                    }
                    addReportHeader(document, schoolName, "Individual Student Report")
                    
                    // Student Info Section
                    addSection(document, "Student Information")
                    addKeyValue(document, "Name", student.name)
                    addKeyValue(document, "Student ID", student.studentId)
                    addKeyValue(document, "Exam", exam.name)
                    addKeyValue(document, "Date Taken", formatDate(student.scannedAt))
                    
                    // Score Section
                    addSection(document, "Score Summary")
                    addScoreBox(document, score, totalPoints, percentage)
                    
                    // Question-by-Question Breakdown
                    addSection(document, "Detailed Breakdown")
                    addQuestionTable(document, answers, keys)
                    
                    // MELC Competency Mastery
                    addSection(document, "Competency Mastery Levels")
                    addMasteryTable(document, masteryData)
                    
                    // Performance Chart
                    addSection(document, "Visual Analysis")
                    addPerformanceChart(document, answers, keys)
                    
                    // Footer
                    addReportFooter(document)
                }
            }
        }
        
        return outputFile.absolutePath
    }
    
    private suspend fun generateClassReport(
        type: ReportType.ClassSummary,
        schoolName: String,
        logoPath: String?
    ): String {
        val exam = repository.examDao.getExam(type.examId) ?: throw IllegalArgumentException("Exam not found")
        val students = repository.getStudents(type.examId).first()
        val keys = repository.getAnswerKeys(type.examId).first()
        
        // Calculate statistics
        val scores = students.map { student ->
            repository.calculateScore(student, keys)
        }
        val totalPoints = keys.sumOf { it.points }
        val percentages = scores.map { (it.toFloat() / totalPoints * 100) }
        
        val stats = ClassStatistics(
            mean = percentages.average().toFloat(),
            median = percentages.sorted()[percentages.size / 2],
            highest = percentages.maxOrNull() ?: 0f,
            lowest = percentages.minOrNull() ?: 0f,
            standardDeviation = calculateStdDev(percentages)
        )
        
        // Item analysis
        val itemAnalysis = calculateItemAnalysis(students, keys)
        
        val outputFile = createOutputFile("class_report_${exam.name}.pdf")
        
        PdfWriter(outputFile).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    // Header
                    if (logoPath != null) addLogo(document, logoPath)
                    addReportHeader(document, schoolName, "Class Summary Report")
                    
                    // Exam Info
                    addSection(document, "Exam Information")
                    addKeyValue(document, "Exam", exam.name)
                    addKeyValue(document, "Total Students", students.size.toString())
                    addKeyValue(document, "Date", formatDate(exam.createdAt))
                    
                    // Class Statistics
                    addSection(document, "Class Statistics")
                    addStatisticsTable(document, stats)
                    
                    // Score Distribution Chart
                    addSection(document, "Score Distribution")
                    addScoreDistributionChart(document, percentages)
                    
                    // Item Analysis
                    addSection(document, "Item Analysis")
                    addItemAnalysisTable(document, itemAnalysis)
                    
                    // MELC Mastery Distribution
                    addSection(document, "Competency Mastery Distribution")
                    addMelcMasteryChart(document, type.examId)
                    
                    // Student Rankings
                    addSection(document, "Student Rankings")
                    addRankingsTable(document, students, scores, totalPoints)
                    
                    addReportFooter(document)
                }
            }
        }
        
        return outputFile.absolutePath
    }
    
    private suspend fun generateSchoolReport(
        type: ReportType.SchoolLevel,
        schoolName: String,
        logoPath: String?
    ): String {
        // Aggregate data across all sections in subject
        val sections = repository.getSectionsBySubject(type.subjectId)
        val aggregatedStats = calculateAggregatedStats(sections, type.quarter)
        
        val outputFile = createOutputFile("school_report_${type.schoolYear}_Q${type.quarter}.pdf")
        
        PdfWriter(outputFile).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    // Header
                    if (logoPath != null) addLogo(document, logoPath)
                    addReportHeader(document, schoolName, "School-Level Analytics Report")
                    
                    // Overview
                    addSection(document, "Overview")
                    addKeyValue(document, "School Year", type.schoolYear)
                    addKeyValue(document, "Quarter", type.quarter.toString())
                    addKeyValue(document, "Total Sections", sections.size.toString())
                    addKeyValue(document, "Total Students", aggregatedStats.totalStudents.toString())
                    
                    // Performance Trends
                    addSection(document, "Performance Trends")
                    addTrendsChart(document, aggregatedStats.trends)
                    
                    // Competency Gaps
                    addSection(document, "Learning Gaps Identified")
                    addLearningGapsTable(document, aggregatedStats.learningGaps)
                    
                    // Comparative Statistics
                    addSection(document, "Section Comparisons")
                    addSectionComparisonChart(document, sections)
                    
                    // Recommendations
                    addSection(document, "Recommendations")
                    addRecommendations(document, aggregatedStats)
                    
                    addReportFooter(document)
                }
            }
        }
        
        return outputFile.absolutePath
    }
    
    data class ClassStatistics(
        val mean: Float,
        val median: Float,
        val highest: Float,
        val lowest: Float,
        val standardDeviation: Float
    )
}
```


### 7. Analytics Engine

#### AnalyticsEngine Service
```kotlin
class AnalyticsEngine(
    private val repository: ExamRepository,
    private val melcRepository: MelcRepository
) {
    /**
     * Calculates difficulty index for a question
     * Formula: (Number of students who answered correctly / Total students) × 100
     * Returns: 0.0 to 100.0
     */
    suspend fun calculateDifficulty(examId: Long, questionNumber: Int): Float {
        val students = repository.getStudents(examId).first()
        val keys = repository.getAnswerKeys(examId).first()
        val correctAnswer = keys.find { it.questionNumber == questionNumber }?.correctAnswer ?: return 0f
        
        val correctCount = students.count { student ->
            val answers = repository.getStudentAnswers(student.id)
            answers.find { it.questionNumber == questionNumber }?.answer == correctAnswer
        }
        
        return if (students.isEmpty()) 0f else (correctCount.toFloat() / students.size * 100)
    }
    
    /**
     * Calculates discrimination index using upper/lower 27% groups
     * Formula: (U - L) / N
     * where U = correct in upper 27%, L = correct in lower 27%, N = size of each group
     * Returns: -1.0 to +1.0
     * 
     * Interpretation:
     * < 0.20: Poor item (consider revision/removal)
     * 0.20-0.29: Fair item (marginal, consider improvement)
     * 0.30-0.39: Good item (acceptable)
     * ≥ 0.40: Excellent item (very good discrimination)
     */
    suspend fun calculateDiscriminationIndex(examId: Long, questionNumber: Int): Float {
        val students = repository.getStudents(examId).first()
        val keys = repository.getAnswerKeys(examId).first()
        
        // Sort students by total score
        val studentsWithScores = students.map { student ->
            val score = repository.calculateScore(student, keys)
            student to score
        }.sortedByDescending { it.second }
        
        if (studentsWithScores.size < 10) return 0f  // Need minimum sample size
        
        // Get upper and lower 27% groups
        val groupSize = (studentsWithScores.size * 0.27).toInt()
        val upperGroup = studentsWithScores.take(groupSize)
        val lowerGroup = studentsWithScores.takeLast(groupSize)
        
        val correctAnswer = keys.find { it.questionNumber == questionNumber }?.correctAnswer ?: return 0f
        
        // Count correct in each group
        val upperCorrect = upperGroup.count { (student, _) ->
            val answers = repository.getStudentAnswers(student.id)
            answers.find { it.questionNumber == questionNumber }?.answer == correctAnswer
        }
        
        val lowerCorrect = lowerGroup.count { (student, _) ->
            val answers = repository.getStudentAnswers(student.id)
            answers.find { it.questionNumber == questionNumber }?.answer == correctAnswer
        }
        
        return (upperCorrect - lowerCorrect).toFloat() / groupSize
    }
    
    /**
     * Classifies discrimination index quality
     */
    fun classifyDiscrimination(index: Float): DiscriminationQuality {
        return when {
            index < 0.20f -> DiscriminationQuality.POOR
            index < 0.30f -> DiscriminationQuality.FAIR
            index < 0.40f -> DiscriminationQuality.GOOD
            else -> DiscriminationQuality.EXCELLENT
        }
    }
    
    enum class DiscriminationQuality {
        POOR, FAIR, GOOD, EXCELLENT
    }
    
    /**
     * Identifies learning gaps (MELCs with <75% mastery across section)
     */
    suspend fun identifyLearningGaps(sectionId: Long): List<LearningGap> {
        val students = repository.getStudentsInSection(sectionId)
        val melcs = melcRepository.getAllMelcs().first()
        
        return melcs.mapNotNull { melc ->
            val masteryData = students.map { student ->
                melcRepository.getStudentMastery(student.id)
                    .find { it.melcId == melc.id }
                    ?.percentage ?: 0f
            }
            
            val averageMastery = masteryData.average().toFloat()
            
            if (averageMastery < 75f) {
                LearningGap(
                    melc = melc,
                    averageMastery = averageMastery,
                    studentsBelow75 = masteryData.count { it < 75f },
                    totalStudents = students.size,
                    severity = when {
                        averageMastery < 50f -> GapSeverity.CRITICAL
                        averageMastery < 65f -> GapSeverity.HIGH
                        else -> GapSeverity.MODERATE
                    }
                )
            } else null
        }.sortedBy { it.averageMastery }
    }
    
    data class LearningGap(
        val melc: MelcEntity,
        val averageMastery: Float,
        val studentsBelow75: Int,
        val totalStudents: Int,
        val severity: GapSeverity
    )
    
    enum class GapSeverity {
        MODERATE, HIGH, CRITICAL
    }
    
    /**
     * Calculates item response curve data for visualization
     */
    suspend fun getItemResponseCurve(examId: Long, questionNumber: Int): ItemResponseCurve {
        val students = repository.getStudents(examId).first()
        val keys = repository.getAnswerKeys(examId).first()
        val key = keys.find { it.questionNumber == questionNumber } ?: 
            return ItemResponseCurve(emptyMap(), 0)
        
        val responses = mutableMapOf<String, Int>()
        val options = key.correctAnswer.split(",") + key.alternativeAnswers.split(",").filter { it.isNotEmpty() }
        
        options.forEach { responses[it] = 0 }
        
        students.forEach { student ->
            val answers = repository.getStudentAnswers(student.id)
            val answer = answers.find { it.questionNumber == questionNumber }?.answer ?: "No Answer"
            responses[answer] = (responses[answer] ?: 0) + 1
        }
        
        return ItemResponseCurve(
            responses = responses,
            totalResponses = students.size
        )
    }
    
    data class ItemResponseCurve(
        val responses: Map<String, Int>,  // Answer -> Count
        val totalResponses: Int
    )
}
```


### 8. Mastery Calculation System

#### MasteryCalculator Service
```kotlin
class MasteryCalculator(
    private val repository: ExamRepository,
    private val melcRepository: MelcRepository
) {
    /**
     * Recalculates MELC mastery for a student after new assessment
     * Aggregates performance across all exams that map to each MELC
     */
    suspend fun updateStudentMastery(studentId: Long) = withContext(Dispatchers.Default) {
        // Get all exams the student has taken
        val studentRecords = repository.getAllStudentRecords(studentId)
        
        // Get all MELC mappings for those exams
        val melcScores = mutableMapOf<Long, MutableList<MelcScore>>()
        
        studentRecords.forEach { student ->
            val examId = student.examId
            val answerKeys = repository.getAnswerKeys(examId).first()
            val studentAnswers = repository.getStudentAnswers(student.id)
            val melcMappings = melcRepository.getMelcsForExam(examId)
            
            // For each MELC mapped in this exam
            melcMappings.forEach { (melc, questions) ->
                val melcId = melc.id
                
                // Calculate points earned for this MELC in this exam
                var earned = 0
                var possible = 0
                
                questions.forEach { questionNumber ->
                    val key = answerKeys.find { it.questionNumber == questionNumber }
                    if (key != null) {
                        possible += key.points
                        val answer = studentAnswers.find { it.questionNumber == questionNumber }
                        if (answer?.answer == key.correctAnswer || 
                            key.alternativeAnswers.split(",").contains(answer?.answer)) {
                            earned += key.points
                        }
                    }
                }
                
                melcScores.getOrPut(melcId) { mutableListOf() }
                    .add(MelcScore(earned, possible))
            }
        }
        
        // Update mastery records
        melcScores.forEach { (melcId, scores) ->
            val totalEarned = scores.sumOf { it.earned }
            val totalPossible = scores.sumOf { it.possible }
            val percentage = if (totalPossible > 0) {
                (totalEarned.toFloat() / totalPossible * 100)
            } else 0f
            
            val masteryLevel = determineMasteryLevel(percentage)
            
            // Insert or update mastery record
            melcRepository.insertOrUpdateMastery(
                studentId = studentId,
                melcId = melcId,
                totalPoints = totalEarned,
                maxPoints = totalPossible,
                percentage = percentage,
                masteryLevel = masteryLevel,
                assessmentCount = scores.size
            )
        }
    }
    
    /**
     * Determines mastery level based on percentage
     * Aligned with DepEd grading standards
     */
    private fun determineMasteryLevel(percentage: Float): String {
        return when {
            percentage >= 90f -> "Advanced"
            percentage >= 80f -> "Proficient"
            percentage >= 75f -> "Approaching"
            else -> "Developing"
        }
    }
    
    data class MelcScore(val earned: Int, val possible: Int)
    
    /**
     * Gets competency mastery summary for student profile
     */
    suspend fun getMasterySummary(studentId: Long): MasterySummary {
        val masteryRecords = melcRepository.getStudentMastery(studentId)
        
        val advanced = masteryRecords.count { it.masteryLevel == "Advanced" }
        val proficient = masteryRecords.count { it.masteryLevel == "Proficient" }
        val approaching = masteryRecords.count { it.masteryLevel == "Approaching" }
        val developing = masteryRecords.count { it.masteryLevel == "Developing" }
        
        val totalAssessed = masteryRecords.size
        val averagePercentage = masteryRecords.map { it.percentage }.average().toFloat()
        
        // Identify strengths (≥85%) and weaknesses (<70%)
        val strengths = masteryRecords.filter { it.percentage >= 85f }
            .sortedByDescending { it.percentage }
            .take(5)
        val weaknesses = masteryRecords.filter { it.percentage < 70f }
            .sortedBy { it.percentage }
            .take(5)
        
        return MasterySummary(
            totalMelcsAssessed = totalAssessed,
            advanced = advanced,
            proficient = proficient,
            approaching = approaching,
            developing = developing,
            averagePercentage = averagePercentage,
            strengths = strengths,
            weaknesses = weaknesses
        )
    }
    
    data class MasterySummary(
        val totalMelcsAssessed: Int,
        val advanced: Int,
        val proficient: Int,
        val approaching: Int,
        val developing: Int,
        val averagePercentage: Float,
        val strengths: List<StudentMelcMasteryEntity>,
        val weaknesses: List<StudentMelcMasteryEntity>
    )
}
```


### 9. Pacing and Curriculum Tracking

#### PacingEngine Service
```kotlin
class PacingEngine(
    private val melcRepository: MelcRepository
) {
    data class PacingGuide(
        val quarter: Int,
        val melcs: List<MelcWithSchedule>,
        val totalMelcs: Int,
        val assessedMelcs: Int,
        val coveragePercentage: Float,
        val behindSchedule: List<MelcEntity>
    )
    
    data class MelcWithSchedule(
        val melc: MelcEntity,
        val recommendedWeek: Int,           // Week in quarter (1-10)
        val status: CoverageStatus,
        val lastAssessedDate: Long?,
        val assessmentCount: Int
    )
    
    enum class CoverageStatus {
        NOT_ASSESSED,                       // Never assessed
        PARTIALLY_ASSESSED,                 // Assessed but not complete coverage
        FULLY_ASSESSED,                     // Adequately assessed
        BEHIND_SCHEDULE                     // Past recommended date without assessment
    }
    
    /**
     * Gets pacing guide for a subject in a specific quarter
     * Compares current progress against DepEd recommended timeline
     */
    suspend fun getPacingGuide(
        subjectId: Long,
        gradeLevel: Int,
        quarter: Int,
        currentWeek: Int
    ): PacingGuide {
        val subject = repository.getSubject(subjectId)
        val melcs = melcRepository.getMelcsByQuarter(subject.name, gradeLevel, quarter).first()
        
        val weeksPerQuarter = 10
        val melcsPerWeek = melcs.size.toFloat() / weeksPerQuarter
        
        // Assign recommended week to each MELC
        val melcsWithSchedule = melcs.mapIndexed { index, melc ->
            val recommendedWeek = ((index / melcsPerWeek).toInt() + 1).coerceIn(1, weeksPerQuarter)
            val assessments = getAssessmentsForMelc(melc.id)
            
            val status = when {
                assessments.isEmpty() && recommendedWeek < currentWeek -> CoverageStatus.BEHIND_SCHEDULE
                assessments.isEmpty() -> CoverageStatus.NOT_ASSESSED
                assessments.size < 2 -> CoverageStatus.PARTIALLY_ASSESSED
                else -> CoverageStatus.FULLY_ASSESSED
            }
            
            MelcWithSchedule(
                melc = melc,
                recommendedWeek = recommendedWeek,
                status = status,
                lastAssessedDate = assessments.maxOfOrNull { it.date },
                assessmentCount = assessments.size
            )
        }
        
        val assessedCount = melcsWithSchedule.count { 
            it.status != CoverageStatus.NOT_ASSESSED && it.status != CoverageStatus.BEHIND_SCHEDULE 
        }
        val behindSchedule = melcsWithSchedule
            .filter { it.status == CoverageStatus.BEHIND_SCHEDULE }
            .map { it.melc }
        
        return PacingGuide(
            quarter = quarter,
            melcs = melcsWithSchedule,
            totalMelcs = melcs.size,
            assessedMelcs = assessedCount,
            coveragePercentage = (assessedCount.toFloat() / melcs.size * 100),
            behindSchedule = behindSchedule
        )
    }
    
    /**
     * Marks MELC as manually covered (for non-assessment coverage)
     */
    suspend fun markMelcCovered(melcId: Long, subjectId: Long, notes: String = "") {
        // Store manual coverage record
        melcRepository.insertManualCoverage(
            melcId = melcId,
            subjectId = subjectId,
            coveredAt = System.currentTimeMillis(),
            notes = notes
        )
    }
    
    /**
     * Generates quarterly coverage report
     */
    suspend fun getQuarterlySummary(
        subjectId: Long,
        gradeLevel: Int,
        schoolYear: String
    ): QuarterlySummary {
        val quarters = (1..4).map { quarter ->
            val guide = getPacingGuide(subjectId, gradeLevel, quarter, currentWeek = 10)
            QuarterProgress(
                quarter = quarter,
                totalMelcs = guide.totalMelcs,
                assessedMelcs = guide.assessedMelcs,
                coveragePercentage = guide.coveragePercentage
            )
        }
        
        return QuarterlySummary(
            schoolYear = schoolYear,
            gradeLevel = gradeLevel,
            quarters = quarters,
            overallCoverage = quarters.map { it.coveragePercentage }.average().toFloat()
        )
    }
    
    data class QuarterProgress(
        val quarter: Int,
        val totalMelcs: Int,
        val assessedMelcs: Int,
        val coveragePercentage: Float
    )
    
    data class QuarterlySummary(
        val schoolYear: String,
        val gradeLevel: Int,
        val quarters: List<QuarterProgress>,
        val overallCoverage: Float
    )
}
```


### 10. Data Portability (User-Owned Backup)

The app is **offline-only** and has **no server-side component**. There is no provider-hosted
sync service, no Firebase Cloud Firestore data sync, and no automatic remote backup. Users own
their data and retain sole custody of it.

Data portability is provided **exclusively** through the on-device Backup/Restore mechanism
(Requirement 14), implemented by the existing `BackupManager` and surfaced in
`BackupManagementScreen`:

- **Backup** exports the complete encrypted database to a backup file written to device storage.
  The user can then move that file to any location or cloud service of their own choosing.
- **Restore** reads a user-selected backup file, validates its integrity, and applies it.
- All backup/restore operations are entirely local and require no network connectivity, so they
  work identically whether the device is online or offline.
- Backup and Restore are available to all users regardless of subscription tier.

> Note: The `sync_logs` table / `SyncLogEntity` is retained in the schema and in `MIGRATION_5_6`
> (see the migration section) but is currently **unused/reserved**. No sync feature reads or
> writes it.


### 11. Subscription and Billing System

#### SubscriptionManager Service
```kotlin
class SubscriptionManager(
    private val context: Context,
    private val billingClient: BillingClient
) {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()
    
    sealed class SubscriptionState {
        object Free : SubscriptionState()
        data class Premium(val expiryDate: Long) : SubscriptionState()
        object PendingPurchase : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }
    
    data class TierLimits(
        val maxSubjects: Int,
        val maxExamsPerSubject: Int,
        val maxScansPerExam: Int,
        val allowsAdvancedAnalytics: Boolean,
        val allowsSchoolReports: Boolean,
        val allowsCustomTemplates: Boolean,
        val allowsCurriculumTracking: Boolean
    )
    
    companion object {
        const val PREMIUM_SKU = "premium_monthly_100php"
        
        val FREE_LIMITS = TierLimits(
            maxSubjects = 3,
            maxExamsPerSubject = 5,
            maxScansPerExam = 30,
            allowsAdvancedAnalytics = false,
            allowsSchoolReports = false,
            allowsCustomTemplates = false,
            allowsCurriculumTracking = false
        )
        
        val PREMIUM_LIMITS = TierLimits(
            maxSubjects = Int.MAX_VALUE,
            maxExamsPerSubject = Int.MAX_VALUE,
            maxScansPerExam = Int.MAX_VALUE,
            allowsAdvancedAnalytics = true,
            allowsSchoolReports = true,
            allowsCustomTemplates = true,
            allowsCurriculumTracking = true
        )
    }
    
    /**
     * Initializes billing client and checks subscription status
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        val billingResult = suspendCoroutine<BillingResult> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    continuation.resume(result)
                }
                
                override fun onBillingServiceDisconnected() {
                    // Retry connection
                }
            })
        }
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            checkSubscriptionStatus()
        }
    }
    
    /**
     * Checks current subscription status with Google Play
     */
    suspend fun checkSubscriptionStatus() = withContext(Dispatchers.IO) {
        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val activePurchase = purchasesResult.purchasesList.firstOrNull { purchase ->
            purchase.products.contains(PREMIUM_SKU) && purchase.isAcknowledged
        }
        
        if (activePurchase != null) {
            // User has active subscription
            _subscriptionState.value = SubscriptionState.Premium(
                expiryDate = getExpiryDate(activePurchase)
            )
            saveSubscriptionState(isPremium = true)
        } else {
            _subscriptionState.value = SubscriptionState.Free
            saveSubscriptionState(isPremium = false)
        }
    }
    
    /**
     * Initiates premium subscription purchase
     */
    suspend fun subscribeToPremium(activity: Activity): Result<Unit> {
        _subscriptionState.value = SubscriptionState.PendingPurchase
        
        return try {
            // Query product details
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PREMIUM_SKU)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            
            val productDetailsResult = withContext(Dispatchers.IO) {
                billingClient.queryProductDetails(params)
            }
            
            val productDetails = productDetailsResult.productDetailsList?.firstOrNull()
                ?: return Result.failure(Exception("Product not found"))
            
            // Launch billing flow
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return Result.failure(Exception("Offer not found"))
            
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            
            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Result.success(Unit)
            } else {
                _subscriptionState.value = SubscriptionState.Error("Purchase failed")
                Result.failure(Exception("Billing flow failed: ${billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            _subscriptionState.value = SubscriptionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Gets current tier limits based on subscription status
     */
    fun getCurrentLimits(): TierLimits {
        return when (_subscriptionState.value) {
            is SubscriptionState.Premium -> PREMIUM_LIMITS
            else -> FREE_LIMITS
        }
    }
    
    /**
     * Checks if operation is allowed under current tier
     */
    suspend fun checkLimit(operation: LimitedOperation): Boolean {
        val limits = getCurrentLimits()
        
        return when (operation) {
            is LimitedOperation.CreateSubject -> {
                val currentCount = localDb.subjectDao().getActiveSubjects().first().size
                currentCount < limits.maxSubjects
            }
            is LimitedOperation.CreateExam -> {
                val currentCount = localDb.examDao().getExamsBySubject(operation.subjectId).first().size
                currentCount < limits.maxExamsPerSubject
            }
            is LimitedOperation.ScanSheet -> {
                val currentCount = localDb.examDao().getStudents(operation.examId).first().size
                currentCount < limits.maxScansPerExam
            }
            is LimitedOperation.AdvancedAnalytics -> limits.allowsAdvancedAnalytics
            is LimitedOperation.SchoolReport -> limits.allowsSchoolReports
            is LimitedOperation.CustomTemplate -> limits.allowsCustomTemplates
            is LimitedOperation.CurriculumTracking -> limits.allowsCurriculumTracking
        }
    }
    
    sealed class LimitedOperation {
        object CreateSubject : LimitedOperation()
        data class CreateExam(val subjectId: Long) : LimitedOperation()
        data class ScanSheet(val examId: Long) : LimitedOperation()
        object AdvancedAnalytics : LimitedOperation()
        object SchoolReport : LimitedOperation()
        object CustomTemplate : LimitedOperation()
        object CurriculumTracking : LimitedOperation()
    }
}
```


### 12. Answer Sheet Parser System

Covers Requirement 22 (Parser for Scanned Answer Sheets) and integrates with Requirement 17 (Preserve Existing Scanner).

#### AnswerSheetParser Service
```kotlin
class AnswerSheetParser(
    private val qrParser: QRCodeParser,
    private val bubbleDetector: BubbleDetectionEngine,
    private val melcRepository: MelcRepository
) {
    data class ParseResult(
        val examMetadata: QRCodeGenerator.ExamMetadata?,  // null if QR failed
        val studentId: String?,
        val studentName: String?,
        val answers: List<DetectedAnswer>,
        val overallConfidence: Float,
        val usedQrCode: Boolean
    )

    data class DetectedAnswer(
        val questionNumber: Int,
        val answer: String,              // "A", "B", ..., "Invalid - Multiple", "No Answer"
        val bubbleState: BubbleState,
        val confidence: Float            // 0.0 - 1.0
    )

    enum class BubbleState {
        EMPTY,      // 0-20% filled
        PARTIAL,    // 21-79% filled
        SHADED,     // 80-100% filled
        MULTIPLE    // >1 bubble shaded for a question
    }

    /**
     * Full parse pipeline (Req 22.1-22.7):
     * 1. Attempt QR detection (target < 1 second)
     * 2. If QR found, validate metadata against DB
     * 3. If QR fails, fall back to bubble-only processing
     * 4. Detect and classify bubbles, resolve multi-marks
     * 5. Return structured result with confidence scores
     */
    suspend fun parse(bitmap: Bitmap): ParseResult = withContext(Dispatchers.Default) {
        val metadata = qrParser.parseQRCode(bitmap)  // Req 22.1, 22.2
        val detection = bubbleDetector.detectBubbles(bitmap)  // Req 22.3, 22.4

        val answers = detection.questions.map { q ->
            val shadedOptions = q.options.filter { it.state == BubbleState.SHADED }
            val resolvedAnswer = when {
                shadedOptions.isEmpty() -> "No Answer"
                shadedOptions.size > 1 -> "Invalid - Multiple"   // Req 22.6
                else -> shadedOptions.first().label
            }
            DetectedAnswer(
                questionNumber = q.number,
                answer = resolvedAnswer,
                bubbleState = shadedOptions.firstOrNull()?.state ?: BubbleState.EMPTY,
                confidence = q.confidence
            )
        }

        ParseResult(
            examMetadata = metadata,
            studentId = detection.studentId,
            studentName = detection.studentName,
            answers = answers,
            overallConfidence = detection.overallConfidence,
            usedQrCode = metadata != null
        )
    }
}
```

#### BubbleDetectionEngine Service
```kotlin
class BubbleDetectionEngine {
    data class DetectionResult(
        val studentId: String?,
        val studentName: String?,
        val questions: List<QuestionDetection>,
        val overallConfidence: Float
    )

    data class QuestionDetection(
        val number: Int,
        val options: List<OptionDetection>,
        val confidence: Float
    )

    data class OptionDetection(
        val label: String,                               // "A".."G"
        val fillRatio: Float,                            // 0.0 - 1.0
        val state: AnswerSheetParser.BubbleState
    )

    /**
     * Classifies bubble fill using threshold + contour detection (Req 22.4, 22.5):
     * EMPTY 0-20%, PARTIAL 21-79%, SHADED 80-100%
     */
    fun classifyFill(fillRatio: Float): AnswerSheetParser.BubbleState = when {
        fillRatio <= 0.20f -> AnswerSheetParser.BubbleState.EMPTY
        fillRatio < 0.80f -> AnswerSheetParser.BubbleState.PARTIAL
        else -> AnswerSheetParser.BubbleState.SHADED
    }

    suspend fun detectBubbles(bitmap: Bitmap): DetectionResult { /* image processing */ }
}
```

#### BubbleGridMapper — printed-layout → pixel-grid bridge

`AnswerSheetParser.parse` needs a `BubbleDetectionEngine.BubbleGrid` describing where each option
bubble sits **in pixel space** on the scanned bitmap. The printed layout, however, is expressed in
**PDF points** (A4, 595×842 pt) by `AnswerSheetPrettyPrinter.SheetModel`. `BubbleGridMapper` is the
pure function that bridges the two coordinate systems:

```kotlin
object BubbleGridMapper {
    /**
     * Maps a printed AnswerSheetPrettyPrinter.SheetModel (PDF points) to a pixel-space
     * BubbleDetectionEngine.BubbleGrid for a scanned bitmap of the given dimensions.
     *
     * Each AnswerSheetPrettyPrinter.Bubble (center ± radius, in PDF points) becomes a clamped
     * pixel OptionRegion via independent per-axis linear scaling:
     *   scaleX = imageWidthPx  / pageWidthPt
     *   scaleY = imageHeightPx / pageHeightPt
     * Regions are clamped to the bitmap bounds so no OptionRegion falls off the image.
     */
    fun buildGrid(
        model: AnswerSheetPrettyPrinter.SheetModel,
        imageWidthPx: Int,
        imageHeightPx: Int,
        pageWidthPt: Float = 595f,
        pageHeightPt: Float = 842f
    ): BubbleDetectionEngine.BubbleGrid { /* pure mapping */ }
}
```

This is the connection between the pretty-printer geometry and the detection engine: because the app
knows the exact printed geometry of a sheet it generated, it can reconstruct the same layout in the
scan and grade it. The mapping is pure (no Android/bitmap dependencies in the coordinate math) and is
JVM-unit-tested by `BubbleGridMapperTest`.

#### Runtime scan pipeline (wired)

`ProcessingScreen` → `BubbleSheetProcessor.processImage(uri, totalQuestions, optionLabels)` now runs
the **real** pipeline end to end:

1. Load the captured image and cap it (existing 1920×1080 handling).
2. Rebuild the printed layout via `AnswerSheetPrettyPrinter.buildSheetModel`.
3. Map that model to a pixel-space `BubbleGrid` via `BubbleGridMapper.buildGrid`.
4. Run `AnswerSheetParser.parse(bitmap, grid)` (QR-first + real bubble-fill classification).
5. Return the detected answers.

This replaces an earlier mock in which `BubbleSheetProcessor.processImage` returned random answers.
`ProcessingScreen` gained `totalQuestions` and `optionLabels` parameters, and the MainActivity
`processing` route supplies `detailState.exam?.totalQuestions ?: 20` plus option labels derived from
the answer key (defaulting to A–D).

#### Known limitations (scanning)

The wired pipeline performs real detection but is scoped to a set of assumptions that should be tracked
before it is relied on for handheld field scans:

- **(a) No perspective correction / deskew** — the mapping assumes a reasonably square-on scan;
  angled or skewed photos will misalign the grid against the printed bubbles.
- **(b) App-printed sheets only** — detection targets sheets printed by the app's
  `AnswerSheetPrettyPrinter` (known geometry), not arbitrary third-party answer sheets.
- **(c) Single physical page per scan** — multi-page exams require one scan per page.
- **(d) On-device accuracy verification pending** — the logic compiles and the pure mapping is
  unit-tested (`BubbleGridMapperTest`), but on-device accuracy testing with real printed sheets is
  still needed.

Perspective correction / deskew is the recommended next step for reliable handheld field scans.

### 13. CSV/Excel Import/Export System

Covers Requirement 11 (CSV and Excel Import/Export) and supports Requirement 7 (roster import) and Requirement 24 (import validation).

#### ImportExportService
```kotlin
class ImportExportService(
    private val context: Context,
    private val repository: ExamRepository,
    private val sectionRepository: SectionRepository,
    private val melcRepository: MelcRepository,
    private val validation: ValidationEngine
) {
    sealed class ExportType {
        data class ExamResults(val examId: Long) : ExportType()      // Req 11.1
        data class ClassRoster(val sectionId: Long) : ExportType()   // Req 11.2
        data class MelcMastery(val sectionId: Long) : ExportType()   // Req 11.3
    }

    enum class FileFormat { CSV, XLSX }

    data class ImportResult(
        val successCount: Int,
        val failedRows: List<RowError>
    )

    data class RowError(val rowNumber: Int, val message: String)

    /**
     * Exports data to Downloads folder with descriptive timestamped names (Req 11.7).
     * CSV via manual writer, XLSX via Apache POI (Req 11.6).
     */
    suspend fun export(type: ExportType, format: FileFormat): String =
        withContext(Dispatchers.IO) {
            val rows = when (type) {
                is ExportType.ExamResults -> buildExamResultRows(type.examId)
                is ExportType.ClassRoster -> buildRosterRows(type.sectionId)
                is ExportType.MelcMastery -> buildMasteryRows(type.sectionId)
            }
            val fileName = buildFileName(type, format)
            writeToDownloads(fileName, rows, format)
        }

    /**
     * Imports student roster (Req 11.4). Validates required headers and each row
     * (Req 11.5, Req 24.5), returning per-row errors without aborting the whole import.
     */
    suspend fun importRoster(sectionId: Long, uri: Uri, format: FileFormat): ImportResult =
        withContext(Dispatchers.IO) {
            val rows = readRows(uri, format)
            requireHeaders(rows, listOf("student_id", "name", "grade_level"))
            val errors = mutableListOf<RowError>()
            var success = 0
            rows.forEachIndexed { index, row ->
                val idResult = validation.validateStudentId(row["student_id"] ?: "")
                val nameValid = (row["name"] ?: "").isNotBlank()
                when {
                    idResult is ValidationEngine.ValidationResult.Invalid ->
                        errors.add(RowError(index + 1, idResult.message))
                    !nameValid ->
                        errors.add(RowError(index + 1, "Name is required"))
                    else -> {
                        // Delegates to SectionRepository.importRoster for the validated row
                        sectionRepository.importRoster(sectionId, listOf(row)); success++
                    }
                }
            }
            ImportResult(success, errors)
        }
}
```

### 14. Localization, Onboarding, Backup, and Notifications

#### LocalizationManager (Requirement 20)
```kotlin
class LocalizationManager(private val context: Context) {
    enum class AppLanguage(val code: String) { ENGLISH("en"), FILIPINO("fil") }

    /**
     * Persists language in SharedPreferences (Req 20.6) and applies via AppCompat
     * per-app locales so UI updates without restart (Req 20.3).
     * MELC descriptions remain in English (Req 20.5).
     */
    fun setLanguage(language: AppLanguage) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("app_language", language.code).apply()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code)
        )
    }

    fun getCurrentLanguage(): AppLanguage {
        val saved = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("app_language", null)
        return AppLanguage.values().firstOrNull { it.code == saved }
            ?: systemDefaultOrEnglish()   // Req 20.7
    }
}
```
String resources live in `res/values/strings.xml` (English) and `res/values-fil/strings.xml` (Filipino), covering navigation, buttons, dialogs, errors, and report headers (Req 20.4).

#### OnboardingManager (Requirement 28)
```kotlin
class OnboardingManager(
    private val onboardingPreferences: OnboardingPreferences,  // EXISTING datastore
    private val repository: ExamRepository
) {
    /**
     * Shows the welcome tutorial on first launch (Req 28.1) covering subjects,
     * exams, MELC mapping, scanning, and reports (Req 28.2). Skippable (Req 28.6)
     * and re-accessible from Settings (Req 28.7).
     */
    suspend fun shouldShowTutorial(): Boolean = !onboardingPreferences.tutorialCompleted()

    /**
     * Seeds sample data (Req 28.5): 1 subject folder, 2 exams, 5 students with results.
     */
    suspend fun seedSampleData() { /* insert sample subject/exams/students */ }
}
```

#### BackupManager (Requirement 29)
```kotlin
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val encryption: DatabaseEncryption
) {
    /**
     * Exports the complete database, images, templates, and config to an
     * encrypted ZIP in device storage with a timestamped name (Req 29.2-29.4):
     * offline_assessment_backup_yyyy_MM_dd_HH_mm.zip
     */
    suspend fun backup(): String = withContext(Dispatchers.IO) { /* zip + encrypt */ }

    /**
     * Validates backup integrity before applying (Req 29.6), then triggers an
     * app restart to reload data (Req 29.7).
     */
    suspend fun restore(backupUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        if (!validateBackupIntegrity(backupUri)) {
            return@withContext Result.failure(Exception("Backup file is corrupted"))
        }
        applyBackup(backupUri)
        Result.success(Unit)
    }
}
```

#### NotificationService (Requirement 30)
```kotlin
class NotificationService(private val context: Context) {
    enum class NotificationType {
        BACKUP_SUCCESS, BACKUP_FAILED, RECYCLE_BIN_EXPIRY, LOW_STORAGE
    }

    /**
     * Posts backup (Req 30.1, 30.2), recycle-bin expiry (Req 30.3), and low-storage
     * (Req 30.4) notifications. Honors user preferences (Req 30.5) and system
     * Do Not Disturb (Req 30.6). Non-critical notices use in-app banners (Req 30.7).
     */
    fun notify(type: NotificationType, message: String, critical: Boolean = false) {
        if (!isTypeEnabled(type)) return
        if (critical) postSystemNotification(type, message)
        else emitInAppBanner(type, message)
    }
}
```

Low-storage checks (Req 27.5, 30.4) run via a periodic `StorageMonitor` that reads available bytes and warns below 100 MB.

### 15. Recycle Bin System (Requirement 10)

Soft deletion is implemented uniformly via `isDeleted` + `deletedAt` columns on `SubjectFolderEntity`, `SectionEntity`, `StudentEntity`, and `ExamEntity`. The `RecycleBinRepository` provides a unified view across entity types.

```kotlin
interface RecycleBinRepository {
    data class RecycleBinItem(
        val id: Long,
        val name: String,
        val itemType: String,        // "Subject", "Section", "Exam"
        val originalLocation: String,
        val deletedAt: Long
    )

    fun getDeletedItems(): Flow<List<RecycleBinItem>>   // Req 10.3
    suspend fun restore(itemType: String, id: Long)      // Req 10.4 - clears isDeleted/deletedAt
    suspend fun permanentlyDelete(itemType: String, id: Long)  // Req 10.5 - hard delete
    suspend fun purgeExpired(retentionDays: Int = 30)    // Req 10.2, 10.6 - deletes items older than 30 days
}
```

A `RecycleBinPurgeWorker` (WorkManager) runs daily to purge items whose `deletedAt` exceeds the 30-day retention window (Req 10.6) and to trigger the expiry notification (Req 30.3).

## Data Models

### Complete Database Schema (Current: Version 5 → Target: Version 6)

> **Baseline correction:** The database is **already at version 5** with **11 registered entities** and migrations 1→2, 2→3, 3→4, 4→5 shipped. It is opened encrypted (SQLCipher) as `exam_scanner_database`. The only schema work remaining is a **`MIGRATION_5_6`** that registers four entity files that already exist on disk but are not yet in `@Database`. There is **no** `v1 → v2` migration to build and **no** `AutoMigration` in use — existing tables must not be renamed or recreated.

**Currently registered (v5) — do not recreate:**
`SubjectFolderEntity`, `SectionEntity`, `MelcEntity`, `TemplateEntity`, `GradingScaleEntity`, `ExamEntity`, `AnswerKeyEntity`, `QuestionMelcMappingEntity`, `StudentEntity`, `StudentAnswerEntity`, `StudentMelcMasteryEntity`.

**To be registered by `MIGRATION_5_6` (files already exist, entities not yet in `@Database`):**
`StudentEnrollmentEntity`, `MelcCoverageEntity`, `StudentNoteEntity`, `SyncLogEntity` → bringing the total to **15 registered entities**.

#### Entity Relationship Diagram

> Note on student↔section: in the shipped v5 schema a student is linked to a section **directly** via `StudentEntity.sectionId` (FK to `sections`). `StudentEnrollmentEntity` (registered by `MIGRATION_5_6`) is the **net-new** join table that additionally supports multi-section membership; the diagram below shows that join relationship. `GradingScaleEntity` (registered, referenced by exams via the `gradingScale` string) is omitted from the diagram for brevity.

```
┌─────────────────────┐
│  SubjectFolderEntity│
│  (id, name, ...)    │
└──────────┬──────────┘
           │ 1:N
           │
┌──────────▼──────────┐       ┌─────────────────────┐
│   SectionEntity     │       │    ExamEntity       │
│  (id, subjectId,..) │       │  (id, name, ...)    │
└──────────┬──────────┘       └──────────┬──────────┘
           │ 1:N                          │ 1:N
           │                              │
┌──────────▼───────────┐      ┌──────────▼──────────┐
│StudentEnrollmentEntity│      │  AnswerKeyEntity    │
│(studentId, sectionId)│      │ (examId, question...)│
└──────────┬───────────┘      └─────────────────────┘
           │ N:1                          │
           │                              │ N:1
┌──────────▼──────────┐       ┌──────────▼──────────┐
│   StudentEntity     │◄──────│ StudentAnswerEntity │
│  (id, name, ...)    │ 1:N   │ (studentId, answer..)│
└──────────┬──────────┘       └─────────────────────┘
           │ 1:N
           │
┌──────────▼──────────┐       ┌─────────────────────┐
│StudentMelcMasteryEntity│     │    MelcEntity       │
│ (studentId, melcId...)│◄────┤  (id, code, desc..) │
└─────────────────────┘  N:1  └──────────┬──────────┘
                                          │ 1:N
                               ┌──────────▼──────────┐
                               │QuestionMelcMappingEntity│
                               │(examId, questionNum, melcId)│
                               └─────────────────────┘
```


### AppDatabase — current v5 declaration (shipped) and the target v6 change

The **shipped** declaration (do not rewrite this; only extend it):

```kotlin
@Database(
    entities = [
        SubjectFolderEntity::class,
        SectionEntity::class,
        MelcEntity::class,
        TemplateEntity::class,
        GradingScaleEntity::class,
        ExamEntity::class,
        AnswerKeyEntity::class,
        QuestionMelcMappingEntity::class,
        StudentEntity::class,
        StudentAnswerEntity::class,
        StudentMelcMasteryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao   // single DAO — there is no SubjectDao/SectionDao/MelcDao/etc.

    companion object {
        // ...built encrypted with SQLCipher SupportFactory + EncryptionKeyManager.getDatabasePassphrase(context),
        // DB name "exam_scanner_database", with .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
```

**Target v6 change (the remaining schema work):** register the four already-authored entity files, bump the version to 6, add `MIGRATION_5_6` to the builder, and (recommended) flip `exportSchema` to `true` so Room migration tests can run against the exported schema.

```kotlin
@Database(
    entities = [
        // ---- unchanged v5 entities ----
        SubjectFolderEntity::class,
        SectionEntity::class,
        MelcEntity::class,
        TemplateEntity::class,
        GradingScaleEntity::class,
        ExamEntity::class,
        AnswerKeyEntity::class,
        QuestionMelcMappingEntity::class,
        StudentEntity::class,
        StudentAnswerEntity::class,
        StudentMelcMasteryEntity::class,
        // ---- NET-NEW: files already exist in data/, just not registered yet ----
        StudentEnrollmentEntity::class,
        MelcCoverageEntity::class,
        StudentNoteEntity::class,
        SyncLogEntity::class
    ],
    version = 6,
    exportSchema = true          // recommended so MigrationTest can validate 5→6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    // New DAO methods for the four new tables are added to the existing ExamDao
    // (or a small additional DAO), consistent with the single-DAO pattern already in use.

    companion object {
        // .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

        /**
         * v5 → v6: register the four new tables. Purely additive — no existing table is
         * renamed, dropped, or recreated. Column shapes here match the shipped entity files
         * exactly (data/StudentEnrollmentEntity.kt, MelcCoverageEntity.kt, StudentNoteEntity.kt,
         * SyncLogEntity.kt).
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS student_enrollments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER NOT NULL,
                        sectionId INTEGER NOT NULL,
                        enrolledAt INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'Active',
                        FOREIGN KEY(studentId) REFERENCES students(id) ON DELETE CASCADE,
                        FOREIGN KEY(sectionId) REFERENCES sections(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_student_enrollments_studentId_sectionId ON student_enrollments(studentId, sectionId)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS melc_coverage (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        melcId INTEGER NOT NULL,
                        subjectId INTEGER NOT NULL,
                        coveredAt INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        coverageType TEXT NOT NULL DEFAULT 'Manual',
                        FOREIGN KEY(melcId) REFERENCES melcs(id) ON DELETE CASCADE,
                        FOREIGN KEY(subjectId) REFERENCES subject_folders(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS student_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(studentId) REFERENCES students(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        operation TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        message TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
```

> **Room FK index note:** Room emits a compile-time warning when a `@ForeignKey` child column lacks an index. `MelcCoverageEntity` and `StudentNoteEntity` reference parent tables via FK columns; if Room warnings are treated as errors, add matching indices to those entity files (and mirror them with `CREATE INDEX` in `MIGRATION_5_6`). This is a consistency detail to verify against the current entity files, not a redesign.
>
> **MELC seeding:** the bundled MELC dataset is loaded via `ExamRepository.initializeSampleMelcs()` (from `SampleMelcsData`) rather than an `onCreate` callback calling a `melcDao`. Expanding that dataset to full subject × grade × quarter coverage (Req 2.1) is feature work, not a migration.


### Additional Supporting Entities (NET-NEW files, registered by `MIGRATION_5_6`)

These three entity files (plus `StudentEnrollmentEntity` above) **already exist** under `app/src/main/java/com/examscanner/premium/data/` but are not yet in `@Database`. The definitions below match the shipped files.

```kotlin
/**
 * Tracks manual MELC coverage (non-assessment based)
 */
@Entity(
    tableName = "melc_coverage",
    foreignKeys = [
        ForeignKey(
            entity = MelcEntity::class,
            parentColumns = ["id"],
            childColumns = ["melcId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MelcCoverageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val melcId: Long,
    val subjectId: Long,
    val coveredAt: Long,
    val notes: String = "",
    val coverageType: String = "Manual"  // "Assessment" or "Manual"
)

/**
 * Teacher notes on student profiles
 */
@Entity(
    tableName = "student_notes",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Sync operation log for debugging
 */
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operation: String,              // "upload", "download", "conflict"
    val entityType: String,             // "exam", "student", etc.
    val entityId: Long,
    val status: String,                 // "success", "failed", "conflict"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
```

### Type Converters

> **Status:** The shipped v5 `AppDatabase` does **not** declare `@TypeConverters` — all columns are primitives (`Long`, `String`, `Int`, `Float`, `Boolean`) or JSON stored as `String`. The converter class below is **optional/net-new**; only introduce it (and the `@TypeConverters` annotation) if a new feature genuinely needs a non-primitive column type. Adding converters does not by itself require a migration, but changing an existing column's type would.

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return value.split(",").filter { it.isNotEmpty() }
    }
    
    @TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString(",")
    }
    
    @TypeConverter
    fun fromJson(value: String): Map<String, Any> {
        return Json.decodeFromString(value)
    }
    
    @TypeConverter
    fun toJson(map: Map<String, Any>): String {
        return Json.encodeToString(map)
    }
}
```


### MELCs Database Structure

The MELCs database contains approximately 5,000+ competencies organized by:
- **Subject**: Mathematics, Science, English, Filipino, Araling Panlipunan, etc.
- **Grade Level**: Grades 1-12
- **Quarter**: 1-4 (per school year)

#### Sample MELCs Data Structure

```kotlin
// Mathematics Grade 7, Quarter 1
MelcEntity(
    code = "M7NS-Ia-1",
    description = "Describes well-defined sets, subsets, universal sets, and the null set and cardinality of sets",
    subject = "Mathematics",
    gradeLevel = 7,
    quarter = 1,
    category = "Number and Number Sense"
)

MelcEntity(
    code = "M7NS-Ia-2",
    description = "Illustrates the union and intersection of sets and the difference of two sets",
    subject = "Mathematics",
    gradeLevel = 7,
    quarter = 1,
    category = "Number and Number Sense"
)

// Science Grade 7, Quarter 1
MelcEntity(
    code = "S7LT-Ia-1",
    description = "Describe the components of a scientific investigation",
    subject = "Science",
    gradeLevel = 7,
    quarter = 1,
    category = "Scientific Investigation"
)
```

#### MELCs Loading Strategy

> **Current reality:** MELCs are seeded from `SampleMelcsData` via `ExamRepository.initializeSampleMelcs()` (which calls `dao.insertMelcs(...)`), **not** from an `assets/melcs_database.json` file and **not** via a `melcDao`/`onCreate` callback. Also note `MelcEntity.gradeLevel` is a **`String`** and there is no `category` field, so any DTO/loader must match that shape.
>
> The remaining **feature work** is to expand the seeded dataset to cover every DepEd subject × grade 1-12 × quarter 1-4 (Req 2.1). Whether that expansion continues to live in `SampleMelcsData` (Kotlin) or moves to a bundled JSON asset is an implementation choice. If a JSON asset is adopted, the loader must produce entities with `gradeLevel: String` and no `category`, e.g.:

```kotlin
object MelcsDataLoader {
    /**
     * OPTIONAL net-new path: load bundled MELCs from assets/melcs_database.json.
     * Must match the shipped MelcEntity shape (gradeLevel is a String; no category field).
     */
    suspend fun loadBundledMelcs(context: Context): List<MelcEntity> =
        withContext(Dispatchers.IO) {
            val json = context.assets.open("melcs_database.json")
                .bufferedReader().use { it.readText() }
            Json.decodeFromString<List<MelcDto>>(json).map { dto ->
                MelcEntity(
                    code = dto.code,
                    description = dto.description,
                    gradeLevel = dto.gradeLevel,   // String
                    subject = dto.subject,
                    quarter = dto.quarter
                )
            }
        }

    @Serializable
    data class MelcDto(
        val code: String,
        val description: String,
        val subject: String,
        val gradeLevel: String,     // String to match MelcEntity
        val quarter: Int
    )
}
```
Seeding today is invoked through the existing `ExamRepository.initializeSampleMelcs()`; a JSON-backed loader would replace the body of that method rather than introduce a new DAO.


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

The following properties are derived from testable acceptance criteria. Non-property criteria (UI presence, offline operation, performance thresholds, encryption presence, accessibility, and the additive `MIGRATION_5_6` schema change) are covered by example, integration, smoke, and manual-audit tests described in the Testing Strategy. Migrations 1→5 are already shipped; the only migration still to write and test is the additive **5→6** that registers the four new tables.

### Property 1: Mastery level classification matches defined bands

*For any* percentage value in the range 0-100, `MasteryCalculator.determineMasteryLevel` SHALL return "Developing" for 0-74.99, "Approaching" for 75-79.99, "Proficient" for 80-89.99, and "Advanced" for 90-100.

**Validates: Requirements 2.5**

### Property 2: Mastery percentage is a bounded aggregation

*For any* set of (earned, possible) point pairs where each earned ≤ possible and possible ≥ 0, the aggregated mastery percentage SHALL equal (Σ earned / Σ possible) × 100 when Σ possible > 0 (and 0 otherwise), and SHALL always fall within the inclusive range 0-100.

**Validates: Requirements 2.4**

### Property 3: Difficulty index equals proportion correct

*For any* cohort of students and answers for a question, `calculateDifficulty` SHALL equal (number of correct responses / total students) × 100 and SHALL fall within the inclusive range 0-100 (returning 0 for an empty cohort).

**Validates: Requirements 6.1**

### Property 4: Discrimination index is bounded and classified consistently

*For any* cohort of at least 10 students, `calculateDiscriminationIndex` SHALL fall within the inclusive range -1.0 to +1.0, and *for any* index value `classifyDiscrimination` SHALL return Poor when < 0.20, Fair when 0.20-0.29, Good when 0.30-0.39, and Excellent when ≥ 0.40.

**Validates: Requirements 6.2, 6.3**

### Property 5: QR metadata round-trip

*For any* valid `ExamMetadata`, encoding it with `QRCodeGenerator.generateQRCode` and then decoding the result with `QRCodeParser.parseQRCode` SHALL produce metadata equivalent to the original.

**Validates: Requirements 4.1, 4.3, 4.5**

### Property 6: Answer sheet print-fill-scan-process round-trip

*For any* valid exam configuration (question count in {5, 10, 20, 50, 100}, option sets A-B through A-G, with or without QR code) and *any* set of validly filled answers, generating the answer sheet, simulating the shaded bubbles, and parsing the simulated sheet SHALL produce detected answers equal to the filled answers.

**Validates: Requirements 21.7, 23.2**

### Property 7: Parse-print-parse round-trip is idempotent

*For any* valid exam configuration and *any* parsed answer set, re-printing that parsed result into an answer sheet and parsing it again SHALL produce an answer set equivalent to the first parse (parse → print → parse yields a stable fixed point).

**Validates: Requirements 23.4**

### Property 8: Bubble parse resolution

*For any* question's detected option set, the parser SHALL resolve to "No Answer" when no bubble is shaded, to the single shaded option's label when exactly one bubble is shaded, and to "Invalid - Multiple" when more than one bubble is shaded; and *for any* fill ratio in 0.0-1.0, classification SHALL be EMPTY (≤0.20), PARTIAL (0.21-0.79), or SHADED (≥0.80).

**Validates: Requirements 22.5, 22.6**

### Property 9: CSV/Excel roster round-trip

*For any* set of valid roster records, exporting them to CSV or XLSX and then importing the produced file SHALL yield records equivalent to the originals.

**Validates: Requirements 11.2, 11.4, 11.6**

### Property 10: Input validation partitions inputs correctly

*For any* input drawn from the invalid partition (empty/whitespace names, question counts < 1 or > 200, student IDs with illegal characters or exceeding 20 characters, section names exceeding 50 characters, profile notes exceeding 500 characters, duplicate MELC mappings), `ValidationEngine` SHALL return Invalid; and *for any* input from the valid partition it SHALL return Valid.

**Validates: Requirements 7.2, 8.5, 24.1, 24.2, 24.3**

### Property 11: Recycle bin retention purge

*For any* set of soft-deleted items with arbitrary deletion timestamps, `purgeExpired(30)` SHALL permanently remove exactly the items whose deletion timestamp is older than 30 days and SHALL retain all others.

**Validates: Requirements 10.2, 10.6**

### Property 12: Tier limit enforcement

*For any* current resource count and tier, `checkLimit` SHALL return true if and only if the count is below the tier's limit (3 subjects, 5 exams per subject, 30 scans per exam for Free); Premium SHALL always return true for these count-based limits.

**Validates: Requirements 15.1, 15.2, 15.3, 16.4**

> Note: A former Property 13 (sync conflict resolution, last-write-wins) was removed because the
> app is offline-only and has no sync feature. The remaining 12 properties are not renumbered.

## Error Handling

### Error Handling Strategy

The system implements a comprehensive error handling strategy across all layers:

#### 1. Repository Layer Errors
```kotlin
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Exception, val message: String) : RepositoryResult<Nothing>()
    object Loading : RepositoryResult<Nothing>()
}

// Example usage in repository
suspend fun getExam(examId: Long): RepositoryResult<ExamEntity> {
    return try {
        val exam = examDao.getExam(examId)
        if (exam != null) {
            RepositoryResult.Success(exam)
        } else {
            RepositoryResult.Error(
                NotFoundException("Exam not found"),
                "Exam with ID $examId does not exist"
            )
        }
    } catch (e: Exception) {
        RepositoryResult.Error(e, "Failed to retrieve exam: ${e.message}")
    }
}
```

#### 2. ViewModel Error Propagation
```kotlin
data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null
)

sealed class UiError {
    data class Network(val message: String) : UiError()
    data class Database(val message: String) : UiError()
    data class Validation(val field: String, val message: String) : UiError()
    data class Permission(val permission: String) : UiError()
    data class LimitReached(val limit: String, val upgrade: Boolean = true) : UiError()
    data class FileOperation(val operation: String, val message: String) : UiError()
}

// ViewModel handles errors and converts to UI-friendly format
viewModelScope.launch {
    _state.value = _state.value.copy(isLoading = true, error = null)
    
    when (val result = repository.getExam(examId)) {
        is RepositoryResult.Success -> {
            _state.value = _state.value.copy(
                data = result.data,
                isLoading = false
            )
        }
        is RepositoryResult.Error -> {
            _state.value = _state.value.copy(
                isLoading = false,
                error = UiError.Database(result.message)
            )
        }
    }
}
```

#### 3. UI Error Display
```kotlin
@Composable
fun ErrorSnackbar(error: UiError?, onDismiss: () -> Unit) {
    error?.let {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                when (it) {
                    is UiError.LimitReached -> {
                        TextButton(onClick = { /* Navigate to subscription */ }) {
                            Text("UPGRADE")
                        }
                    }
                    is UiError.FileOperation -> {
                        TextButton(onClick = { /* Retry file operation */ }) {
                            Text("RETRY")
                        }
                    }
                    else -> {
                        TextButton(onClick = onDismiss) {
                            Text("DISMISS")
                        }
                    }
                }
            }
        ) {
            Text(getErrorMessage(it))
        }
    }
}

fun getErrorMessage(error: UiError): String {
    return when (error) {
        is UiError.Network -> "No internet connection. ${error.message}"
        is UiError.Database -> "Database error: ${error.message}"
        is UiError.Validation -> "${error.field}: ${error.message}"
        is UiError.Permission -> "Permission required: ${error.permission}"
        is UiError.LimitReached -> "Free tier limit reached: ${error.limit}"
        is UiError.FileOperation -> "File error (${error.operation}): ${error.message}"
    }
}
```

#### 4. Specific Error Scenarios

**Scanner Errors:**
```kotlin
sealed class ScannerError {
    object CameraPermissionDenied : ScannerError()
    object ImageProcessingFailed : ScannerError()
    object QRCodeNotDetected : ScannerError()
    object BubbleDetectionFailed : ScannerError()
    data class MLKitError(val message: String) : ScannerError()
}
```

**Subscription Errors:**
```kotlin
sealed class BillingError {
    object BillingUnavailable : BillingError()
    object ProductNotFound : BillingError()
    object PurchaseCancelled : BillingError()
    data class PurchaseFailed(val responseCode: Int) : BillingError()
    object VerificationFailed : BillingError()
}
```

#### 5. Requirement 27 Coverage Map

The following components collectively satisfy all seven acceptance criteria of Requirement 27:

| Criterion | Behavior | Design Element |
|-----------|----------|----------------|
| 27.1 | Retry a failed transient local operation (file export/import, image processing) up to 3 times with exponential backoff | `RetryPolicy.withBackoff` (`attempt < 3`) |
| 27.2 | Roll back failed DB operations and show error | `database.withTransaction { }` + `RepositoryResult.Error` → `UiError.Database` |
| 27.3 | Log image-processing failures and allow retry or manual entry | `ScannerError` + `CrashReporter.recordException` + manual-entry fallback |
| 27.4 | Preserve unsaved data in temp storage; recover on next launch | `DraftRecoveryManager` |
| 27.5 | Warn when storage is insufficient | `StorageMonitor` |
| 27.6 | Log errors locally without exposing sensitive data | `SecureLogger` (workspace standard) + `ErrorLog` |
| 27.7 | Offer "Report Issue" on critical errors | `IssueReporter` |

**Transient Local-Operation Retry With Exponential Backoff (Req 27.1):**
```kotlin
class RetryPolicy(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 1_000
) {
    /**
     * Retries a suspending local operation (e.g., file export/import or image
     * processing) up to [maxAttempts] times, doubling the delay after each failure
     * (1s, 2s, 4s). Rethrows after the final attempt so the caller can surface a
     * UiError.FileOperation.
     */
    suspend fun <T> withBackoff(block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts - 1) {
                    delay(baseDelayMs * (1L shl attempt))  // 1s, 2s, 4s
                }
            }
        }
        throw lastError ?: IllegalStateException("Operation failed with no captured error")
    }
}
```

**Crash Recovery / Draft Preservation (Req 27.4):**
```kotlin
class DraftRecoveryManager(private val context: Context) {
    /**
     * Persists an in-progress edit (e.g., an exam or answer key being created) to a
     * temp file so it survives a process death or crash (Req 27.4). Drafts are keyed
     * by screen so they can be offered for recovery on next launch. No student PII is
     * stored beyond what the user has already entered on-screen.
     */
    fun saveDraft(key: String, json: String) {
        File(draftDir(), "$key.json").writeText(json)
    }

    fun loadDraft(key: String): String? =
        File(draftDir(), "$key.json").takeIf { it.exists() }?.readText()

    fun clearDraft(key: String) {
        File(draftDir(), "$key.json").delete()
    }

    /** Called on app launch to detect recoverable drafts and prompt the user. */
    fun pendingDrafts(): List<String> =
        draftDir().listFiles()?.map { it.nameWithoutExtension } ?: emptyList()

    private fun draftDir(): File =
        File(context.filesDir, "drafts").apply { mkdirs() }
}
```

**Sensitive-Data-Safe Error Logging (Req 27.6):**
```kotlin
/**
 * All error logging routes through the workspace SecureLogger, which is disabled in
 * production release builds and never emits sensitive values. Errors are also
 * appended to a local rotating file (ErrorLog) for the "Report Issue" flow, with
 * student PII (names, IDs, contact info) redacted before being written (Req 27.6).
 */
object ErrorLog {
    fun record(tag: String, throwable: Throwable) {
        SecureLogger.e(tag, "Recoverable error", throwable)   // never logs PII
        appendRedacted(tag, throwable.redactedSummary())
    }
}
```

**Report Issue (Req 27.7):**
```kotlin
class IssueReporter(private val context: Context) {
    /**
     * Surfaced when a critical error occurs (Req 27.7). Bundles the redacted local
     * ErrorLog and device/app metadata (version, Android level, free storage) into a
     * pre-filled support email or Crashlytics report. Never attaches student data.
     */
    fun reportIssue(summary: String) { /* build redacted report + share intent */ }
}
```

Low-storage warnings (Req 27.5) reuse the same `StorageMonitor` described in the Notification section, surfacing a `UiError`/banner when free space drops below 100 MB.


## Testing Strategy

### Testing Approach

The system employs a comprehensive testing strategy covering property-based tests, unit tests, integration tests, and end-to-end testing. Property tests verify the universal correctness properties above; unit tests cover concrete examples and edge cases; integration/smoke tests cover I/O, migration, and configuration; manual audits cover accessibility.

#### 0. Property-Based Testing

Property tests are the primary mechanism for validating the Correctness Properties. They use **[jqwik](https://jqwik.net/)** (JUnit 5 property library for Kotlin/Java) rather than a hand-rolled generator framework.

Rules:
- Each of the 13 correctness properties is implemented by a **single** property-based test.
- Each property test runs a **minimum of 100 iterations** (`@Property(tries = 100)` or higher).
- Each test is tagged with a comment referencing its design property, using the format:
  `// Feature: offline-assessment-transformation, Property {number}: {property_text}`
- Pure-logic properties (1-5, 8-13) run entirely in-memory. The two answer-sheet round-trip properties (6: print→fill→scan→process, and 7: parse→print→parse) use a deterministic simulated `BubbleDetectionEngine` so the pipelines can be exercised 100+ times without a camera or real image I/O.

Example (Property 1 — mastery classification):
```kotlin
// Feature: offline-assessment-transformation, Property 1: Mastery level classification matches defined bands
@Property(tries = 100)
fun masteryLevelMatchesBands(@ForAll @FloatRange(min = 0f, max = 100f) percentage: Float) {
    val level = MasteryCalculator.determineMasteryLevel(percentage)
    val expected = when {
        percentage >= 90f -> "Advanced"
        percentage >= 80f -> "Proficient"
        percentage >= 75f -> "Approaching"
        else -> "Developing"
    }
    assertEquals(expected, level)
}
```

Example (Property 6 — answer-sheet round-trip):
```kotlin
// Feature: offline-assessment-transformation, Property 6: Answer sheet print-fill-scan-process round-trip
@Property(tries = 100)
fun answerSheetRoundTrip(
    @ForAll("examConfigs") config: AnswerSheetPrettyPrinter.SheetConfig,
    @ForAll("validAnswers") filled: Map<Int, String>
) {
    val sheetModel = prettyPrinter.buildSheetModel(config)
    val simulated = simulateFill(sheetModel, filled)          // deterministic bubble fills
    val result = parser.parse(simulated)                       // uses simulated detector
    val detected = result.answers.associate { it.questionNumber to it.answer }
    assertEquals(filled, detected.filterKeys { it in filled.keys })
}
```

Example (Property 7 — parse→print→parse idempotence):
```kotlin
// Feature: offline-assessment-transformation, Property 7: Parse-print-parse round-trip is idempotent
@Property(tries = 100)
fun parsePrintParseIsStable(
    @ForAll("examConfigs") config: AnswerSheetPrettyPrinter.SheetConfig,
    @ForAll("validAnswers") filled: Map<Int, String>
) {
    val firstParse = parser.parse(simulateFill(prettyPrinter.buildSheetModel(config), filled))
    val reprinted = prettyPrinter.buildSheetModel(config)
    val secondParse = parser.parse(simulateFill(reprinted, firstParse.answers.associate { it.questionNumber to it.answer }))
    assertEquals(
        firstParse.answers.map { it.questionNumber to it.answer },
        secondParse.answers.map { it.questionNumber to it.answer }
    )
}
```

Non-property coverage:
- **Migration (Req 18)**: Migrations 1→5 are already shipped. `MigrationTest` seeds a **v5** database, runs `MIGRATION_5_6`, and validates that the four new tables (`student_enrollments`, `melc_coverage`, `student_notes`, `sync_logs`) are created with the correct columns/indices and that **all pre-existing v5 data is preserved untouched** (no table renamed or recreated). Requires `exportSchema = true` and the `androidx.room:room-testing` `MigrationTestHelper`. Note: because the production DB is opened with SQLCipher, the migration test must either open its test DB with the same `SupportFactory` or run against an unencrypted in-test DB — call this out when implementing.
- **Offline (Req 13)**: airplane-mode integration tests confirm all core flows work without network.
- **Performance (Req 19)**: benchmark tests assert launch < 3s, list load < 1s, scan < 5s, and no query > 500ms.
- **Security (Req 25.1)**: smoke test verifies the database is opened via the SQLCipher `SupportFactory`.
- **Accessibility (Req 26)**: manual audit with TalkBack, contrast checks (4.5:1 / 3:1), and 48dp touch-target verification. Full WCAG validation requires manual testing with assistive technologies and expert review.

#### 1. Unit Testing

**Repository Layer Tests:**
```kotlin
class ExamRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ExamRepository
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExamRepository(database.examDao())
    }
    
    @Test
    fun `createExam should insert exam and return id`() = runTest {
        val examId = repository.createExam("Math Test", 50)
        
        val exam = repository.examDao.getExam(examId)
        assertNotNull(exam)
        assertEquals("Math Test", exam?.name)
        assertEquals(50, exam?.totalQuestions)
    }
    
    @Test
    fun `calculateScore should sum points for correct answers`() = runTest {
        // Setup exam with answer keys
        val examId = repository.createExam("Test", 5)
        val keys = listOf(
            AnswerKeyEntity(examId = examId, questionNumber = 1, correctAnswer = "A", points = 2),
            AnswerKeyEntity(examId = examId, questionNumber = 2, correctAnswer = "B", points = 3)
        )
        repository.saveAnswerKeys(keys)
        
        // Setup student with answers
        val studentId = repository.saveStudentResults(
            examId, "12345", "John Doe",
            listOf(1 to "A", 2 to "B", 3 to "C")
        )
        val student = repository.getStudent(studentId)!!
        
        // Calculate score
        val score = repository.calculateScore(student, keys)
        
        assertEquals(5, score)  // 2 + 3 correct
    }
}
```

**ViewModel Tests:**
```kotlin
class ExamViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var repository: ExamRepository
    private lateinit var viewModel: ExamViewModel
    
    @Before
    fun setup() {
        repository = mock()
        viewModel = ExamViewModel(repository)
    }
    
    @Test
    fun `loadExams should update state with exams`() = runTest {
        val exams = listOf(
            ExamWithStats(ExamEntity(1, "Test 1", 50), 10, 85.5f)
        )
        whenever(repository.getAllExamsWithStats()).thenReturn(flowOf(exams))
        
        viewModel.loadExams()
        advanceUntilIdle()
        
        val state = viewModel.examState.value
        assertEquals(exams, state.exams)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }
}
```

**Business Logic Tests:**
```kotlin
class AnalyticsEngineTest {
    private lateinit var repository: ExamRepository
    private lateinit var engine: AnalyticsEngine
    
    @Before
    fun setup() {
        repository = mock()
        engine = AnalyticsEngine(repository, mockMelcRepository)
    }
    
    @Test
    fun `calculateDifficulty should return percentage of correct answers`() = runTest {
        // Mock 10 students, 7 answered question 1 correctly
        val students = (1..10).map { StudentEntity(it.toLong(), "$it", "Student $it", 1L) }
        val correctAnswer = "A"
        
        whenever(repository.getStudents(1L)).thenReturn(flowOf(students))
        whenever(repository.getAnswerKeys(1L)).thenReturn(flowOf(
            listOf(AnswerKeyEntity(1, 1L, 1, correctAnswer))
        ))
        
        // 7 students answered A, 3 answered B
        students.take(7).forEach { student ->
            whenever(repository.getStudentAnswers(student.id)).thenReturn(
                listOf(StudentAnswerEntity(1, student.id, 1, "A"))
            )
        }
        students.drop(7).forEach { student ->
            whenever(repository.getStudentAnswers(student.id)).thenReturn(
                listOf(StudentAnswerEntity(1, student.id, 1, "B"))
            )
        }
        
        val difficulty = engine.calculateDifficulty(1L, 1)
        
        assertEquals(70.0f, difficulty, 0.01f)
    }
    
    @Test
    fun `calculateDiscriminationIndex should use upper and lower 27 percent`() = runTest {
        // Setup test data with 30 students
        // Upper 27% (8 students): 7/8 correct on question 1
        // Lower 27% (8 students): 2/8 correct on question 1
        // Expected: (7-2)/8 = 0.625
        
        val result = engine.calculateDiscriminationIndex(1L, 1)
        
        assertEquals(0.625f, result, 0.01f)
        assertEquals(AnalyticsEngine.DiscriminationQuality.EXCELLENT, 
                     engine.classifyDiscrimination(result))
    }
}
```


#### 2. Integration Testing

**Database Migration Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Test
    fun migrate5To6_addsNewTablesAndPreservesData() {
        // Create database at the current shipped version (5) and seed representative data.
        val db = helper.createDatabase(TEST_DB_NAME, 5).apply {
            execSQL("INSERT INTO subject_folders (id, name, settingsJson, createdAt, isDeleted) VALUES (1, 'Math', '{}', 1000, 0)")
            execSQL("INSERT INTO exams (id, subjectFolderId, name, totalQuestions, qrCode, gradingScale, passingGrade, useNegativeMarking, negativeMarkValue, allowLateScans, createdAt, isDeleted) VALUES (1, 1, 'Test Exam', 50, '', 'DEPED_K12', 75, 0, 0, 1, 1000, 0)")
            execSQL("INSERT INTO students (id, studentId, name, sectionId, examId, gradeLevel, contactInfo, photoPath, scannedAt) VALUES (1, '12345', 'John', 0, 1, '7', '', '', 1000)")
            close()
        }

        // Apply the additive 5 -> 6 migration.
        helper.runMigrationsAndValidate(TEST_DB_NAME, 6, true, MIGRATION_5_6)

        val migratedDb = helper.getMigrationDatabase()

        // Existing v5 data is preserved untouched.
        val examCursor = migratedDb.query("SELECT name FROM exams WHERE id = 1")
        assertTrue(examCursor.moveToFirst())
        assertEquals("Test Exam", examCursor.getString(0))
        val studentCursor = migratedDb.query("SELECT gradeLevel FROM students WHERE id = 1")
        assertTrue(studentCursor.moveToFirst())
        assertEquals("7", studentCursor.getString(0))  // gradeLevel is a String

        // New tables exist and are queryable.
        migratedDb.query("SELECT * FROM student_enrollments").use { assertNotNull(it) }
        migratedDb.query("SELECT * FROM melc_coverage").use { assertNotNull(it) }
        migratedDb.query("SELECT * FROM student_notes").use { assertNotNull(it) }
        migratedDb.query("SELECT * FROM sync_logs").use { assertNotNull(it) }
    }
}
```

**Scanner Integration Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class ScannerIntegrationTest {
    @Test
    fun scanAnswerSheet_withQRCode_shouldAutoDetectExam() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val qrGenerator = QRCodeGenerator()
        val qrParser = QRCodeParser()
        val processor = BubbleSheetProcessor(context)
        
        // Generate QR code
        val metadata = QRCodeGenerator.ExamMetadata(
            examId = 1L,
            examName = "Math Test",
            totalQuestions = 50,
            subjectId = 1L,
            createdAt = System.currentTimeMillis()
        )
        val qrBitmap = qrGenerator.generateQRCode(metadata)
        
        // Parse QR code
        val parsedMetadata = qrParser.parseQRCode(qrBitmap)
        
        assertNotNull(parsedMetadata)
        assertEquals(1L, parsedMetadata?.examId)
        assertEquals("Math Test", parsedMetadata?.examName)
        assertEquals(50, parsedMetadata?.totalQuestions)
    }
    
    @Test
    fun answerSheetRoundTrip_shouldPreserveData() = runTest {
        // Generate answer sheet PDF
        val printer = AnswerSheetPrettyPrinter(qrGenerator, context)
        val exam = ExamEntity(1L, "Test", 20)
        val metadata = QRCodeGenerator.ExamMetadata(...)
        val config = AnswerSheetPrettyPrinter.SheetConfig(
            examName = "Test",
            totalQuestions = 20,
            optionsCount = 4
        )
        
        val pdfPath = printer.generateAnswerSheet(exam, metadata, config)
        
        // Simulate filling and scanning
        // (In real test, would use image manipulation libraries)
        
        assertTrue(File(pdfPath).exists())
    }
}
```

#### 3. UI Testing

**Compose UI Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class ExamListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun examListScreen_displaysExams() {
        val exams = listOf(
            ExamWithStats(ExamEntity(1, "Math Test", 50), 10, 85.5f),
            ExamWithStats(ExamEntity(2, "Science Quiz", 30), 5, 92.0f)
        )
        
        composeTestRule.setContent {
            ExamListScreen(
                exams = exams,
                onExamClick = {},
                onNewExamClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Math Test").assertExists()
        composeTestRule.onNodeWithText("Science Quiz").assertExists()
        composeTestRule.onNodeWithText("85.5%").assertExists()
    }
    
    @Test
    fun newExamButton_navigatesToCreation() {
        var navigationCalled = false
        
        composeTestRule.setContent {
            ExamListScreen(
                exams = emptyList(),
                onExamClick = {},
                onNewExamClick = { navigationCalled = true }
            )
        }
        
        composeTestRule.onNodeWithText("NEW EXAM").performClick()
        assertTrue(navigationCalled)
    }
}
```

#### 4. Performance Testing

**Database Query Performance:**
```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceTest {
    @Test
    fun largeDatasetQuery_shouldCompleteQuickly() = runTest {
        // Insert 1000 students
        val students = (1..1000).map {
            StudentEntity(it.toLong(), "$it", "Student $it", 1L)
        }
        repository.insertStudents(students)
        
        // Measure query time
        val startTime = System.currentTimeMillis()
        val results = repository.getStudents(1L).first()
        val duration = System.currentTimeMillis() - startTime
        
        // Should complete within 500ms
        assertTrue(duration < 500)
        assertEquals(1000, results.size)
    }
    
    @Test
    fun pdfGeneration_shouldHandleLargeDataset() = runTest {
        // 100 students, 100 questions
        val reportGenerator = ReportGenerator(context, repository, melcRepository)
        
        val startTime = System.currentTimeMillis()
        val pdfPath = reportGenerator.generateReport(
            ReportGenerator.ReportType.ClassSummary(examId = 1L, sectionId = 1L),
            schoolName = "Test School"
        )
        val duration = System.currentTimeMillis() - startTime
        
        // Should complete within 10 seconds on low-end device
        assertTrue(duration < 10000)
        assertTrue(File(pdfPath).exists())
    }
}
```


## Performance Optimization

### Optimization Strategies for Low-End Devices

#### 1. Database Optimizations

**Indexing Strategy:**
```kotlin
// Critical indexes for frequently queried columns
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["examId"]),           // For getStudentsByExam queries
        Index(value = ["studentId"]),        // For lookups by school ID
        Index(value = ["isDeleted"]),        // For filtering active records
        Index(value = ["gradeLevel"])        // For filtering by grade
    ]
)

// Composite indexes for common query patterns
@Entity(
    tableName = "student_melc_mastery",
    indices = [
        Index(value = ["studentId", "melcId"]),  // Unique constraint + query optimization
        Index(value = ["melcId", "masteryLevel"]) // For mastery distribution queries
    ]
)
```

**Query Optimization:**
```kotlin
// Use pagination for large lists
@Query("SELECT * FROM exams WHERE subjectId = :subjectId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
suspend fun getExamsPaginated(subjectId: Long, limit: Int, offset: Int): List<ExamEntity>

// Use projections to reduce data transfer
@Query("SELECT id, name, totalQuestions FROM exams WHERE isDeleted = 0")
fun getExamsSummary(): Flow<List<ExamSummary>>

data class ExamSummary(val id: Long, val name: String, val totalQuestions: Int)

// Avoid N+1 queries with JOINs
@Query("""
    SELECT students.*, 
           COUNT(student_answers.id) as answerCount
    FROM students
    LEFT JOIN student_answers ON students.id = student_answers.studentEntityId
    WHERE students.examId = :examId
    GROUP BY students.id
""")
fun getStudentsWithAnswerCount(examId: Long): Flow<List<StudentWithAnswerCount>>
```

#### 2. Memory Management

**Bitmap Optimization:**
```kotlin
object BitmapHelper {
    /**
     * Downsamples large images to target resolution
     * Reduces memory usage from ~30MB to ~5MB per scan
     */
    fun loadOptimizedBitmap(uri: Uri, context: Context, targetWidth: Int = 1920, targetHeight: Int = 1080): Bitmap? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            
            // First decode with inJustDecodeBounds to get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input.close()
            
            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            
            // Decode with sample size
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565  // Use 2 bytes per pixel instead of 4
            
            val input2 = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input2, null, options)
            input2?.close()
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}
```

**Memory Cache:**
```kotlin
object MemoryCache {
    private val cache = LruCache<String, Any>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()  // Use 1/8 of available memory
    )
    
    fun <T> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return cache.get(key) as? T
    }
    
    fun <T> put(key: String, value: T) {
        cache.put(key, value)
    }
    
    fun evict(key: String) {
        cache.remove(key)
    }
    
    // Cache frequently accessed MELCs
    suspend fun getMelcsCached(subject: String, gradeLevel: Int, repository: MelcRepository): List<MelcEntity> {
        val key = "melcs_${subject}_$gradeLevel"
        return get(key) ?: run {
            val melcs = repository.getMelcsBySubject(subject, gradeLevel).first()
            put(key, melcs)
            melcs
        }
    }
}
```


#### 3. UI Performance

**Lazy Loading and Pagination:**
```kotlin
@Composable
fun ExamListScreen(viewModel: ExamViewModel) {
    val exams by viewModel.examsPaginated.collectAsState(initial = emptyList())
    
    LazyColumn {
        items(
            items = exams,
            key = { it.id }
        ) { exam ->
            ExamCard(exam)
        }
        
        // Load more when reaching end
        item {
            if (exams.isNotEmpty()) {
                LaunchedEffect(Unit) {
                    viewModel.loadMoreExams()
                }
            }
        }
    }
}

// ViewModel with pagination
class ExamViewModel {
    private val pageSize = 20
    private var currentOffset = 0
    
    private val _examsPaginated = MutableStateFlow<List<ExamWithStats>>(emptyList())
    val examsPaginated: StateFlow<List<ExamWithStats>> = _examsPaginated
    
    fun loadMoreExams() {
        viewModelScope.launch {
            val newExams = repository.getExamsPaginated(
                limit = pageSize,
                offset = currentOffset
            )
            _examsPaginated.value = _examsPaginated.value + newExams
            currentOffset += pageSize
        }
    }
}
```

**Composition Optimization:**
```kotlin
// Use derivedStateOf to avoid recomposition
@Composable
fun StudentListScreen(students: List<StudentEntity>) {
    val sortedStudents by remember {
        derivedStateOf {
            students.sortedByDescending { it.scannedAt }
        }
    }
    
    LazyColumn {
        items(sortedStudents) { student ->
            StudentCard(student)
        }
    }
}

// Use keys to maintain scroll position
@Composable
fun ExamDetailTabs(examId: Long) {
    var selectedTab by remember { mutableStateOf(0) }
    
    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
            Text("Scores")
        }
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
            Text("Item Analysis")
        }
    }
    
    when (selectedTab) {
        0 -> ScoresTab(examId)  // Will maintain state when switching back
        1 -> ItemAnalysisTab(examId)
    }
}
```

#### 4. Background Processing

**Coroutine Optimization:**
```kotlin
// Use appropriate dispatchers
class ExamRepository {
    // IO operations
    suspend fun loadExam(id: Long) = withContext(Dispatchers.IO) {
        examDao.getExam(id)
    }
    
    // CPU-intensive operations
    suspend fun calculateAnalytics(examId: Long) = withContext(Dispatchers.Default) {
        val students = getStudents(examId).first()
        val keys = getAnswerKeys(examId).first()
        
        // Parallel processing for large datasets
        students.map { student ->
            async {
                calculateScore(student, keys)
            }
        }.awaitAll()
    }
}

// Batch database operations
suspend fun importStudentsBatch(students: List<StudentEntity>) {
    database.withTransaction {
        students.chunked(100).forEach { batch ->
            examDao.insertStudents(batch)
        }
    }
}
```

**Worker for Long-Running Tasks (Recycle-Bin Purge, Req 10.6):**
```kotlin
class RecycleBinPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val db = (applicationContext as ExamScannerApplication).database
            db.recycleBinDao().purgeExpired(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

// Schedule periodic purge (no network required — the app is offline-only)
fun schedulePeriodicPurge(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()
    
    val purgeRequest = PeriodicWorkRequestBuilder<RecycleBinPurgeWorker>(
        repeatInterval = 6,
        repeatIntervalTimeUnit = TimeUnit.HOURS
    )
        .setConstraints(constraints)
        .build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "recycle_bin_purge_work",
        ExistingPeriodicWorkPolicy.KEEP,
        purgeRequest
    )
}
```

#### 5. PDF Generation Optimization

**Incremental PDF Rendering:**
```kotlin
class OptimizedPDFGenerator {
    /**
     * Generates PDF in chunks to avoid memory spikes
     * Writes to file incrementally instead of building entire PDF in memory
     */
    suspend fun generateLargeReport(data: ReportData): String = withContext(Dispatchers.IO) {
        val outputFile = createTempFile("report", ".pdf")
        
        PdfWriter(outputFile).use { writer ->
            PdfDocument(writer).apply {
                addEventHandler(PdfDocumentEvent.END_PAGE) {
                    // Force write to disk after each page
                    writer.flush()
                }
            }.use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { document ->
                    // Add content page by page
                    data.sections.chunked(10).forEach { sectionChunk ->
                        addSectionChunk(document, sectionChunk)
                        // Allow GC between chunks
                        delay(10)
                    }
                }
            }
        }
        
        outputFile.absolutePath
    }
}
```


## Dependencies and Libraries

> **Version strategy (conservative track):** The pinned versions below are the latest stable releases verified compatible with `compileSdk 34` and `minSdk 26` (as of Sept 2026). We intentionally stay on the pre-1.12 Compose channel, Room 2.x, AGP 8.x, and iText 7.2.x. The larger migration to compileSdk 37 / AGP 9.1.2+ / Compose 1.12 / Room 3.0 (all of which introduce breaking changes) is deferred to a future effort. Where no newer SDK-34-compatible release exists (e.g. `security-crypto`, `sqlcipher`, `MPAndroidChart`, `biometric`), the current artifact is kept and noted inline.

### Build Configuration Updates

```gradle
// app/build.gradle

android {
    namespace 'com.examscanner.premium'
    compileSdk 34

    defaultConfig {
        // NOTE: the shipped app id / package is com.examscanner.premium. Do NOT rename it to
        // com.offlineassessment.premium — a package rename breaks Play Store identity, signing,
        // and existing installs. Any rebrand is user-facing (app label / store listing) only.
        applicationId "com.examscanner.premium"
        minSdk 26
        targetSdk 34
        versionCode 2  // increment as appropriate for the next release
        versionName "2.0"
    }
    
    buildFeatures {
        compose true
        buildConfig true  // For BuildConfig.VERSION_NAME
    }
}

composeOptions {
    // Matches Kotlin 1.9.24 (Compose Compiler 1.5.14)
    kotlinCompilerExtensionVersion '1.5.14'
}

dependencies {
    // Core AndroidX (latest stable on SDK-34 / pre-1.12 track)
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-process:2.8.7'
    implementation 'androidx.activity:activity-compose:1.9.3'
    // Compose BOM 2024.09.00: last line staying comfortably on the pre-1.12 stable channel
    implementation platform('androidx.compose:compose-bom:2024.09.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.material:material-icons-extended'
    implementation 'androidx.navigation:navigation-compose:2.8.5'
    
    // Room Database (stay on 2.x; do NOT move to Room 3.0)
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // CameraX (latest stable, SDK-34 compatible)
    implementation 'androidx.camera:camera-camera2:1.4.1'
    implementation 'androidx.camera:camera-lifecycle:1.4.1'
    implementation 'androidx.camera:camera-view:1.4.1'
    
    // ML Kit Text Recognition
    implementation 'com.google.mlkit:text-recognition:16.0.1'
    
    // ML Kit Barcode Scanning for QR codes
    implementation 'com.google.mlkit:barcode-scanning:17.3.0'
    
    // ZXing for QR code generation
    implementation 'com.google.zxing:core:3.5.3'
    implementation 'com.google.zxing:android-core:3.3.0'
    
    // iText7 for PDF generation (keep 7.2.x line; do NOT jump to iText 8/9 — changes licensing/packages)
    implementation 'com.itextpdf:itext7-core:7.2.6'
    implementation 'com.itextpdf:layout:7.2.6'
    
    // Apache POI for Excel import/export
    implementation 'org.apache.poi:poi:5.3.0'
    implementation 'org.apache.poi:poi-ooxml:5.3.0'
    
    // Firebase Authentication (auth gate + subscription identity). No Firestore/Storage sync.
    implementation platform('com.google.firebase:firebase-bom:33.7.0')
    implementation 'com.google.firebase:firebase-auth-ktx'
    
    // Google Play Billing for subscriptions
    implementation 'com.android.billingclient:billing-ktx:7.1.1'
    
    // WorkManager for background maintenance (e.g., recycle-bin purge)
    implementation 'androidx.work:work-runtime-ktx:2.10.0'
    
    // Kotlinx Serialization for JSON
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3'
    // Coroutines interop for Play Services (Tasks -> suspend)
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1'
    
    // Accompanist (last stable before deprecation; SDK-34 compatible)
    implementation 'com.google.accompanist:accompanist-systemuicontroller:0.36.0'
    implementation 'com.google.accompanist:accompanist-permissions:0.36.0'
    
    // Charts library for analytics visualization (keep — no newer stable release)
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // SQLCipher for database encryption
    // Keep net.zetetic:android-database-sqlcipher:4.5.4 — the newer
    // net.zetetic:sqlcipher-android:4.6.1 changes the artifact/package and would
    // require an import migration, deferred under the conservative track.
    implementation 'net.zetetic:android-database-sqlcipher:4.5.4'
    implementation 'androidx.sqlite:sqlite-ktx:2.4.0'

    // Security Crypto for EncryptedSharedPreferences (DB passphrase storage)
    // Keep 1.1.0-alpha06 — no stable release of security-crypto exists yet.
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // AppCompat for per-app locales (runtime language switching, Req 20)
    implementation 'androidx.appcompat:appcompat:1.7.0'

    // Biometric authentication for student data access (Req 25.2)
    // Keep 1.1.0 — latest stable release.
    implementation 'androidx.biometric:biometric:1.1.0'
    
    // Coil image loading
    implementation 'io.coil-kt:coil-compose:2.7.0'

    // DataStore for preferences
    implementation 'androidx.datastore:datastore-preferences:1.1.1'
    
    // Testing (EXISTING + additions)
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.14.2'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.4.0'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
    // jqwik for property-based testing (Correctness Properties)
    testImplementation 'net.jqwik:jqwik:1.9.1'
    
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
    androidTestImplementation 'androidx.room:room-testing:2.6.1'
    
    debugImplementation 'androidx.compose.ui:ui-tooling'
    debugImplementation 'androidx.compose.ui:ui-test-manifest'
}

// NEW: Apply Firebase and Kotlin Serialization plugins
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'com.google.gms.google-services'  // NEW: Firebase
    id 'kotlinx-serialization'           // NEW: Serialization
}
```

### Project-level build.gradle

```gradle
// project-level build.gradle

buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.2'  // Firebase
    }
}

plugins {
    // AGP 8.7.3: latest 8.x, compatible with compileSdk 34 (do NOT use AGP 9.x)
    id 'com.android.application' version '8.7.3' apply false
    // Kotlin 1.9.24: matches Compose Compiler extension 1.5.14
    id 'org.jetbrains.kotlin.android' version '1.9.24' apply false
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.9.24' apply false
}
```


## Deployment Strategy

### Release Management

#### Version 2.0 Release Plan

**Phase 1: Internal Testing (2 weeks)**
- Deploy to internal test devices
- Test all 30 requirements
- Performance profiling on low-end devices (2GB RAM)
- Database migration validation for the additive `MIGRATION_5_6` (from the shipped v5 schema)
- Load testing with 1000+ students

**Phase 2: Closed Beta (4 weeks)**
- Distribute via Google Play Internal Testing Track
- Recruit 50-100 Filipino teachers
- Collect feedback on UX and feature completeness
- Monitor crash reports via Firebase Crashlytics
- Iterate on critical issues

**Phase 3: Open Beta (4 weeks)**
- Move to Google Play Beta Track
- Expand to 500-1000 users
- Test subscription flow end-to-end
- Validate Backup/Restore reliability
- Monitor billing integration

**Phase 4: Production Release**
- Publish to Google Play Store
- Implement staged rollout: 10% → 25% → 50% → 100%
- Monitor key metrics: crash rate, subscription conversion, DAU
- Prepare hotfix pipeline for critical issues

### App Store Optimization

**Play Store Listing:**
```
Title: Offline Assessment - Teacher's Testing Toolkit

Short Description (80 chars):
Complete assessment platform for Filipino teachers with MELCs tracking & analytics

Full Description:
Transform your teaching with Offline Assessment - the complete exam management 
system designed for Filipino K-12 educators.

✨ KEY FEATURES:
• DepEd MELCs Integration - Track competency-based learning outcomes
• QR-Coded Answer Sheets - Fast, accurate scanning
• Professional PDF Reports - Individual, class, and school-level
• Advanced Analytics - Difficulty index, discrimination index, learning gaps
• Curriculum Tracking - Monitor MELCs coverage by quarter
• Offline-First - Works without internet
• Cloud Backup - Optional sync across devices

📊 POWERFUL ANALYTICS:
• Item analysis with discrimination index
• Learning gap identification
• Student competency mastery tracking
• Performance trends and insights

🎓 DESIGNED FOR TEACHERS:
• Subject folder organization
• Section and class management
• Comprehensive student profiles
• CSV/Excel import/export
• Recycle bin with 30-day retention

💎 FREE & PREMIUM:
Free: 3 subjects, 5 exams per subject, 30 scans per exam
Premium (₱100/month): Unlimited everything + advanced features

Built by teachers, for teachers. Join thousands of Filipino educators 
already using Offline Assessment.

Keywords: teacher, assessment, exam scanner, DepEd, MELCs, grading, 
         analytics, Philippines, K-12, education
```

### Monitoring and Analytics

```kotlin
// Firebase Analytics Integration
class AnalyticsTracker(private val firebaseAnalytics: FirebaseAnalytics) {
    fun trackExamCreated(subjectId: Long, questionCount: Int) {
        firebaseAnalytics.logEvent("exam_created") {
            param("subject_id", subjectId)
            param("question_count", questionCount.toLong())
        }
    }
    
    fun trackScanCompleted(examId: Long, processingTime: Long) {
        firebaseAnalytics.logEvent("scan_completed") {
            param("exam_id", examId)
            param("processing_time_ms", processingTime)
        }
    }
    
    fun trackSubscriptionPurchased(tier: String, price: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE) {
            param(FirebaseAnalytics.Param.ITEM_NAME, "premium_subscription")
            param(FirebaseAnalytics.Param.VALUE, 100.0)  // ₱100
            param(FirebaseAnalytics.Param.CURRENCY, "PHP")
        }
    }
    
    fun trackFeatureUsage(featureName: String, isPremiumFeature: Boolean) {
        firebaseAnalytics.logEvent("feature_used") {
            param("feature_name", featureName)
            param("is_premium", if (isPremiumFeature) "yes" else "no")
        }
    }
}

// Crashlytics Integration
class CrashReporter(private val crashlytics: FirebaseCrashlytics) {
    fun log(message: String) {
        crashlytics.log(message)
    }
    
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }
    
    fun recordException(exception: Exception) {
        crashlytics.recordException(exception)
    }
    
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
```


## Implementation Roadmap

### Development Phases

> **Reconciliation note:** This roadmap was written against the original "start from v1" assumption. In reality, the database foundation (v5, encrypted, 11 entities, migrations 1→5) and much of Subject/Section/Exam/Roster/Scanner/UI work has **already shipped**. Treat Phases 1-2 as **mostly complete**; the genuinely remaining foundation task is the additive `MIGRATION_5_6` plus wiring the four new entities. The later phases (QR, analytics, mastery, reports, pacing, sync, billing, i18n) remain valid as **net-new** feature work that integrates with the existing `ExamRepository`/`ExamDao`/screens.

#### Phase 1: Foundation (mostly SHIPPED — remaining: register 4 new tables)
**Goal: Finish the additive schema step; the rest of the foundation already exists**

Already done: encrypted v5 `AppDatabase`, migrations 1→5, 11 registered entities, single `ExamDao`, single `ExamRepository`, MELC seeding via `initializeSampleMelcs()`, built-in grading scales/templates seeding.

Remaining tasks:
1. Add `MIGRATION_5_6` and register `StudentEnrollmentEntity`, `MelcCoverageEntity`, `StudentNoteEntity`, `SyncLogEntity`; bump `version` to 6; set `exportSchema = true`.
2. Add DAO methods for the four new tables (extend `ExamDao` or add a small DAO).
3. Write the `MigrationTest` for 5→6 (data preserved, new tables created) — see Testing Strategy.
4. Expand the seeded MELC dataset toward full subject × grade 1-12 × quarter 1-4 coverage (Req 2.1), keeping `MelcEntity.gradeLevel: String`.

#### Phase 2: Subject & Section Management (largely SHIPPED)
**Goal: Fill gaps on top of the existing three-tier hierarchy**

Already done (in `ExamRepository` + existing screens `SubjectFolderListScreen`, `SectionManagementScreen`, `StudentRosterScreen`): subject-folder CRUD (soft delete), section CRUD, roster add/bulk-insert, post-scan auto-organization, CSV import via existing `CSVImportUtility`.

Remaining tasks:
1. Wire enrollment reads/writes through the newly registered `StudentEnrollmentEntity` (multi-section membership).
2. Add subject restore/permanent-delete symmetry and `SubjectStats`/`SectionStats` aggregations.
3. Add CSV roster **export** (import already exists).
4. Optionally split subject/section ViewModel concerns out of `ExamViewModel` (not required).

Deliverables:
- Subject folder creation and management
- Section CRUD operations
- Student enrollment system
- CSV import working

#### Phase 3: QR Code System (Weeks 5-6)
**Goal: QR-coded answer sheet generation and parsing**

Tasks:
1. Integrate ZXing library for QR generation
2. Implement QRCodeGenerator service
3. Integrate ML Kit Barcode Scanning
4. Implement QRCodeParser service
5. Create AnswerSheetPrettyPrinter with iText7
6. Build answer sheet preview screen
7. Test round-trip: generate → print → scan → parse

Deliverables:
- QR code generation working
- QR code parsing with ML Kit
- Printable PDF answer sheets
- Round-trip validation passing

#### Phase 4: MELCs Integration (Weeks 7-8)
**Goal: Competency mapping and mastery calculation**

Tasks:
1. Create MELC browser screen with filtering
2. Implement question-to-MELC mapping UI
3. Build MasteryCalculator service
4. Implement StudentMelcMastery updates
5. Create competency mastery visualization
6. Build student profile with mastery matrix
7. Test mastery calculations across multiple exams

Deliverables:
- MELC browser functional
- Question mapping working
- Mastery calculation accurate
- Student profiles showing competency levels

#### Phase 5: Analytics Engine (Weeks 9-10)
**Goal: Advanced statistical analysis**

Tasks:
1. Implement AnalyticsEngine service
2. Calculate difficulty index
3. Calculate discrimination index (upper/lower 27%)
4. Implement learning gap detection
5. Create item response curve visualization
6. Build analytics dashboard screen
7. Integrate MPAndroidChart for visualizations
8. Test with large datasets (100+ students)

Deliverables:
- All analytics calculations working
- Learning gaps identified correctly
- Visual charts rendering
- Performance acceptable on low-end devices

#### Phase 6: PDF Report Generation (Weeks 11-12)
**Goal: Professional multi-level reports**

Tasks:
1. Implement ReportGenerator service
2. Build individual student report template
3. Build class summary report template
4. Build school-level report template
5. Integrate iText7 for PDF creation
6. Implement incremental rendering for performance
7. Add school logo and branding support
8. Test report generation with large datasets

Deliverables:
- 3 report types functional
- PDFs properly formatted
- Generation completes within 10 seconds
- Reports saved to Downloads folder

#### Phase 7: Pacing & Curriculum Tracking (Weeks 13-14)
**Goal: DepEd curriculum coverage monitoring**

Tasks:
1. Implement PacingEngine service
2. Create curriculum tracking dashboard
3. Build quarterly pacing guide view
4. Implement manual MELC coverage marking
5. Create behind-schedule alerts
6. Build quarterly summary reports
7. Test across all 4 quarters

Deliverables:
- Pacing guide calculating correctly
- Coverage percentage accurate
- Behind-schedule detection working
- Quarterly summaries generating

#### Phase 8: User-Owned Backup and Restore (Weeks 15-16)
**Goal: On-device, user-controlled data portability (no server component)**

> De-scoped: cloud sync was removed. The app is offline-only; data portability is provided
> entirely through the existing on-device Backup/Restore (`BackupManager`).

Tasks:
1. Export the complete encrypted database to a backup file in device storage
2. Let the user choose where to move/store the backup file
3. Validate a selected backup file before restoring
4. Restore from a validated backup file and reload data
5. Surface Backup/Restore in the Backup Management settings screen (all tiers)

Deliverables:
- Backup/Restore working entirely on-device
- Backup file validation on restore
- Backup/Restore available regardless of subscription tier

#### Phase 9: Subscription & Billing (Weeks 17-18)
**Goal: Freemium monetization**

Tasks:
1. Set up Google Play Console billing
2. Create subscription product (₱100/month)
3. Implement SubscriptionManager service
4. Integrate Google Play Billing Library
5. Build subscription management screen
6. Implement limit checks throughout app
7. Create upgrade prompts for free users
8. Test subscription purchase flow
9. Test subscription status verification
10. Handle subscription expiration

Deliverables:
- Subscription purchase working
- Limits enforced correctly
- Upgrade prompts showing appropriately
- Billing tested on real devices

#### Phase 10: UI/UX Polish & Internationalization (Weeks 19-20)
**Goal: Premium experience and Filipino support**

Tasks:
1. Maintain existing glassmorphism design
2. Create new screens matching design system
3. Add loading states and skeleton screens
4. Implement error states and empty states
5. Add string resources for English/Filipino
6. Create language switcher in settings
7. Test all screens in both languages
8. Add haptic feedback
9. Optimize animations for low-end devices
10. Accessibility audit (TalkBack, contrast, touch targets)

Deliverables:
- All screens match design system
- Filipino translation complete
- Loading/error states polished
- Accessibility compliant

#### Phase 11: Testing & Quality Assurance (Weeks 21-22)
**Goal: Production-ready quality**

Tasks:
1. Write unit tests for all repositories
2. Write unit tests for all business logic services
3. Write ViewModel tests
4. Write database migration tests
5. Write integration tests (scanner, sync, reports)
6. Write UI tests for critical flows
7. Performance testing on low-end devices
8. Load testing with 1000+ students
9. Battery usage profiling
10. Memory leak detection

Deliverables:
- 80%+ code coverage
- All tests passing
- Performance benchmarks met
- No memory leaks

#### Phase 12: Beta Release & Iteration (Weeks 23-26)
**Goal: User validation and refinement**

Tasks:
1. Deploy to Google Play Internal Testing
2. Recruit 50-100 Filipino teachers
3. Collect feedback via in-app surveys
4. Monitor Firebase Crashlytics
5. Analyze usage patterns via Firebase Analytics
6. Fix critical bugs
7. Implement high-priority feature requests
8. Optimize based on real-world usage
9. Update documentation
10. Prepare marketing materials

Deliverables:
- Beta version stable
- Critical bugs fixed
- User feedback incorporated
- Crash rate <1%

#### Phase 13: Production Launch (Week 27)
**Goal: Public release**

Tasks:
1. Final QA pass
2. Update Play Store listing
3. Create promotional graphics
4. Submit to Google Play for review
5. Implement staged rollout (10% → 100%)
6. Monitor metrics closely
7. Prepare hotfix pipeline
8. Respond to user reviews
9. Plan v2.1 feature roadmap

Deliverables:
- App live on Google Play Store
- Staged rollout complete
- Monitoring active
- Support channels ready


## Security Considerations

### Data Protection

#### 1. Local Data Security

**SQLCipher Encryption:**
```kotlin
// NOTE: This is a conceptual illustration. The SHIPPED implementation already lives in
// AppDatabase.getDatabase(context): it builds the encrypted DB named "exam_scanner_database"
// using net.sqlcipher SupportFactory + EncryptionKeyManager.getDatabasePassphrase(context).
// Do NOT introduce a second builder or a different DB name ("offline_assessment_database"
// below is illustrative only) — reuse the existing AppDatabase.getDatabase path.
object DatabaseEncryption {
    fun getEncryptedDatabase(context: Context): AppDatabase {
        // Delegates to the shipped, already-encrypted builder.
        return AppDatabase.getDatabase(context)
    }
    
    private fun getOrCreatePassphrase(context: Context): String {
        // Preferred: wrap the passphrase with an Android Keystore key and store the
        // encrypted blob in EncryptedSharedPreferences (see security-standards).
        // The passphrase itself is never persisted in plain SharedPreferences.
        val prefs = EncryptedSharedPreferences.create(
            context,
            "security",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        var passphrase = prefs.getString("db_passphrase", null)

        if (passphrase == null) {
            // Generate cryptographically secure passphrase
            passphrase = generateSecurePassphrase()
            prefs.edit().putString("db_passphrase", passphrase).apply()
        }

        return passphrase
    }
    
    private fun generateSecurePassphrase(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
```

#### 2. Backup File Security

**Backup File Encryption (on-device only):**
```kotlin
class SecureBackupCrypto {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    
    /**
     * Encrypts the backup payload before it is written to a user-controlled backup file.
     * Uses Android Keystore for key management. Data never leaves the device except in the
     * backup file the user chooses to move.
     */
    fun encryptData(data: ByteArray): EncryptedData {
        val key = getOrCreateEncryptionKey()
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        
        return EncryptedData(encrypted, iv)
    }
    
    private fun getOrCreateEncryptionKey(): SecretKey {
        if (!keyStore.containsAlias("data_encryption_key")) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            
            val keyGenSpec = KeyGenParameterSpec.Builder(
                "data_encryption_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            
            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
        
        return (keyStore.getEntry("data_encryption_key", null) as KeyStore.SecretKeyEntry).secretKey
    }
    
    data class EncryptedData(val ciphertext: ByteArray, val iv: ByteArray)
}
```

#### 3. Authentication Security

**Biometric Authentication:**
```kotlin
class BiometricAuthHelper(private val context: Context) {
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                
                override fun onAuthenticationFailed() {
                    onError("Authentication failed")
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to access student data")
            .setSubtitle("Use your fingerprint or face")
            .setNegativeButtonText("Cancel")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
               BiometricManager.BIOMETRIC_SUCCESS
    }
}
```

#### 4. Input Validation

**Comprehensive Validation:**
```kotlin
object ValidationEngine {
    fun validateExamName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Exam name cannot be empty")
            name.length > 200 -> ValidationResult.Invalid("Exam name too long (max 200 characters)")
            name.contains(Regex("[<>:\"/\\\\|?*]")) -> ValidationResult.Invalid("Exam name contains invalid characters")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateStudentId(id: String): ValidationResult {
        return when {
            id.isBlank() -> ValidationResult.Invalid("Student ID cannot be empty")
            id.length > 20 -> ValidationResult.Invalid("Student ID too long (max 20 characters)")
            !id.matches(Regex("^[a-zA-Z0-9-]+$")) -> ValidationResult.Invalid("Student ID can only contain letters, numbers, and hyphens")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateQuestionCount(count: Int): ValidationResult {
        return when {
            count < 1 -> ValidationResult.Invalid("Question count must be at least 1")
            count > 200 -> ValidationResult.Invalid("Question count cannot exceed 200")
            else -> ValidationResult.Valid
        }
    }

    // Req 7.2: section name is required and capped at 50 characters
    fun validateSectionName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Section name cannot be empty")
            name.length > 50 -> ValidationResult.Invalid("Section name too long (max 50 characters)")
            else -> ValidationResult.Valid
        }
    }

    // Req 8.5: profile notes are capped at 500 characters per note
    fun validateNote(note: String): ValidationResult {
        return when {
            note.length > 500 -> ValidationResult.Invalid("Note too long (max 500 characters)")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateFileUpload(file: File, maxSize: Long = 10 * 1024 * 1024): ValidationResult {
        return when {
            !file.exists() -> ValidationResult.Invalid("File does not exist")
            file.length() > maxSize -> ValidationResult.Invalid("File size exceeds ${maxSize / 1024 / 1024}MB limit")
            !isValidFileType(file) -> ValidationResult.Invalid("Invalid file type")
            else -> ValidationResult.Valid
        }
    }
    
    private fun isValidFileType(file: File): Boolean {
        val allowedExtensions = listOf("pdf", "docx", "jpg", "jpeg", "png")
        val extension = file.extension.lowercase()
        return extension in allowedExtensions
    }
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }
}
```

### Privacy Compliance

**Data Minimization:**
- Collect only necessary student information
- Optional fields for contact info and photos
- No personal data sent to third parties; the app is offline-only and has no data-sync backend
- Data portability is user-controlled via on-device Backup/Restore

**User Rights:**
```kotlin
class PrivacyManager(private val repository: ExamRepository) {
    /**
     * Permanently deletes all student data (GDPR Right to Erasure)
     */
    suspend fun deleteAllStudentData(studentId: Long) {
        repository.permanentlyDeleteStudent(studentId)
        // Data is local-only; nothing to remove from any remote store.
    }
    
    /**
     * Exports all student data for portability (GDPR Right to Data Portability)
     */
    suspend fun exportStudentData(studentId: Long): File {
        val student = repository.getStudent(studentId)
        val exams = repository.getExamsForStudent(studentId)
        val mastery = melcRepository.getStudentMastery(studentId)
        
        val json = Json.encodeToString(
            StudentDataExport(
                student = student,
                exams = exams,
                mastery = mastery
            )
        )
        
        val file = File(context.filesDir, "student_${studentId}_export.json")
        file.writeText(json)
        return file
    }
    
    @Serializable
    data class StudentDataExport(
        val student: StudentEntity,
        val exams: List<ExamRecord>,
        val mastery: List<StudentMelcMasteryEntity>
    )
}
```


## Risk Assessment and Mitigation

### Technical Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| **Database Migration Failure** | Medium | High | - Comprehensive migration tests<br>- Backup before migration<br>- Rollback mechanism<br>- Gradual rollout (10% → 100%) |
| **Performance Issues on Low-End Devices** | Medium | High | - Extensive testing on 2GB RAM devices<br>- Image downsampling<br>- Database query optimization<br>- Pagination and lazy loading<br>- Memory profiling |
| **QR Code Detection Failures** | Medium | Medium | - Fallback to manual exam selection<br>- High error correction in QR codes<br>- Clear instructions for proper lighting<br>- ML Kit alternative barcode formats |
| **PDF Generation Memory Crashes** | Low | High | - Incremental rendering<br>- Streaming to file<br>- Memory limit monitoring<br>- Chunked processing |
| **Backup/Restore File Corruption** | Low | High | - Validate backup file integrity before restore<br>- Encrypt backup payload<br>- Clear error messaging and safe abort on invalid file |
| **Billing Integration Issues** | Low | High | - Extensive testing in sandbox<br>- Server-side verification (future)<br>- Clear error messages<br>- Support contact readily available |
| **MELCs Database Size** | Low | Medium | - Compress JSON asset (gzip)<br>- Load incrementally on demand<br>- Cache frequently accessed MELCs<br>- Database indexing |

### Business Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| **Low Subscription Conversion** | Medium | High | - Clear value proposition<br>- Generous free tier for evaluation<br>- In-app upgrade prompts at key moments<br>- Free trial period (future) |
| **User Resistance to Change** | Medium | Medium | - Preserve all v1 functionality<br>- Gradual feature introduction<br>- Comprehensive onboarding<br>- Tutorial videos |
| **Competition from Free Alternatives** | High | Medium | - Focus on DepEd MELCs integration<br>- Superior offline experience<br>- Advanced analytics<br>- Professional reports |
| **Regulatory Changes (DepEd Policy)** | Low | High | - Modular MELCs database design<br>- Easy updates via app update<br>- Monitor DepEd announcements<br>- Flexible curriculum tracking |

### Operational Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| **Firebase Auth Cost/Availability** | Low | Low | - Auth-only usage (no data sync)<br>- Monitor usage dashboards<br>- Graceful handling when auth is unavailable |
| **Support Burden** | Medium | Medium | - Comprehensive in-app help<br>- FAQ section<br>- Video tutorials<br>- Community forum (future)<br>- Clear error messages |
| **Data Loss** | Low | Critical | - User-owned Backup/Restore<br>- Encrypted local backup files<br>- Export functionality<br>- Database corruption recovery |

## Accessibility Features

### WCAG 2.1 Compliance

**Level AA Requirements:**

1. **Perceivable**
   - All images have content descriptions for TalkBack
   - Color contrast ratios meet 4.5:1 for normal text, 3:1 for large text
   - Text can scale up to 200% without loss of functionality
   - Charts include text alternatives for data

2. **Operable**
   - All functionality available from keyboard (external keyboard support)
   - Touch targets minimum 48dp × 48dp
   - No time limits on user actions
   - Clear focus indicators
   - Gesture alternatives (swipe = buttons available)

3. **Understandable**
   - Consistent navigation across screens
   - Clear error messages with suggestions
   - Labels and instructions provided for all inputs
   - Language can be changed (English/Filipino)

4. **Robust**
   - Works with TalkBack screen reader
   - Semantic markup in Compose (contentDescription, Role)
   - Compatible with assistive technologies

**Implementation:**
```kotlin
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    contentDescription: String = text,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge  // Scales with system font size
        )
    }
}

@Composable
fun AccessibleChart(
    data: List<DataPoint>,
    contentDescription: String
) {
    Column {
        // Visual chart
        Chart(data = data)
        
        // Text alternative for screen readers
        Text(
            text = contentDescription,
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            }
        )
        
        // Data table alternative
        if (shouldShowDataTable()) {
            DataTable(data = data)
        }
    }
}
```


## Appendix

### A. File Structure

> **Reconciliation notes for this appendix (the tree below is the aspirational target, not current reality):**
> - The real package root is **`com.examscanner.premium`**, not `com.offlineassessment.premium`.
> - Entities are currently defined **inline in `data/AppDatabase.kt`**, not in a `data/entities/` folder — except the four net-new files (`StudentEnrollmentEntity.kt`, `MelcCoverageEntity.kt`, `StudentNoteEntity.kt`, `SyncLogEntity.kt`) which **already exist directly under `data/`**.
> - There is a **single `ExamDao`** and a **single `ExamRepository`** today. The separate `dao/*` and `repository/*` files below are conceptual groupings; splitting them is optional and not required to finish the work.
> - Existing `data/` files not shown below but present today: `BuiltInData.kt`, `SampleMelcsData.kt`, `OnboardingPreferences.kt`.
> - Many screens marked "(NEW)" below **already exist** under `ui/screens/` (e.g. `RecycleBinScreen`, `SectionManagementScreen`, `StudentRosterScreen`, `SubjectFolderListScreen`, `SettingsScreen`, `TemplateGeneratorScreen`, `SmartDashboardMVP`, onboarding/auth screens). Verify against `ui/screens/` before creating a new file. Existing utilities also already cover much of the "domain/util" tree: `utils/` contains `BackupManager`, `CSVImportUtility`, `CSVAnswerKeyImport`, `ExportUtility`, `TemplatePDFGenerator`, `AnalyticsExporter`, `EncryptionKeyManager`, `SecureLogger`, `InputSanitizer`, `PasswordValidator`, `RootDetector`, `TrialGuard`; `analytics/` contains `AnalyticsTracker`, `ReportingService`, `SessionTracker`; `auth/` contains `AuthRepository`, `AuthState`, `AuthViewModel`.
> - `ExamEntity`, `AnswerKeyEntity`, `StudentEntity`, `StudentAnswerEntity` are **not** the original v1 shapes — they are the expanded v5 shapes documented in Data Models. `StudentEntity` is not "(MODIFIED, pending)"; its expansion already shipped.

```
app/src/main/java/com/examscanner/premium/   (actual package root)
# ---- aspirational layout below; see notes above for what already exists ----
com/offlineassessment/premium/
├── MainActivity.kt
├── OfflineAssessmentApplication.kt
│
├── data/
│   ├── entities/
│   │   ├── ExamEntity.kt (EXISTING)
│   │   ├── AnswerKeyEntity.kt (EXISTING)
│   │   ├── StudentEntity.kt (MODIFIED)
│   │   ├── StudentAnswerEntity.kt (EXISTING)
│   │   ├── SubjectFolderEntity.kt (NEW)
│   │   ├── SectionEntity.kt (NEW)
│   │   ├── StudentEnrollmentEntity.kt (NEW)
│   │   ├── MelcEntity.kt (NEW)
│   │   ├── QuestionMelcMappingEntity.kt (NEW)
│   │   ├── StudentMelcMasteryEntity.kt (NEW)
│   │   ├── TemplateEntity.kt (NEW)
│   │   ├── MelcCoverageEntity.kt (NEW)
│   │   ├── StudentNoteEntity.kt (NEW)
│   │   └── SyncLogEntity.kt (NEW)
│   │
│   ├── dao/
│   │   ├── ExamDao.kt (EXPANDED)
│   │   ├── SubjectDao.kt (NEW)
│   │   ├── SectionDao.kt (NEW)
│   │   ├── MelcDao.kt (NEW)
│   │   ├── TemplateDao.kt (NEW)
│   │   └── SyncDao.kt (NEW)
│   │
│   ├── repository/
│   │   ├── ExamRepository.kt (EXPANDED)
│   │   ├── SubjectRepository.kt (NEW)
│   │   ├── StudentRepository.kt (NEW)
│   │   ├── MelcRepository.kt (NEW)
│   │   ├── SectionRepository.kt (NEW)
│   │   ├── TemplateRepository.kt (NEW)
│   │   └── SyncRepository.kt (NEW)
│   │
│   ├── AppDatabase.kt (v2 with migration)
│   └── Converters.kt
│
├── domain/
│   ├── analytics/
│   │   ├── AnalyticsEngine.kt
│   │   ├── MasteryCalculator.kt
│   │   └── PacingEngine.kt
│   │
│   ├── qrcode/
│   │   ├── QRCodeGenerator.kt
│   │   └── QRCodeParser.kt
│   │
│   ├── pdf/
│   │   ├── AnswerSheetPrettyPrinter.kt
│   │   └── ReportGenerator.kt
│   │
│   ├── parser/
│   │   ├── AnswerSheetParser.kt (NEW)
│   │   └── BubbleDetectionEngine.kt (NEW)
│   │
│   ├── billing/
│   │   └── SubscriptionManager.kt
│   │
│   ├── importexport/
│   │   └── ImportExportService.kt (CSV + XLSX via Apache POI)
│   │
│   ├── localization/
│   │   └── LocalizationManager.kt (NEW)
│   │
│   ├── onboarding/
│   │   └── OnboardingManager.kt (NEW)
│   │
│   ├── backup/
│   │   └── BackupManager.kt (NEW)
│   │
│   ├── notification/
│   │   ├── NotificationService.kt (NEW)
│   │   └── StorageMonitor.kt (NEW)
│   │
│   ├── error/
│   │   ├── RetryPolicy.kt (NEW - Req 27.1, transient local-op backoff)
│   │   ├── DraftRecoveryManager.kt (NEW - Req 27.4)
│   │   ├── ErrorLog.kt (NEW - Req 27.6, uses SecureLogger)
│   │   └── IssueReporter.kt (NEW - Req 27.7)
│   │
│   ├── recyclebin/
│   │   └── RecycleBinRepository.kt (NEW)
│   │
│   └── validation/
│       └── ValidationEngine.kt
│
├── viewmodel/
│   ├── ExamViewModel.kt (EXPANDED)
│   ├── SubjectViewModel.kt (NEW)
│   ├── StudentViewModel.kt (NEW)
│   ├── AnalyticsViewModel.kt (NEW)
│   ├── ReportViewModel.kt (NEW)
│   ├── SubscriptionViewModel.kt (NEW)
│   └── DashboardViewModel.kt (NEW)
│
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt
│   │
│   ├── screens/
│   │   ├── subjects/
│   │   │   ├── SubjectListScreen.kt (NEW)
│   │   │   ├── SubjectDetailScreen.kt (NEW)
│   │   │   └── CreateSubjectScreen.kt (NEW)
│   │   │
│   │   ├── exams/
│   │   │   ├── ExamListScreen.kt (EXISTING, modified)
│   │   │   ├── ExamDetailScreen.kt (EXPANDED)
│   │   │   ├── NewExamScreen.kt (EXISTING)
│   │   │   ├── EditKeyScreen.kt (EXPANDED with MELC mapping)
│   │   │   └── ItemAnalysisScreen.kt (EXPANDED)
│   │   │
│   │   ├── students/
│   │   │   ├── StudentsScreen.kt (EXPANDED)
│   │   │   ├── StudentProfileScreen.kt (NEW)
│   │   │   └── StudentPerformanceScreen.kt (NEW)
│   │   │
│   │   ├── sections/
│   │   │   ├── SectionsScreen.kt (NEW)
│   │   │   ├── SectionDetailScreen.kt (NEW)
│   │   │   └── RosterManagementScreen.kt (NEW)
│   │   │
│   │   ├── melcs/
│   │   │   ├── MelcBrowserScreen.kt (NEW)
│   │   │   ├── MelcDetailScreen.kt (NEW)
│   │   │   └── MelcMappingScreen.kt (NEW)
│   │   │
│   │   ├── analytics/
│   │   │   ├── AnalyticsDashboardScreen.kt (NEW)
│   │   │   ├── LearningGapsScreen.kt (NEW)
│   │   │   └── CompetencyTrendsScreen.kt (NEW)
│   │   │
│   │   ├── curriculum/
│   │   │   ├── CurriculumTrackingScreen.kt (NEW)
│   │   │   ├── PacingGuideScreen.kt (NEW)
│   │   │   └── QuarterlySummaryScreen.kt (NEW)
│   │   │
│   │   ├── reports/
│   │   │   ├── ReportGeneratorScreen.kt (NEW)
│   │   │   └── ReportPreviewScreen.kt (NEW)
│   │   │
│   │   ├── scanner/
│   │   │   ├── CameraScreen.kt (EXISTING)
│   │   │   ├── ProcessingScreen.kt (EXISTING)
│   │   │   └── GradingViewScreen.kt (EXISTING)
│   │   │
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt (NEW)
│   │   │   ├── SubscriptionScreen.kt (NEW)
│   │   │   ├── BackupManagementScreen.kt (NEW - Backup/Restore)
│   │   │   └── LanguageScreen.kt (NEW)
│   │   │
│   │   ├── dashboard/
│   │   │   └── TeacherDashboardScreen.kt (NEW)
│   │   │
│   │   └── recyclebin/
│   │       └── RecycleBinScreen.kt (NEW)
│   │
│   ├── components/
│   │   ├── GlassCard.kt (EXISTING)
│   │   ├── FloatingGlassCard.kt (EXISTING)
│   │   ├── GlassButton.kt (EXISTING)
│   │   ├── StatCard.kt (NEW)
│   │   ├── ChartCard.kt (NEW)
│   │   ├── MelcChip.kt (NEW)
│   │   ├── MasteryIndicator.kt (NEW)
│   │   ├── SubscriptionBanner.kt (NEW)
│   │   ├── SyncStatusIndicator.kt (NEW)
│   │   └── EmptyStateView.kt (NEW)
│   │
│   └── theme/
│       ├── Color.kt (EXISTING)
│       ├── Theme.kt (EXISTING)
│       └── Type.kt (EXISTING)
│
├── scanner/
│   ├── BubbleSheetProcessor.kt (EXISTING, enhanced)
│   ├── BubbleDetector.kt (NEW)
│   └── ImagePreprocessor.kt (NEW)
│
└── util/
    ├── BitmapHelper.kt
    ├── DateFormatter.kt
    ├── MemoryCache.kt
    ├── AnalyticsTracker.kt
    ├── CrashReporter.kt
    └── PermissionHelper.kt

assets/
└── melcs_database.json (NEW - 5000+ MELCs)

res/
├── values/
│   ├── strings.xml (English)
│   └── strings_fil.xml (Filipino) (NEW)
└── drawable/
    └── (existing graphics + new icons)
```

### B. Estimated APK Size

- **Current v1.0**: ~56 MB
- **Estimated v2.0**: ~75 MB

Size increase breakdown:
- MELCs database JSON: ~2 MB compressed
- New dependencies (ZXing, iText7, Firebase, POI): ~15 MB
- New screens and resources: ~2 MB

Optimization strategies:
- ProGuard/R8 code shrinking
- Resource shrinking
- Split APKs by architecture (arm64, x86)
- App Bundle for Play Store (reduces download size by ~20%)

### C. Glossary of Technical Terms

| Term | Definition |
|------|------------|
| **MVVM** | Model-View-ViewModel architectural pattern for separation of concerns |
| **Room** | Android SQLite persistence library with compile-time SQL verification |
| **Flow** | Kotlin Coroutines reactive stream for asynchronous data |
| **Compose** | Android declarative UI framework (replacement for XML layouts) |
| **StateFlow** | Hot Flow that represents a state with a current value |
| **DAO** | Data Access Object - interface for database operations |
| **Entity** | Data class representing a database table |
| **Repository** | Abstraction layer between data sources and business logic |
| **ViewModel** | Lifecycle-aware component for managing UI state |
| **Coroutine** | Kotlin lightweight thread for asynchronous programming |
| **Dispatchers** | Execution contexts for coroutines (IO, Default, Main) |
| **iText7** | Java library for PDF creation and manipulation |
| **ZXing** | ("Zebra Crossing") Barcode/QR code processing library |
| **ML Kit** | Google mobile SDK for machine learning features |
| **Firebase** | Google Backend-as-a-Service platform |
| **Firestore** | NoSQL cloud database from Firebase |
| **SQLCipher** | Encrypted SQLite database engine |
| **LRU Cache** | Least Recently Used cache eviction policy |
| **ProGuard/R8** | Code optimization and obfuscation tools |

### D. References

1. **DepEd MELCs**: [https://www.deped.gov.ph/](https://www.deped.gov.ph/)
2. **Android Room Database**: [https://developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room)
3. **Jetpack Compose**: [https://developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose)
4. **ML Kit Documentation**: [https://developers.google.com/ml-kit](https://developers.google.com/ml-kit)
5. **iText7 Documentation**: [https://itextpdf.com/](https://itextpdf.com/)
6. **Google Play Billing**: [https://developer.android.com/google/play/billing](https://developer.android.com/google/play/billing)
7. **Firebase Documentation**: [https://firebase.google.com/docs](https://firebase.google.com/docs)
8. **Item Analysis Guide**: Classical Test Theory for item difficulty and discrimination
9. **WCAG 2.1 Guidelines**: [https://www.w3.org/WAI/WCAG21/quickref/](https://www.w3.org/WAI/WCAG21/quickref/)
10. **Android Performance Best Practices**: [https://developer.android.com/topic/performance](https://developer.android.com/topic/performance)

### E. Requirements Traceability Matrix

Every requirement maps to at least one design element (component, data model, or interface) and, where applicable, a correctness property.

| Req | Title | Primary Design Element(s) | Property |
|-----|-------|---------------------------|----------|
| 1 | Subject Folder Organization | `SubjectFolderEntity`, `SubjectRepository`, SubjectListScreen | - |
| 2 | DepEd MELCs Integration | `MelcEntity`, `MelcRepository`, `MasteryCalculator` | 1, 2 |
| 3 | Flexible Assessment Creation | `TemplateEntity`, `TemplateRepository`, `ValidationEngine` | 10 |
| 4 | QR-Coded Answer Sheet Generation | `QRCodeGenerator`, `QRCodeParser` | 5 |
| 5 | Professional PDF Report Generation | `ReportGenerator` | - |
| 6 | Advanced Analytics and Item Analysis | `AnalyticsEngine` | 3, 4 |
| 7 | Section and Class Management | `SectionEntity`, `StudentEnrollmentEntity`, `SectionRepository`, `ImportExportService` | 9 |
| 8 | Comprehensive Student Profiles | `StudentEntity` (modified), `StudentNoteEntity`, `MasteryCalculator`, `ValidationEngine` | 2, 10 |
| 9 | Curriculum Tracking and Pacing | `PacingEngine`, `MelcCoverageEntity` | - |
| 10 | Recycle Bin and Data Recovery | `RecycleBinRepository`, soft-delete columns, `RecycleBinPurgeWorker` | 11 |
| 11 | CSV and Excel Import/Export | `ImportExportService` | 9 |
| 12 | Teacher Dashboard and Insights | `DashboardViewModel`, TeacherDashboardScreen | - |
| 13 | Offline-First Architecture | Room primary source, local MELCs cache, `MemoryCache` | - |
| 14 | User-Owned Data Backup and Restore | BackupManager, BackupManagementScreen | - |
| 15 | Free Tier with Limitations | `SubscriptionManager` (FREE_LIMITS, `checkLimit`) | 12 |
| 16 | Premium Subscription | `SubscriptionManager` (PREMIUM_LIMITS, billing) | 12 |
| 17 | Preserve Existing Scanner | `BubbleSheetProcessor`, existing entities retained | - |
| 18 | Database Schema Expansion | `AppDatabase` **v5 (shipped)** → **v6 via `MIGRATION_5_6`** (registers the 4 new tables); migrations 1→5 already shipped | - |
| 19 | Performance Optimization | `BitmapHelper`, pagination, `MemoryCache`, indexing | - |
| 20 | Internationalization Support | `LocalizationManager`, string resources | - |
| 21 | Answer Sheet Pretty Printer | `AnswerSheetPrettyPrinter` | 6 |
| 22 | Parser for Scanned Answer Sheets | `AnswerSheetParser`, `BubbleDetectionEngine` | 6, 8 |
| 23 | Round-Trip Validation | `AnswerSheetParser` + `AnswerSheetPrettyPrinter` (round-trip) | 6, 7 |
| 24 | Data Integrity and Validation | `ValidationEngine` (exam/student/section/note/file/CSV) | 10 |
| 25 | Security and Privacy | `DatabaseEncryption` (SQLCipher), `BiometricAuthHelper`, `PrivacyManager` | - |
| 26 | Accessibility Compliance | `AccessibleButton`, `AccessibleChart`, semantics | - |
| 27 | Error Handling and Recovery | `RepositoryResult`, `UiError`, `RetryPolicy`, `DraftRecoveryManager`, `SecureLogger`/`ErrorLog`, `IssueReporter`, `StorageMonitor` | - |
| 28 | Onboarding and User Guidance | `OnboardingManager`, sample data seeding | - |
| 29 | Backup and Restore | `BackupManager` | - |
| 30 | Notification System | `NotificationService`, `StorageMonitor` | 11 |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-12 | Design Team | Initial comprehensive design document |
| 1.1 | 2025-01-13 | Design Team | Aligned design with all 30 requirements: added Answer Sheet Parser, Bubble Detection, Import/Export, Localization, Onboarding, Backup, Notification, and Recycle Bin components; added Correctness Properties section (12 properties); added property-based testing strategy (jqwik); added requirements traceability matrix; hardened DB passphrase storage (EncryptedSharedPreferences + Keystore) per security-standards |
| 1.2 | 2025-01-14 | Design Team | Completeness/consistency pass: fully covered Requirement 27 with a criterion map plus `SyncRetryPolicy` (27.1), `DraftRecoveryManager` (27.4), `SecureLogger`/`ErrorLog` for PII-safe logging (27.6), and `IssueReporter` (27.7); split the answer-sheet round-trip into Property 6 (print→fill→scan→process, Req 21.7/23.2) and new Property 6b (parse→print→parse idempotence, Req 23.4); added `validateSectionName` (Req 7.2) and `validateNote` (Req 8.5) to `ValidationEngine` and extended Property 9; updated property count to 13, file structure, and traceability matrix accordingly |
| 1.3 | 2026-09 | Reconciliation | **Baseline reconciled with the actual codebase.** Corrected the false "pristine v1 4-entity DB + v1→v2 migration" premise: the database is already **v5, encrypted (SQLCipher)**, `exam_scanner_database`, with 11 registered entities and migrations 1→5 shipped. Replaced the fabricated v2 `AppDatabase`/`MIGRATION_1_2`/`AutoMigration` with the real v5 declaration and an additive **`MIGRATION_5_6`** that registers the four already-authored, not-yet-registered entity files (`StudentEnrollmentEntity`, `MelcCoverageEntity`, `StudentNoteEntity`, `SyncLogEntity`). Corrected entity shapes to match the code (`MelcEntity.gradeLevel: String`; `SubjectFolderEntity.settingsJson`; the richer `TemplateEntity`; the already-expanded `ExamEntity`/`StudentEntity`; added the previously undocumented `GradingScaleEntity`). Reframed `ExamRepository`, DAO, screens, PDF/CSV/backup utilities, analytics services, and auth as **EXISTING**, with new domain services (analytics, mastery, QR, pretty printer, pacing, billing, sync) described as **integrating** with them. Re-anchored the migration test to 5→6, corrected the package/app-id and DB-name/builder references, and marked EXISTING vs NET-NEW throughout. Kept the conservative library-version strategy and the correctness-properties/testing sections intact. |
| 1.4 | 2026-09 | Implementation | **Documented the real scan pipeline.** Added `BubbleGridMapper` (printed-layout→pixel-grid bridge) that maps `AnswerSheetPrettyPrinter` geometry (PDF points) to a pixel-space `BubbleDetectionEngine.BubbleGrid`, connecting the pretty printer to `AnswerSheetParser`; `BubbleSheetProcessor.processImage` now performs real bubble detection (build model → map grid → `AnswerSheetParser.parse`) instead of returning mock random answers; `ProcessingScreen` threads the exam question-count and option labels. Recorded scanning known limitations (no perspective/deskew, app-printed single-page sheets only, on-device accuracy verification pending). Also recorded post-implementation UI/runtime refinements (Results/Analytics/Reports tabs, Settings Privacy/About consolidation, AppCompatActivity + theme for per-app locales, AppLockGate `rememberSaveable`, ReportViewModel `SubscriptionManager` wiring). |

---

**End of Design Document**
