# Implementation Plan: Offline Assessment Transformation

## Overview

This plan targets **only the work that genuinely remains** to finish the Offline Assessment transformation. The design and requirements were reconciled against the actual codebase, which is far more built-out than an earlier draft assumed. The Room database already ships **encrypted at version 5** (SQLCipher `SupportFactory` + `EncryptionKeyManager.getDatabasePassphrase`, DB name `exam_scanner_database`) with **11 registered entities**, migrations `1→5`, and a single large `ExamRepository(dao: ExamDao)` that already implements subject-folder CRUD (soft delete), grading-scale/template init + CRUD, MELC read + validated question→MELC mapping, exam CRUD, answer-key save, student results + score calculation, roster/section management, post-scan section organization, per-section item analysis (`QuestionAnalysis`), the exam recycle bin, and `clearAllData` with auto-backup. Most screens already exist under `ui/screens/`, as do the scanner (`BubbleSheetProcessor`, `CameraScreen`), `ExamViewModel`, auth, analytics, and a full set of utilities.

This plan therefore **integrates with and extends** the existing code — it never recreates it. The verbs "wire" and "extend" are used deliberately for anything that already exists; "create" is reserved for genuinely net-new files.

### Already completed (do not re-plan)

- **Build dependencies + test infrastructure** are in place in `app/build.gradle`: JUnit 5 platform (`de.mannodermaus.android-junit5`), jqwik `1.9.1`, Apache POI (`poi` + `poi-ooxml`), ZXing (`core` + embedded), ML Kit `barcode-scanning`, Play Billing `billing-ktx`, `androidx.room:room-testing`, and the `room.schemaLocation` compiler arg (paired with flipping `exportSchema = true`).
- **The four net-new entity FILES already exist on disk** but are **NOT yet registered** in `@Database`: `data/StudentEnrollmentEntity.kt`, `data/MelcCoverageEntity.kt`, `data/StudentNoteEntity.kt`, `data/SyncLogEntity.kt`. Their column shapes are fixed by the shipped files — do not change them; register them as-is.

### Ground rules for every task

- The DB is **v5 → v6 via one additive `MIGRATION_5_6`**. There is **no** `v1→v2` migration. No existing table is renamed, dropped, or recreated.
- `MelcEntity.gradeLevel` is a **`String`**; `StudentEntity` uses a single `name` + `photoPath` (not `profilePhotoPath`) and has **no** soft-delete. Profile notes live in the net-new `StudentNoteEntity`. `TemplateEntity` is already rich.
- All new code follows workspace conventions: Azure Glass theme colors, `SecureLogger` for logging, SQLCipher for the DB, and analytics tracking (`AnalyticsTracker`/`SessionTracker`) for important events.
- New services integrate with the existing `ExamRepository`/`ExamDao` and existing utilities (`BackupManager`, `CSVImportUtility`, `CSVAnswerKeyImport`, `ExportUtility`, `TemplatePDFGenerator`, `AnalyticsExporter`, `EncryptionKeyManager`, `SecureLogger`, `InputSanitizer`, `PasswordValidator`, `RootDetector`, `TrialGuard`, `BubbleSheetProcessor`, `BuiltInData`, `SampleMelcsData`, `OnboardingPreferences`) rather than duplicating them.
- Each of the 13 design correctness properties maps to exactly one jqwik property test (min 100 tries), tagged `// Feature: offline-assessment-transformation, Property {n}: {text}`.

## Tasks

