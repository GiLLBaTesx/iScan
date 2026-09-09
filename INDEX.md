# iScan - Filipino Exam Scanner & Analytics Platform

> Complete documentation for the premium OMR bubble sheet scanner app with MELC integration, built for Filipino educators.

**Repository**: [https://github.com/GiLLBaTesx/iScan.git](https://github.com/GiLLBaTesx/iScan.git)  
**Platform**: Android (Kotlin + Jetpack Compose)  
**Status**: ✅ Production Ready (91% Feature Complete)  
**Last Updated**: September 4, 2026

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Key Features](#key-features)
3. [Design & UI](#design--ui)
4. [MELC Integration](#melc-integration)
5. [Backup & Data Management](#backup--data-management)
6. [Privacy & Security](#privacy--security)
7. [Getting Started](#getting-started)
8. [Architecture](#architecture)
9. [Competitive Advantage](#competitive-advantage)
10. [Build & Install](#build--install)
11. [Feature Status](#feature-status)
12. [Testing](#testing)
13. [Roadmap](#roadmap)

---

## Overview

**iScan** is a comprehensive exam management and grading system designed specifically for Filipino K-12 educators. It combines OMR bubble sheet scanning with MELC (Most Essential Learning Competencies) tracking and analytics.

### What Makes iScan Different?

- 🇵🇭 **MELC Integration** - Track curriculum competencies (2,414 MELCs pre-loaded)
- 📊 **Advanced Analytics** - Competency-based performance insights
- 🎨 **ScanKey Novelty Azure Glass Design** - Modern, premium UI
- 💾 **Complete Backup System** - Protect your data with exports
- 📄 **PDF Template Generator** - Create OMR sheets instantly
- 📱 **Offline-First** - Works without internet connection

---

## Key Features

### Core Functionality ✅

| Feature | Description | Status |
|---------|-------------|--------|
| **Subject Folders** | Organize exams by subject/course | ✅ Working |
| **Exam Management** | Create, edit, delete exams | ✅ Working |
| **Answer Key Editor** | Set correct answers (A-E support) | ✅ Working |
| **Camera Scanner** | Scan bubble sheets with camera | ✅ Working |
| **Auto-Grading** | Instant scoring and results | ✅ Working |
| **Student Roster** | Manage students by section | ✅ Working |
| **CSV Import/Export** | Bulk import students, export results | ✅ Working |
| **Section Management** | Organize classes and sections | ✅ Working |

### Advanced Features ✅

| Feature | Description | Status |
|---------|-------------|--------|
| **MELC Tagging** | Tag questions to learning competencies (inline in Edit Key) | ✅ Working |
| **Competency Analytics** | Track mastery by MELC code | ✅ Working |
| **Student Analytics** | Individual performance insights | ✅ Working |
| **Smart Dashboard** | Exam overview with key metrics | ✅ Working |
| **PDF Template Download** | Quick access to 5 OMR templates (20Q-100Q) | ✅ Working |
| **Template Generator** | Create custom OMR templates | ✅ Working |
| **Backup System** | Create, restore, export, cleanup backups | ✅ Working |
| **Recycle Bin** | 30-day soft delete with restore capability | ✅ Working |
| **Privacy Policy** | In-app privacy documentation | ✅ Working |
| **Auto-backup Cleanup** | Smart retention (keeps last 3, removes >30 days) | ✅ Working |

### Partially Implemented ⚠️

| Feature | Description | Status | Priority |
|---------|-------------|--------|----------|
| **PDF Export** | Export analytics/reports to PDF | ⚠️ Button exists, not functional | High |
| **Language Selection** | Multi-language support | ⚠️ UI exists, English only | Low |

### Not Implemented / Removed 🔒

| Feature | Description | Status |
|---------|-------------|--------|
| **Firebase Authentication** | User login/signup system | 🗑️ Completely removed (not needed) |
| **Cloud Sync** | Real-time data synchronization | 🚫 Not planned (privacy-first) |
| **Cloud Storage** | Store data on Firebase/cloud | 🚫 Not planned (local-only) |

**Note**: All authentication and Firebase code has been removed. The app is 100% offline and privacy-focused.

---

## Design & UI

### ScanKey Novelty Azure Glass Theme

**Color Palette**:
- Ice White: `#F4F9FF` (primary background)
- Electric Blue: `#0052FF` (accent)
- Icy Cyan: `#00D4FF` (success states)
- Deep Red: `#8B0000` (error states)
- Warning Amber: `#FFA726` (warnings)

**Design Principles**:
- ✨ Frosted glass cards with blur effect
- 🎨 Multi-color answer buttons (A-E: red/blue/orange/deepred/purple)
- 📱 Long-press context menus for quick actions
- 🎯 Minimalist top bar with ⋮ menu
- 💫 Smooth animations and transitions

**Key Screens**:
1. **SubjectFolderListScreen** - Home dashboard with folders
2. **ExamDetailScreen** - Exam overview with analytics
3. **EditKeyScreen** - Answer key editor with inline MELC tagging
4. **BackupManagementScreen** - Backup management UI
5. **SettingsScreen** - App settings and utilities

---

## MELC Integration

### What are MELCs?

MELCs (Most Essential Learning Competencies) are the learning standards defined by DepEd Philippines for K-12 curriculum. iScan has **2,414 MELCs pre-loaded** covering all subjects and grade levels.

### How It Works:

1. **Tag Questions** - Assign MELC codes to exam questions in Edit Key screen
2. **Inline Tagging** - Tag while setting answer keys (one place, one flow)
3. **Competency Tracking** - See which competencies students master
4. **Analytics** - View competency-based performance metrics

### MELC Features:

- ✅ Tag questions in Edit Key screen (inline tagging while setting answers)
- ✅ Real-time save and display of MELC tags
- ✅ Competency analytics per MELC code
- ✅ Mastery tracking (pass/fail by competency)
- ✅ 2,414 MELCs across all subjects

### Sample MELCs:

```
English Grade 1: EN1A-Ia-1 - Recognize letters
Math Grade 7: M7AL-IIa-1 - Translate phrases to expressions
Science Grade 10: S10LT-IIIa-34 - Explain genetic engineering
```

---

## Backup & Data Management

### Complete Backup Solution

**Features**:
- ✅ Create database backups with verification
- ✅ Restore from backup (automatic app restart)
- ✅ Export to external storage (Google Drive, Downloads, etc.)
- ✅ Import from file picker
- ✅ Delete individual backups
- ✅ Clear all backups (with confirmation)
- ✅ Automatic cleanup (keeps last 7 backups)
- ✅ Smart retention (remove backups >30 days, keep minimum 3)
- ✅ Safety backups before restore operations
- ✅ Backup integrity verification
- ✅ WAL checkpoint before backup (ensures consistency)
- ✅ Share latest backup button (quick export)

### Recycle Bin System

**30-Day Soft Delete Protection**:
- ✅ Deleted exams kept for 30 days
- ✅ Easy restore with one tap
- ✅ Permanent delete option
- ✅ Empty recycle bin (bulk delete)
- ✅ Shows "Deleted X days ago" timestamps
- ✅ Automatic cleanup after 30 days

### Clear All Data

**Safe Data Deletion**:
- ✅ Automatic safety backup before clearing
- ✅ Enhanced confirmation dialog with warnings
- ✅ Shows backup location after clearing
- ✅ Deletes all: folders, exams, students, scans
- ✅ Keeps backups intact (separate storage)

### Backup Location:
```
/storage/emulated/0/Android/data/com.examscanner.premium/files/backups/
```

### Backup Files:
- `backup_YYYYMMDD_HHMMSS.db` - Regular backups
- `backup_before_restore_YYYYMMDD_HHMMSS.db` - Safety backups
- `backup_before_clear_YYYYMMDD_HHMMSS.db` - Pre-deletion backups

### How to Use:

**Create Backup**:
1. Settings → Data Management → Manage Backups
2. Tap floating action button (FAB)
3. Wait for "✓ Backup created successfully!"

**Restore Backup**:
1. Manage Backups → Tap Restore icon on backup
2. Confirm action → App creates safety backup
3. App restarts in 5 seconds → Data restored

**Export Backup**:
1. Manage Backups → Tap "Share Latest Backup"
2. Or tap Share icon on specific backup
3. Choose destination (Drive, Downloads, etc.)

**Cleanup Old Backups**:
1. Manage Backups → Tap "Cleanup Old Backups"
2. Removes backups >30 days
3. Always keeps at least 3 most recent

**Recycle Bin**:
1. Settings → Storage → Recycle Bin
2. View deleted exams from last 30 days
3. Tap Restore (green) or Delete Permanently (red)

---

## Privacy & Security

### Privacy-by-Design Architecture

**Data Protection Principles**:
- 🔒 **Local Storage Only** - All data stored on device
- 🚫 **No Cloud Uploads** - Zero external data transmission
- 🔐 **No Tracking** - No analytics, no advertisements
- 🛡️ **No Third Parties** - No data sharing whatsoever
- 📱 **Offline-First** - Full functionality without internet

### Philippine Data Privacy Act (RA 10173) Compliance

**Teacher as Data Controller**:
- You (the teacher) control all student data
- App developers have no access to your data
- Local storage ensures full data sovereignty
- Easy export for compliance reporting

**Student Privacy Protection**:
- Recommend using student IDs instead of full names
- No biometric data collection
- No location tracking
- No contact access

### Developer Responsibilities

**Our Commitments**:
- ✅ No data collection or external transmission
- ✅ No tracking, analytics, or advertising
- ✅ Regular security updates and bug fixes
- ✅ Transparent privacy policy updates
- ✅ Prompt response to security issues
- ✅ Local-only architecture (no backdoors)

**We Are NOT Responsible For**:
- ❌ User compliance with school policies
- ❌ Device security (lost/stolen devices)
- ❌ How users share exported data
- ❌ Data loss due to user error

### Security Best Practices

**For Teachers**:
1. **Device Security**
   - Use strong PIN/password/fingerprint lock
   - Enable device encryption (default on modern Android)
   - Enable "Find My Device" for remote wipe

2. **Data Minimization**
   - Only collect necessary assessment data
   - Use student IDs instead of full names when possible
   - Avoid storing unnecessary personal information

3. **Regular Backups**
   - Weekly backups to Google Drive (encrypted)
   - Monthly backups downloaded to secure PC
   - Delete old data after school year ends

4. **Access Control**
   - Don't share device with students
   - Lock device when not in use
   - Don't leave device unattended

### Privacy Policy Access

**In-App Documentation**:
- Settings → Privacy & Security → Privacy Policy
- Comprehensive offline-accessible policy
- Covers: data handling, user rights, compliance
- Updated with every app release

### DepEd Compliance

**Follows DepEd Data Privacy Guidelines**:
- Transparent data collection
- User consent requirements documented
- Data retention recommendations
- Incident response procedures

**Complies With**:
- Philippine Data Privacy Act (RA 10173)
- DepEd Data Privacy Guidelines
- FERPA principles (for international use)
- GDPR principles (where applicable)

---

## Getting Started

### Prerequisites:
- Android Studio Hedgehog or later
- Android SDK 34
- Kotlin 1.9+
- Gradle 8.0+

### Installation:

```bash
# Clone repository
git clone https://github.com/GiLLBaTesx/iScan.git
cd iScan

# Build debug APK
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Quick Start:

1. **Create Folder** - Tap "+" to create subject folder (e.g., "Math Grade 7")
2. **Create Exam** - Open folder → Tap "+" → Set name and questions
3. **Set Answer Key & Tag MELCs** - Tap exam → "Edit Answer Key" → Select correct answers and tag MELCs inline
4. **Scan Sheet** - Tap "Scan" → Camera opens → Capture bubble sheet
5. **View Results** - See scores, analytics, and competency tracking

### User Flow:

```
Home (Folders) 
  → Folder Detail (Exams)
    → Exam Detail (Results + Analytics)
      → Edit Key (Set answers + Tag MELCs in one place)
      → Camera (Scan sheets)
      → Student Roster (Manage students)
```

---

## Architecture

### Tech Stack:

**Frontend**:
- Jetpack Compose (Modern UI toolkit)
- Material 3 Design
- Navigation Component
- Kotlin Coroutines

**Backend**:
- Room Database (SQLite wrapper)
- Repository Pattern
- ViewModels + StateFlow
- Dependency Injection

**Key Libraries**:
```gradle
// UI
implementation 'androidx.compose.material3:material3:1.1.2'
implementation 'androidx.navigation:navigation-compose:2.7.5'

// Database
implementation 'androidx.room:room-runtime:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'

// Camera
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'

// PDF Generation (iText)
implementation 'com.itextpdf:itext7-core:7.2.5'

// DataStore (preferences)
implementation 'androidx.datastore:datastore-preferences:1.0.0'

// QR Code Generation
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
```

### Database Schema:

**Main Tables**:
- `subject_folders` - Subject/course folders
- `exams` - Exam definitions
- `answer_keys` - Correct answers per question
- `students` - Student roster
- `student_answers` - Scanned results
- `melc_entities` - MELC competencies (2,414 records)
- `question_melc_mappings` - Question-to-MELC links
- `sections` - Class sections

### Key Files:

**Core**:
- `MainActivity.kt` - Navigation setup
- `ExamScannerApplication.kt` - App initialization

**Data Layer**:
- `AppDatabase.kt` - Room database
- `ExamRepository.kt` - Data access
- `ExamViewModel.kt` - State management

**UI Screens**:
- `SubjectFolderListScreen.kt` - Home dashboard
- `ExamDetailScreen.kt` - Exam overview
- `EditKeyScreen.kt` - Answer key editor with inline MELC tagging
- `BackupManagementScreen.kt` - Backup UI
- `SettingsScreen.kt` - Settings

**Utilities**:
- `BackupManager.kt` - Backup operations
- `TemplatePDFGenerator.kt` - PDF generation
- `BubbleSheetProcessor.kt` - OMR scanning
- `ExportUtility.kt` - CSV export
- `CSVImportUtility.kt` - CSV import

---

## Competitive Advantage

### vs ZipGrade

| Feature | iScan | ZipGrade |
|---------|-------|----------|
| **Price** | Free | $6.99/year |
| **MELC Integration** | ✅ 2,414 MELCs | ❌ None |
| **Competency Analytics** | ✅ Built-in | ❌ None |
| **Custom Templates** | ✅ Unlimited | ⚠️ Limited |
| **Offline Mode** | ✅ Full functionality | ⚠️ Limited |
| **Export Options** | ✅ CSV + Backup | ⚠️ CSV only |
| **Student Management** | ✅ Sections + Roster | ⚠️ Basic |
| **UI Design** | ✅ Modern Azure Glass | ⚠️ Dated |
| **Local Data** | ✅ Full control | ❌ Cloud dependent |

### Unique Selling Points:

1. **Filipino Education Focus** - Built for DepEd K-12 curriculum
2. **MELC Competency Tracking** - Only scanner with MELC integration
3. **Advanced Analytics** - Competency-based insights
4. **Complete Offline Support** - No internet required
5. **Premium Design** - ScanKey Novelty Azure Glass UI
6. **Free & Open Source** - No subscriptions or cloud fees
7. **Full Data Control** - Local storage + export backups

---

## Build & Install

### Build Commands:

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Run tests
./gradlew test

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Build Configuration:

```gradle
android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.examscanner.premium"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
```

### APK Location:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Feature Status

### Completion Summary

**Overall Progress**: 91% Complete (21/23 major features)

| Category | Functional | Needs Work | Completion |
|----------|-----------|------------|-----------|
| Core Assessment | 9/9 | 0 | 100% ✅ |
| Data Management | 4/4 | 0 | 100% ✅ |
| Privacy & Security | 3/3 | 0 | 100% ✅ |
| Templates | 3/3 | 0 | 100% ✅ |
| Settings | 2/3 | 1 | 67% ⚠️ |
| Export/Reporting | 0/1 | 1 | 0% 🔴 |
| **TOTAL** | **21/23** | **2** | **91%** ✅ |

### Production Readiness

**✅ Ready for Deployment**:
- Core assessment workflow is complete
- Data management is robust
- Privacy protections implemented
- Backup/recovery system working
- UI is polished and functional

**🎯 Recommended Before Major Launch**:
1. Implement PDF export (highest priority)
2. Remove/disable non-functional language selector
3. Test with real classroom data (50+ students)

**Deployment Options**:
- ✅ Internal school use - Ready now
- ✅ Pilot program - Ready now
- ✅ Google Play Store - Ready (list PDF export as "coming soon")
- 🎯 Full public launch - Add PDF export first

### Known Limitations

**PDF Export**:
- Button exists in Smart Dashboard
- Currently shows: "PDF export feature coming in next update" toast
- **Impact**: Users cannot print/share analytics reports
- **Workaround**: Use CSV export for data, screenshot for visuals
- **ETA**: Implement in next sprint

**Language Selection**:
- Menu shows "English (Coming soon)"
- App is English-only
- **Impact**: Non-English speakers may have difficulty
- **Options**: Remove menu, implement Filipino, or leave as-is
- **Priority**: Low (most Filipino teachers understand English)

---

## Roadmap

### Phase 1: Critical Features (1-2 weeks)

**1. PDF Export Implementation** 🔴 High Priority
- Export exam summary reports
- Export score sheets with student names/scores
- Export question analysis with charts
- Export competency analytics
- Share via file picker or print

**2. Polish Non-functional Elements** 🟡 Medium Priority
- Remove or disable language selector
- Add functionality to "App Storage" (navigate to Android settings)
- Clean up any remaining "Coming soon" placeholders

### Phase 2: Optimization (1-2 weeks)

**3. Performance Improvements** 🟢 Low Priority
- Add pagination for exam lists (handle 100+ exams)
- Optimize database queries with indexes
- Add loading skeletons for better UX
- Improve camera scan processing speed

**4. Enhanced UX** 🟢 Low Priority
- Add search functionality (find exams by name/date)
- Add filters (by subject, date range, pass rate)
- Add sort options (newest, oldest, by average score)
- Add empty states with helpful guidance

### Phase 3: Advanced Features (2-4 weeks)

**5. CSV Enhancements** 🟢 Nice to Have
- Bulk import students from CSV
- Export with custom column selection
- Export for DepEd reporting formats
- Export for school MIS integration

**6. Advanced Analytics** 🟢 Nice to Have
- Historical trends (track student progress over time)
- Comparative analysis (section vs section)
- Item difficulty analysis (identify hard questions)
- Mastery levels by MELC code

**7. Bulk Operations** 🟢 Power User
- Select multiple exams for export
- Batch move exams between folders
- Bulk delete with confirmation
- Batch print reports

### Phase 4: Optional Enhancements

**8. Notifications** 🔵 Optional
- Reminder to backup data weekly
- Alert when recycle bin items will auto-delete
- Notify when low on storage

**9. Localization** 🔵 Optional
- Filipino/Tagalog translation
- Cebuano translation
- String resource extraction
- Regional date/number formats

### Features NOT Planned

**Live Sync / Cloud Storage** 🚫
- Real-time data synchronization across devices
- Cloud storage of exam data
- Firestore/Firebase database integration
- **Reason**: Privacy-first architecture - all data stays local

**Why No Cloud Sync?**
1. **Privacy Commitment** - Zero external data transmission
2. **Data Sovereignty** - Teachers have full control
3. **DepEd Compliance** - Meets data privacy requirements
4. **Offline-First** - No internet dependency
5. **No Costs** - No cloud storage fees

**Alternative**: Export backups to Google Drive manually (user controlled)

### Future Considerations

**Advanced Features**:
- Biometric authentication (fingerprint/face unlock)
- Database encryption at rest
- Backup file encryption
- Auto-backup scheduling
- Dark mode theme
- Tablet/landscape optimization
- Parent/student portal
- Integration with Learning Management Systems (LMS)

**Teacher Collaboration**:
- Share exam templates with colleagues
- School-wide MELC analytics
- District-level reporting
- Professional development insights

---

## Testing

### Manual Testing Checklist:

**Core Features**:
- [x] Create subject folder
- [x] Rename/delete folder
- [x] Create exam with questions
- [x] Set answer key (A-E support)
- [x] Scan bubble sheet
- [x] View results and scores
- [x] Export to CSV

**MELC Features**:
- [x] Tag MELC in Edit Key screen
- [x] Verify tag appears in MELC Map screen
- [x] Tag MELC in MELC Map screen
- [x] Verify tag appears in Edit Key screen
- [x] View competency analytics
- [x] Check mastery tracking

**Backup Features**:
- [x] Create backup
- [x] List backups (verify timestamp, size)
- [x] Restore backup
- [x] Restart app (verify data restored)
- [x] Export backup to Downloads
- [x] Import backup from file picker
- [x] Delete old backup
- [x] Cleanup old backups (smart retention)
- [x] Clear all backups (bulk delete)
- [x] Share latest backup

**Recycle Bin**:
- [x] Delete exam (moves to recycle bin)
- [x] View deleted items in recycle bin
- [x] Restore deleted exam
- [x] Permanently delete exam
- [x] Empty recycle bin (bulk delete)
- [x] Verify 30-day auto-cleanup

**Privacy & Security**:
- [x] View privacy policy in-app
- [x] Verify no network requests (offline mode)
- [x] Check local data storage
- [x] Test safety backup before clear
- [x] Verify data deletion

**Template Features**:
- [x] Download 20Q template from home
- [x] Download 40Q template from home
- [x] Verify PDF opens or shares
- [x] Create custom template from Settings
- [x] Generate with 5 choices (A-E)

**UI/UX**:
- [x] Verify frosted glass cards
- [x] Test long-press menus
- [x] Check multi-color answer buttons
- [x] Verify smooth animations
- [x] Test all navigation flows

**Settings**:
- [x] Privacy policy accessible
- [x] Backup management working
- [x] Recycle bin functional
- [x] Clear all data (with safety backup)
- [x] Template generator
- [x] About dialog

### Test Script:

```bash
# Run backup test
./test_backup_restore.sh

# Check database
adb shell "run-as com.examscanner.premium sqlite3 /data/data/com.examscanner.premium/databases/exam_scanner_database 'SELECT COUNT(*) FROM exams;'"

# List backups
adb shell "ls -lh /storage/emulated/0/Android/data/com.examscanner.premium/files/backups/"

# Check recycle bin
adb shell "run-as com.examscanner.premium sqlite3 /data/data/com.examscanner.premium/databases/exam_scanner_database 'SELECT COUNT(*) FROM exams WHERE isDeleted = 1;'"
```

### Performance Testing:

**Load Testing**:
- Test with 100+ exams
- Test with 50+ students per exam
- Test with 10+ folders
- Verify UI responsiveness

**Stress Testing**:
- Create/restore large backups (50MB+)
- Scan 20+ bubble sheets rapidly
- Delete/restore 50+ items from recycle bin

**Edge Cases**:
- Empty exam (no students)
- Exam with 0 questions  
- Invalid QR codes
- Camera permission denied
- Storage permission denied
- No backups available
- Recycle bin empty

**Device Compatibility**:
- Android 7 (API 24) - minimum
- Android 10 (API 29) - common
- Android 12 (API 31) - common
- Android 14 (API 34) - latest
- Various screen sizes (phone, tablet)
- Different camera qualities

---

## Project Structure

```
iScan/
├── app/
│   ├── src/main/
│   │   ├── java/com/examscanner/premium/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ExamScannerApplication.kt
│   │   │   ├── data/                    # Database, repository, entities
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── ExamRepository.kt
│   │   │   │   ├── BuiltInData.kt       # 2,414 MELCs
│   │   │   │   └── SampleMelcsData.kt
│   │   │   ├── scanner/                 # Camera & OMR processing
│   │   │   │   ├── CameraScreen.kt
│   │   │   │   └── BubbleSheetProcessor.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/             # All UI screens
│   │   │   │   │   ├── SubjectFolderListScreen.kt
│   │   │   │   │   ├── ExamDetailScreen.kt
│   │   │   │   │   ├── EditKeyScreen.kt
│   │   │   │   │   ├── MapQuestionsToMelcScreen.kt
│   │   │   │   │   ├── BackupManagementScreen.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   ├── components/          # Reusable components
│   │   │   │   │   ├── GlassCard.kt
│   │   │   │   │   └── ScanKeyComponents.kt
│   │   │   │   └── theme/               # Colors, typography
│   │   │   │       └── Color.kt
│   │   │   ├── utils/                   # Utilities
│   │   │   │   ├── BackupManager.kt
│   │   │   │   ├── TemplatePDFGenerator.kt
│   │   │   │   ├── ExportUtility.kt
│   │   │   │   └── CSVImportUtility.kt
│   │   │   └── viewmodel/               # ViewModels
│   │   │       └── ExamViewModel.kt
│   │   ├── res/                         # Resources
│   │   └── AndroidManifest.xml
│   ├── build.gradle                     # App dependencies
├── gradle/
├── build.gradle                         # Project config
├── settings.gradle
├── index.md                             # This file
└── README.md                            # GitHub README

```

---

## Development Notes

### Recent Changes:

**September 4, 2026**:
- ✅ **Removed Firebase/Auth completely** - Not needed for offline-first app
- ✅ Implemented Recycle Bin (30-day soft delete with restore)
- ✅ Enhanced backup management (cleanup old backups, clear all)
- ✅ Added Privacy Policy screen (in-app documentation)
- ✅ Implemented Clear All Data with automatic safety backup
- ✅ Added developer responsibilities to privacy policy
- ✅ Fixed backup/restore crash issues
- ✅ Added backup action buttons (restore, share, delete)
- ✅ Improved UI with visible action icons
- ✅ Smart backup retention (keeps last 3, removes >30 days)

**September 3, 2026**:
- ✅ Enabled PDF template download from home dialog
- ✅ Enabled backup export with file picker
- ✅ Improved backup restore (WAL checkpoint, safety backups)
- ✅ Updated UI warnings for app restart requirement
- ✅ Pushed all changes to GitHub

**Previous Updates**:
- ✅ Implemented bi-directional MELC sync
- ✅ Redesigned UI with ScanKey Azure Glass theme
- ✅ Created complete backup system
- ✅ Integrated 2,414 MELCs from DepEd
- ✅ Built competency analytics dashboard
- ✅ Added PDF template generator

### Known Issues:

1. **PDF Export Not Implemented** - Button exists but doesn't generate PDFs (planned for next sprint)
2. **App Restart Required After Restore** - Room database caches connections, requires restart to see restored data (by design, 5-second delay added)
3. **Language Selection Non-functional** - Shows "Coming soon", English only currently
4. **Deprecation Warnings** - Minor icon deprecation warnings (non-critical)

### Future Enhancements:

**High Priority**:
- [ ] Implement PDF export (reports, analytics, score sheets)
- [ ] Add search functionality for exams
- [ ] Add filter/sort options

**Medium Priority**:
- [ ] CSV export enhancements (custom columns, DepEd formats)
- [ ] Advanced analytics (historical trends, item difficulty)
- [ ] Bulk operations (multi-select, batch export)

**Low Priority**:
- [ ] Add search/filter functionality for exams
- [ ] Add scheduled automatic backups
- [ ] Add backup encryption
- [ ] Add dark mode theme
- [ ] Add multi-language support (Tagalog, Cebuano)
- [ ] Add notifications (backup reminders)

**Not Planned**:
- ❌ Firebase/Cloud authentication
- ❌ Cloud storage for user data
- ❌ Multi-device data sync

- ❌ Live sync / real-time synchronization
- ❌ Cloud storage for user data
- ❌ Multi-device data sync via Firebase

**Optional** (Would require architecture changes):
- [ ] Teacher collaboration features (share templates)
- [ ] Parent/student portal
- [ ] LMS integration

---

## Credits

**Developed by**: iScan Development Team  
**Design Inspired by**: ScanKey Novelty Azure Glass  
**MELC Data Source**: DepEd Philippines K-12 Curriculum  
**Platform**: Android (Kotlin + Jetpack Compose)  

---

## License

This project is proprietary software. All rights reserved.

**For Educational Use**: Free for Filipino educators and schools  
**Commercial Use**: Contact for licensing

---

## Support

**Issues**: [GitHub Issues](https://github.com/GiLLBaTesx/iScan/issues)  
**Email**: support@iscan.app (if applicable)  
**Documentation**: This file (index.md)

---

## Quick Reference

### Important Paths:
```
Database: /data/data/com.examscanner.premium/databases/exam_scanner_database
Backups: /storage/emulated/0/Android/data/com.examscanner.premium/files/backups/
Templates: /storage/emulated/0/Documents/ExamScanner/Templates/
```

### Key Commands:
```bash
# Build
./gradlew assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Check database
adb shell "run-as com.examscanner.premium ls -la /data/data/com.examscanner.premium/databases/"

# List backups
adb shell "ls -lh /storage/emulated/0/Android/data/com.examscanner.premium/files/backups/"
```

### Feature Access:
```
MELC Tagging: Edit Key → Tap MELC icon on question → Select MELC (all in one place!)
Analytics: Exam Detail → Scroll down to "Competency Analytics"
Backup: Settings → Manage Backups
Templates: Home → ⋮ menu → Download Templates
Export: Exam Detail → "Export" button
```

---

**Version**: 1.0  
**Status**: ✅ Production Ready (91% Complete)  
**Last Updated**: September 4, 2026  
**Ready For**: Pilot deployment, internal school use, Google Play Store submission

---

*This is the comprehensive documentation for iScan. All other markdown files have been consolidated into this INDEX.md. For specific questions or issues, please refer to the relevant sections above or check the GitHub repository.*