- [x] 1. Register the four new entities and add the additive `MIGRATION_5_6`
  - [x] 1.1 Register net-new entities and bump the schema to v6
    - In `data/AppDatabase.kt`, add `StudentEnrollmentEntity::class`, `MelcCoverageEntity::class`, `StudentNoteEntity::class`, `SyncLogEntity::class` to the `@Database` `entities` array (bringing the total to 15), set `version = 6`, and flip `exportSchema = true`
    - Add the `MIGRATION_5_6 : Migration(5, 6)` object that `CREATE TABLE`s `student_enrollments`, `melc_coverage`, `student_notes`, `sync_logs` with column shapes matching the existing entity files, plus their FK indices (`student_enrollments` unique `(studentId, sectionId)`; matching `CREATE INDEX` for the FK child columns on `melc_coverage` and `student_notes`); register it in the builder's `.addMigrations(...)` alongside `1_2..4_5`
    - Keep opening the DB via `SupportFactory(EncryptionKeyManager.getDatabasePassphrase())`; do NOT rename/recreate any existing table
    - Resolve any Room "FK column lacks index" warnings by mirroring indices in both the entity files and the migration SQL
    - _Requirements: 8.7, 9.x (coverage storage), 13.2, 13.7, 18.7_

  - [x] 1.2 Add DAO methods for the four new tables
    - Extend `ExamDao` (do not create parallel DAOs) with insert/read/delete queries for `student_enrollments` (enroll/unenroll, list by section, list by student), `melc_coverage` (upsert coverage status, read by subject/quarter), `student_notes` (insert timestamped note, list by student), and `sync_logs` (insert log, read recent, clear)
    - Return `Flow` where the UI observes changes; use `@Transaction` where multi-step
    - _Requirements: 7.7, 8.5, 9.6, 13.6, 14.2_

  - [x]* 1.3 Write the 5→6 migration test
    - Using `MigrationTestHelper` + `androidx.room:room-testing`: seed a **v5** DB with representative rows (subject_folders, exams, students with String `gradeLevel`), run `MIGRATION_5_6`, assert all pre-existing rows are preserved untouched and the four new tables exist and are queryable
    - Note the SQLCipher-in-test caveat: open the test DB with the same `SupportFactory` OR run against an unencrypted in-test DB, per the design
    - _Requirements: 18.7_

- [x] 2. Checkpoint - schema registered and migration verified
  - Ensure the project builds, Room generates the v6 schema JSON, and the migration test passes. Ask the user if questions arise.

- [x] 3. Extend the existing data layer for net-new reads/writes
  - [x] 3.1 Add enrollment + coverage + notes access to `ExamRepository`
    - Add `enrollStudent`/`unenrollStudent`/`getEnrollments(sectionId)` over `student_enrollments`, `upsertMelcCoverage`/`getCoverage(subject, quarter)` over `melc_coverage`, and `addStudentNote`/`getStudentNotes(studentId)` over `student_notes`, delegating to the new DAO methods
    - Wrap operations with analytics tracking and `SecureLogger` on failure; preserve all existing repository methods
    - _Requirements: 7.3, 8.5, 9.2, 9.6_

  - [x] 3.2 Add subject and section recycle-bin symmetry + pagination
    - Add `restoreSubjectFolder`/`permanentlyDeleteSubjectFolder` and `restoreSection`/`permanentlyDeleteSection` to mirror the existing exam recycle-bin methods, and a `purgeExpiredDeleted(retentionDays = 30)` sweep across soft-deleted subjects/sections/exams
    - Add a paginated exam query (`getExamsBySubjectPaged(folderId, limit, offset)`) and `getAllStudentRecords(studentId)` used by mastery/reports
    - _Requirements: 1.6, 7.x, 10.1, 10.2, 10.4, 10.5, 10.6, 10.7, 19.2_

  - [x]* 3.3 Write property test for recycle-bin retention purge
    - **Property 11: Recycle bin retention purge**
    - **Validates: Requirements 10.2, 10.6**
    - Use jqwik over arbitrary deletion timestamps; assert `purgeExpiredDeleted(30)` removes exactly items older than 30 days

  - [x] 3.4 Expand the bundled MELC dataset for full DepEd coverage
    - Extend `SampleMelcsData` (consumed by the existing `ExamRepository.initializeSampleMelcs()`) so the seeded set contains at least one MELC per core subject × grade level 1-12 × quarter 1-4; keep `gradeLevel` as a `String`
    - _Requirements: 2.1, 13.3_

- [x] 4. Implement net-new domain services (mastery, analytics, validation)
  - [x] 4.1 Create `MasteryCalculator` over the existing DAO
    - `determineMasteryLevel` band logic (Developing 0-74.99 / Approaching 75-79.99 / Proficient 80-89.99 / Advanced 90-100), `updateStudentMastery(studentId)` aggregating earned/possible across all mapped exams and persisting reduced `(percentage, masteryLevel)` via `dao.insertStudentMelcMastery` (REPLACE), and `getMasterySummary` (strengths/weaknesses)
    - _Requirements: 2.4, 2.5, 8.3, 8.4_

  - [x]* 4.2 Write property test for mastery level classification
    - **Property 1: Mastery level classification matches defined bands**
    - **Validates: Requirements 2.5**

  - [x]* 4.3 Write property test for bounded mastery aggregation
    - **Property 2: Mastery percentage is a bounded aggregation**
    - **Validates: Requirements 2.4**

  - [x] 4.4 Create `AnalyticsEngine` over the existing repository
    - `calculateDifficulty`, `calculateDiscriminationIndex` (upper/lower 27%), `classifyDiscrimination`, `identifyLearningGaps` (<75% across section), `getItemResponseCurve`, and low-quality flagging (discrimination < 0.20, difficulty < 30% or > 90%); reuse existing `QuestionAnalysis`/score methods, don't duplicate them
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.7_

  - [x]* 4.5 Write property test for difficulty index
    - **Property 3: Difficulty index equals proportion correct**
    - **Validates: Requirements 6.1**

  - [x]* 4.6 Write property test for discrimination index bounds and classification
    - **Property 4: Discrimination index is bounded and classified consistently**
    - **Validates: Requirements 6.2, 6.3**

  - [x] 4.7 Create `ValidationEngine` with typed results
    - Validate exam name non-empty + question count 1-200, student ID unique-within-section + ≤20 legal chars, section name ≤50, note ≤500, MELC mapping existence/no-duplicate, template file size ≤10MB/format; reuse existing `InputSanitizer`
    - _Requirements: 7.2, 8.5, 24.1, 24.2, 24.3, 24.4, 24.6, 24.7_

  - [ ]* 4.8 Write property test for input validation partitioning
    - **Property 10: Input validation partitions inputs correctly**
    - **Validates: Requirements 7.2, 8.5, 24.1, 24.2, 24.3**

- [x] 5. Implement QR + answer-sheet generation and parsing services
  - [x] 5.1 Create `QRCodeGenerator`
    - JSON + Base64 `ExamMetadata` encoding via ZXing; bitmap and base64 outputs for PDF embedding
    - _Requirements: 4.1, 4.3_

  - [x] 5.2 Create `QRCodeParser`
    - ML Kit barcode scanning to decode `ExamMetadata` with a fast `hasQRCode` check; `SecureLogger` on failure
    - _Requirements: 4.4, 4.5, 4.7, 22.1, 22.2_

  - [x]* 5.3 Write property test for QR metadata round-trip
    - **Property 5: QR metadata round-trip**
    - **Validates: Requirements 4.1, 4.3, 4.5**

  - [x] 5.4 Create `AnswerSheetPrettyPrinter` (iText7)
    - Generate A4 PDF with QR in the fixed header, student info fields, instruction text, bubble grid (A-G, ≥8mm/5mm), incremental page rendering; consume the existing rich `TemplateEntity` for custom layouts; expose a testable `buildSheetModel(config)` for round-trip simulation
    - _Requirements: 3.5, 3.6, 4.2, 4.6, 19.5, 21.1, 21.2, 21.3, 21.4, 21.5, 21.6_

  - [x] 5.5 Create `BubbleDetectionEngine` integrating with `BubbleSheetProcessor`
    - Threshold + contour fill-ratio detection with `classifyFill` (EMPTY ≤0.20 / PARTIAL 0.21-0.79 / SHADED ≥0.80); reuse the existing `BubbleSheetProcessor` for image handling rather than replacing it; enforce the 1920×1080 image cap
    - _Requirements: 17.1, 17.2, 19.3, 19.4, 22.4, 22.5, 22.7_

  - [x] 5.6 Create `AnswerSheetParser` pipeline
    - QR-first then bubble fallback, multi-mark resolution ("No Answer"/single label/"Invalid - Multiple"), confidence scores, structured `ParseResult`; keep auto-grading via the existing answer-key comparison; provide a deterministic simulated detector path for round-trip tests
    - _Requirements: 4.4, 4.5, 4.7, 17.3, 22.2, 22.3, 22.6, 22.7_
    - _Refined: `BubbleDetectionEngine` (5.5) and `AnswerSheetParser` (5.6) are now actually wired into the runtime scan path via `BubbleGridMapper` + `BubbleSheetProcessor.processImage` (previously implemented but not called by the UI — see 11.2)._

  - [x]* 5.7 Write property test for bubble parse resolution
    - **Property 8: Bubble parse resolution**
    - **Validates: Requirements 22.5, 22.6**

  - [x]* 5.8 Write property test for print-fill-scan-process round-trip
    - **Property 6: Answer sheet print-fill-scan-process round-trip**
    - **Validates: Requirements 21.7, 23.2**
    - Uses the deterministic simulated `BubbleDetectionEngine` across question counts {5,10,20,50,100} and option sets A-B..A-G

  - [x]* 5.9 Write property test for parse-print-parse idempotence
    - **Property 7: Parse-print-parse round-trip is idempotent**
    - **Validates: Requirements 23.4**

- [x] 6. Checkpoint - domain services (mastery, analytics, QR, sheet I/O)
  - Ensure all tests pass and the project builds. Ask the user if questions arise.

- [x] 7. Implement reporting, pacing, and import/export services
  - [x] 7.1 Create `ReportGenerator` for all three report types (iText7)
    - Individual, class summary, and school-level reports with school name/logo branding and incremental page rendering; save PDF to Downloads and return the file path for the share sheet; read data through the existing `ExamRepository` + new `MasteryCalculator`/`AnalyticsEngine`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 19.5_

  - [x]* 7.2 Write unit tests for report statistics
    - Test mean/median/highest/lowest/std-dev and empty-cohort edge cases
    - _Requirements: 5.4_

  - [x] 7.3 Create `PacingEngine` over MELC coverage
    - `getPacingGuide` (recommended-week schedule + coverage status), behind-schedule detection, `markMelcCovered`/`markMelcSkipped` persisting to `melc_coverage`, `getQuarterlySummary`; coverage % = assessed/total × 100
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [x]* 7.4 Write unit tests for coverage status and behind-schedule logic
    - Test status transitions and coverage percentage math
    - _Requirements: 9.2, 9.3, 9.5_

  - [x] 7.5 Create `ImportExportService` reusing existing CSV utilities
    - XLSX read/write via Apache POI for exam results, roster, and MELC mastery, plus CSV via the existing `CSVImportUtility`/`ExportUtility`/`AnalyticsExporter`/`CSVAnswerKeyImport`; export to Downloads with timestamped names; roster import validates each row via `ValidationEngine`
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 24.5_

  - [x]* 7.6 Write property test for CSV/Excel roster round-trip
    - **Property 9: CSV/Excel roster round-trip**
    - **Validates: Requirements 11.2, 11.4, 11.6**

- [x] 8. Implement subscription/tier gating ~~and cloud sync~~ _(cloud sync removed — app is offline-only; data portability via Backup/Restore)_
  - [x] 8.1 Create `SubscriptionManager` (Google Play Billing)
    - Billing client init, `checkSubscriptionStatus` on launch, `subscribeToPremium`, tier limits, `checkLimit` for count-based (3 subjects / 5 exams per subject / 30 scans per exam) and feature-based gating, revert to Free on expiry; integrate with existing `TrialGuard`
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7_

  - [x]* 8.2 Write property test for tier limit enforcement
    - **Property 12: Tier limit enforcement**
    - **Validates: Requirements 15.1, 15.2, 15.3, 16.4**

  - [ ] 8.3 ~~Create `SyncManager` (Firebase, last-write-wins)~~ **(removed — app is offline-only; data portability via Backup/Restore)**
    - ~~Reuse existing `auth/` (email/password) for gating; sync exams/keys/students/results/folders/sections, AES-256 encryption before upload, `sync_logs` writes, sync-status StateFlow, connectivity-triggered auto-sync + offline queueing; entirely behind opt-in; wrap network calls in `SyncRetryPolicy` backoff~~
    - De-scoped per the offline/user-owned-data decision. `SyncManager.kt` deleted. The `sync_logs` table/`SyncLogEntity`/DAO methods are retained (unused/reserved) per Option A. Data portability is provided by the existing `BackupManager` (Requirement 14).
    - _Requirements: (Req 14 now "User-Owned Data Backup and Restore"; satisfied by BackupManager, not SyncManager)_

  - [ ]* 8.4 ~~Write property test for last-write-wins conflict resolution~~ **(removed with SyncManager)**
    - ~~**Property 13: Sync conflict resolution is last-write-wins**~~
    - De-scoped: `SyncConflictResolutionPropertyTest.kt` deleted. Former Property 13 removed from design; remaining 12 properties are not renumbered.

- [x] 9. Checkpoint - reporting, pacing, import/export, billing ~~sync~~ _(sync removed)_
  - Ensure all tests pass and the project builds. Ask the user if questions arise.

- [x] 10. Add net-new ViewModels (only where genuinely missing)
  - [x] 10.1 Create `StudentViewModel`
    - Profile data, performance history, mastery matrix, timestamped notes (via `student_notes`), photo attachment (`photoPath`); back with `ExamRepository` + `MasteryCalculator`
    - _Requirements: 8.1, 8.2, 8.3, 8.5, 8.6_

  - [x] 10.2 Create `AnalyticsViewModel` and `ReportViewModel`
    - Expose `AnalyticsEngine` outputs and orchestrate `ReportGenerator`; gate school-level reports + advanced analytics through `SubscriptionManager.checkLimit`
    - _Requirements: 5.1, 5.7, 6.4, 6.6, 15.4, 15.7_

  - [x] 10.3 Create `DashboardViewModel`
    - Aggregate stats, trends with per-range interval granularity (daily/weekly/monthly), action items (learning gaps / behind-schedule MELCs / flagged questions), recent activity; refresh on data changes
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

  - [x] 10.4 Create ~~`SyncViewModel` and~~ `SubscriptionViewModel` _(SyncViewModel removed — app is offline-only; data portability via Backup/Restore)_
    - Bind `SubscriptionManager` state to UI (`SyncViewModel`/`SyncManager` de-scoped and deleted)
    - _Requirements: 16.3, 16.7_

  - [x]* 10.5 Write unit tests for dashboard trend interval selection
    - Test interval granularity mapping for each date-range filter
    - _Requirements: 12.3, 12.6_

- [x] 11. Wire net-new services into existing screens
  - [x] 11.1 Wire MELC mapping + item analysis into existing exam screens
    - Wire the existing `MelcSelectorDialog` for question→MELC mapping through `ExamRepository.saveQuestionMelcMappings`; render item response curve, answer-distribution bars, and mastery pie/line charts in `ExamDetailScreen`, preserving existing bar charts; use Azure Glass colors + content descriptions
    - _Requirements: 2.2, 2.3, 6.4, 6.6, 17.4, 17.5_
    - _Refined: Exam Detail reorganized into Results/Analytics/Reports tabs; item analysis moved to the Analytics tab; reports to the Reports tab; overflow menu limited to Edit answer key/Rename exam/Generate answer sheet; MELC-mapping and Delete-exam entry points removed from this screen — MELC mapping remains in the Answer Key Editor._

  - [x] 11.2 Wire answer-sheet generation + QR-first scanning into existing screens
    - Add an answer-sheet generation entry in `ExamDetailScreen`/`TemplateGeneratorScreen` calling `AnswerSheetPrettyPrinter`; wire `CameraScreen`/`ProcessingScreen` to `AnswerSheetParser` with QR-first flow and manual exam-selection fallback
    - _Requirements: 4.7, 17.1, 21.1, 22.3_
    - _Refined: `ProcessingScreen` now runs REAL bubble detection — new pure `BubbleGridMapper` maps the printed `AnswerSheetPrettyPrinter` layout to a pixel-space `BubbleGrid`, and `BubbleSheetProcessor.processImage(uri, totalQuestions, optionLabels)` runs `AnswerSheetParser.parse` instead of returning mock random answers (BubbleGridMapperTest added). Known limitations: no perspective/deskew (assumes square-on scan), app-printed single-page sheets only, on-device accuracy verification pending._

  - [x] 11.3 Wire report generation + share into existing screens
    - Add report options (Individual/Class/School) to `ExamDetailScreen`, generate via `ReportViewModel`, and present the Android system share sheet for the saved PDF
    - _Requirements: 5.1, 5.7_
    - _Refined: report options now live on the Exam Detail Reports tab; the exam-detail `ReportViewModel` receives the app-scoped `SubscriptionManager` so school-level report tier-gating works for Premium users._

  - [x] 11.4 Wire dashboard, student profile, recycle bin, and settings screens
    - Bind `SmartDashboardMVP` to `DashboardViewModel`; wire student profile UI to `StudentViewModel`; extend `RecycleBinScreen` to show subjects/sections (not just exams) with restore/permanent-delete; add subscription-management entry to `SettingsScreen` _(cloud-sync-setup entry removed — Backup Management is the data-portability entry point)_
    - _Requirements: 8.1, 10.1, 10.3, 10.4, 10.5, 12.1, 15.5, 16.2_
    - _Refined: removed the duplicate Privacy Policy entry (standalone "Privacy & Security" section) and a dead in-Settings "About" dialog from `SettingsScreen`; Privacy Policy is now reached via Settings → About iScan → Privacy Policy._

  - [x] 11.5 Wire pacing/curriculum tracking UI
    - Surface `PacingEngine` output (coverage by quarter, behind-schedule MELCs, mark covered/skipped) in a dashboard/analytics section using existing screens; keep MELC descriptions in English
    - _Requirements: 9.1, 9.2, 9.4, 9.5, 9.6_

  - [x]* 11.6 Add accessibility semantics and verify touch targets on wired screens
    - Content descriptions, logical reading order, ≥48dp targets, non-color-only status indicators
    - _Requirements: 17.7_

- [x] 12. Checkpoint - ViewModels and UI wiring
  - Ensure all tests pass and the project builds. Ask the user if questions arise.

- [x] 13. Implement cross-cutting concerns
  - [x] 13.1 Create `LocalizationManager` + string resources
    - Per-app locale switching without restart, persisted in SharedPreferences; `values/strings.xml` (English) + `values-fil/strings.xml` (Filipino); MELC descriptions stay in English; wire the toggle into `SettingsScreen`
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7_
    - _Refined per runtime review: `MainActivity` → `AppCompatActivity` + AppCompat XML theme (`Theme.AppCompat.DayNight.NoActionBar`) so per-app locales back-port on API 26–32._

  - [x] 13.2 Wire an authentication gate for student records
    - Require device credential/biometric on launch and on return-to-foreground before showing student data, reusing existing `auth/` + security utilities and `SecurityWarningScreen`
    - _Requirements: 8.7, 13.2_
    - _Refined per runtime review: `AppLockGate` persists its unlocked state via `rememberSaveable` so a configuration change/locale switch does not spuriously re-prompt biometrics; genuine backgrounding still re-locks._

  - [x] 13.3 Create error handling, crash recovery, and storage monitor
    - `RepositoryResult`/`UiError` propagation, `withTransaction` rollback, `DraftRecoveryManager` for in-progress scan/edit persistence + restore on launch, `RetryPolicy` backoff for transient local operations (file export/import, image processing), `ErrorLog` via `SecureLogger` (PII-redacted), and `StorageMonitor` low-storage banner
    - _Requirements: 27.1, 27.2, 27.3, 27.4, 27.5, 27.6, 27.7_

  - [x] 13.4 Create `NotificationService` + `RecycleBinPurgeWorker`
    - Backup/expiry/low-storage notifications honoring preferences + DND, in-app banners for non-critical; `RecycleBinPurgeWorker` (WorkManager) daily calls `ExamRepository.purgeExpiredDeleted(30)` and emits an expiry notification
    - _Requirements: 10.6, 27.5_

  - [x] 13.5 Create `OnboardingManager` reusing existing onboarding
    - First-launch tutorial (skippable, re-accessible) + contextual tooltips through the existing `OnboardingScreen`/`OnboardingPreferences`; optional sample-data seeding (1 subject / 2 exams / 5 students) via existing repository methods
    - _Requirements: 28.x (onboarding)_

- [x] 14. Integrate performance optimizations and in-memory caching
  - Add an LRU in-memory cache for the MELC dataset and subject folders, enforce the 1920×1080 image cap in the scanning path, ensure paginated exam-list loading uses `getExamsBySubjectPaged`, and log query execution times via `SecureLogger`
  - _Requirements: 19.1, 19.2, 19.4, 19.6, 19.7_

- [x] 15. Wire application startup and dependency graph
  - Update `ExamScannerApplication`/`MainActivity` to initialize `SubscriptionManager` _(SyncManager wiring removed — app is offline-only)_, apply the saved locale, run the onboarding/auth gates in the correct order, and schedule `RecycleBinPurgeWorker`; DB init already exists — extend, don't duplicate
  - _Requirements: 13.1, 13.2, 16.6, 20.7, 27.4_
  - _Refined per runtime review: `MainActivity` is an `AppCompatActivity` (a `FragmentActivity` subclass, so `BiometricPrompt` still works) with an AppCompat theme for per-app locale back-port on API 26–32; the exam-detail `ReportViewModel` receives the app-scoped `SubscriptionManager` for school-report gating._

- [ ]* 16. Write integration tests for offline flow and round-trip developer mode
  - Cover the offline exam-create/scan/grade/report path and round-trip developer settings across the test matrix (bubble configs A-B..A-G, question counts 5/10/20/50/100, QR present/absent)
  - _Requirements: 13.1, 23.1, 23.3, 23.5, 23.6, 23.7_

- [x] 17. Final checkpoint - full build and all tests pass
  - Ensure the project builds and all unit, property, and migration tests pass. Ask the user if questions arise.

## Notes

- Build dependencies, JUnit 5 + jqwik, POI/ZXing/ML Kit-barcode/Billing/room-testing, and the schema-export compiler arg are **already in place** — no task re-adds them.
- The four net-new entity **files already exist**; Task 1 only *registers* them and adds the additive `MIGRATION_5_6`. No `v1→v2` migration exists or is created.
- Tasks marked with `*` are optional test sub-tasks and can be skipped for a faster MVP; core implementation tasks are never optional.
- Each of the 13 correctness properties is implemented by exactly one jqwik property test (min 100 tries) tagged with its design property number; the additive `5→6` migration is validated by a Room migration test (Req 18.7), with the SQLCipher-in-test caveat noted in the design.
- Existing `ExamRepository`, `ExamDao`, entities, screens, scanner, `ExamViewModel`, and utilities are integrated with and extended — never recreated (Requirement 17).
- All new code uses Azure Glass theme colors, `SecureLogger`, SQLCipher encryption, and analytics tracking per workspace conventions.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3", "3.1", "3.2", "3.4"] },
    { "id": 3, "tasks": ["3.3", "4.1", "4.4", "4.7", "5.1", "5.2", "5.5"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.5", "4.6", "4.8", "5.3", "5.4", "5.6"] },
    { "id": 5, "tasks": ["5.7", "5.8", "5.9", "7.1", "7.3", "7.5", "8.1", "8.3"] },
    { "id": 6, "tasks": ["7.2", "7.4", "7.6", "8.2", "8.4"] },
    { "id": 7, "tasks": ["10.1", "10.2", "10.3", "10.4"] },
    { "id": 8, "tasks": ["10.5", "11.1", "11.4", "11.5"] },
    { "id": 9, "tasks": ["11.2"] },
    { "id": 10, "tasks": ["11.3", "11.6"] },
    { "id": 11, "tasks": ["13.1", "13.2", "13.3", "13.4", "13.5"] },
    { "id": 12, "tasks": ["14", "15"] },
    { "id": 13, "tasks": ["16"] }
  ]
}
```
