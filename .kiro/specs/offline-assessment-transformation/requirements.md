# Requirements Document

## Introduction

This document specifies requirements for transforming the existing "Exam Scanner" Android application into "Offline Assessment" - a comprehensive teacher assessment system tailored for Filipino K-12 educators. The system expands from basic exam scanning into a complete assessment platform with DepEd MELCs integration, subject organization, professional reporting, advanced analytics, and subscription monetization.

The transformation maintains the existing MVVM architecture, Room database persistence, and premium glassmorphism UI while adding substantial functionality for curriculum tracking, competency mapping, flexible assessment creation, QR-coded answer sheets, class management, and offline-first operation.

## Glossary

- **Assessment_System**: The complete Offline Assessment Android application
- **Teacher**: Filipino K-12 educator using the system (public or private school)
- **Subject_Folder**: Organizational container grouping exams by subject area (Math, Science, English, etc.)
- **MELC**: Most Essential Learning Competency as defined by DepEd
- **MELC_Dataset**: The bundled collection of MELC entries covering every DepEd core subject, grade level (1-12), and quarter (1-4) combination
- **Competency_Mapping**: Association between assessment questions and specific MELCs
- **QR_Sheet**: Printable answer sheet with embedded QR code containing exam metadata
- **Assessment_Template**: PDF or Word document format used for exam creation
- **Premium_User**: Teacher with active ₱100/month subscription
- **Free_User**: Teacher using the system with tier limitations
- **Section**: Class group within a subject (e.g., "Grade 7-A Math")
- **Student_Profile**: Comprehensive student record including demographics and performance history
- **Item_Analysis**: Statistical analysis of question performance (difficulty, discrimination)
- **Analytics_Dashboard**: Teacher-facing interface showing statistics, trends, and insights
- **Recycle_Bin**: Temporary storage for deleted items with restoration capability
- **Quarter**: One of four DepEd grading periods
- **Competency_Mastery**: Student achievement level for specific MELC (developing, approaching, proficient, advanced)
- **PDF_Report**: Exportable document containing scores, statistics, or analysis
- **Learning_Gap**: Identified deficiency in student competency mastery
- **Discrimination_Index**: Statistical measure of question effectiveness (range -1.0 to +1.0)
- **Pacing_Guide**: Curriculum timeline for MELC coverage
- **Batch_Operation**: Action applied to multiple items simultaneously
- **Export_Format**: CSV or Excel file format for data extraction
- **Existing_Scanner**: Current Exam Scanner functionality (camera, ML recognition, grading)

## Requirements

### Requirement 1: Subject Folder Organization

**User Story:** As a Teacher, I want to organize my exams into subject folders, so that I can manage assessments by curriculum area

#### Acceptance Criteria

1. THE Assessment_System SHALL display a subject folder list as the primary navigation screen
2. WHEN a Teacher creates a subject folder, THE Assessment_System SHALL require a subject name (minimum 1 character, maximum 100 characters)
3. THE Assessment_System SHALL allow subject folders to contain optional subject-specific settings (default grading scale, default question count, color theme)
4. WHEN a Teacher selects a subject folder, THE Assessment_System SHALL display all exams within that subject
5. THE Assessment_System SHALL support renaming subject folders without affecting contained exams
6. WHEN a Teacher deletes a subject folder, THE Assessment_System SHALL move the folder and all contained exams to the Recycle_Bin
7. THE Assessment_System SHALL persist subject folder data in the Room database

### Requirement 2: DepEd MELCs Integration

**User Story:** As a Teacher, I want to map assessment questions to DepEd MELCs, so that I can track competency-based learning outcomes

#### Acceptance Criteria

1. THE Assessment_System SHALL bundle a MELC_Dataset that contains at least one MELC entry for every combination of DepEd core subject, grade level 1 through 12, and quarter 1 through 4
2. WHEN a Teacher creates or edits an exam, THE Assessment_System SHALL allow mapping each question to one or more MELCs
3. THE Assessment_System SHALL display MELC code, description, and quarter when selecting competencies
4. WHEN a Teacher views exam results, THE Assessment_System SHALL calculate competency mastery percentages per MELC
5. THE Assessment_System SHALL classify student mastery into four levels: Developing (0-74%), Approaching (75-79%), Proficient (80-89%), Advanced (90-100%)
6. THE Assessment_System SHALL allow filtering MELCs by quarter, subject, and grade level
7. THE Assessment_System SHALL persist MELC mappings in the Room database with foreign key relationships

### Requirement 3: Flexible Assessment Creation

**User Story:** As a Teacher, I want to upload custom assessment templates, so that I can use various sheet formats beyond the default layout

#### Acceptance Criteria

1. WHEN a Teacher creates an exam, THE Assessment_System SHALL provide options: "Default Template", "Upload PDF", "Upload Word Document"
2. THE Assessment_System SHALL accept PDF files up to 10 MB in size
3. THE Assessment_System SHALL accept Word documents (.docx) up to 10 MB in size
4. WHEN a Teacher uploads a template, THE Assessment_System SHALL store the file in local storage
5. WHEN a Teacher customizes a sheet layout, THE Assessment_System SHALL allow modifying exactly these attributes: bubble positions, header fields, and question arrangement, and SHALL keep all other layout attributes (page size, QR code placement, instruction text) fixed
6. WHEN a Teacher selects a custom template, THE Assessment_System SHALL use that template for answer sheet generation
7. THE Assessment_System SHALL validate that uploaded files are readable and not corrupted

### Requirement 4: QR-Coded Answer Sheet Generation

**User Story:** As a Teacher, I want to generate answer sheets with embedded QR codes, so that scanning is faster and more reliable

#### Acceptance Criteria

1. WHEN a Teacher creates an exam, THE Assessment_System SHALL generate a unique QR code embedding exam metadata (exam ID, name, question count, date created)
2. THE Assessment_System SHALL produce printable PDF answer sheets including the QR code in the header section
3. THE Assessment_System SHALL encode QR data using a standard format (JSON string with Base64 encoding if needed)
4. WHEN scanning with the camera, THE Assessment_System SHALL attempt QR detection before bubble sheet processing
5. WHEN a QR code is detected, THE Assessment_System SHALL decode exam metadata and automatically associate the scan with the correct exam
6. WHERE an answer sheet uses a customized layout (bubble positions, header fields, or question arrangement per Requirement 3.5), THE Assessment_System SHALL keep the QR code in its fixed header placement
7. WHEN QR detection fails, THE Assessment_System SHALL fall back to manual exam selection by the Teacher

### Requirement 5: Professional PDF Report Generation

**User Story:** As a Teacher, I want to generate PDF reports, so that I can share results with parents and administrators

#### Acceptance Criteria

1. WHEN viewing exam results, THE Assessment_System SHALL provide "Generate Report" options: "Individual Student", "Class Summary", "School-Level Analytics", presented on the exam's Reports tab
2. THE Assessment_System SHALL create PDF reports formatted for printing on A4 or letter-size paper
3. FOR individual student reports, THE Assessment_System SHALL include: student name, exam name, score, percentage, question-by-question breakdown, competency mastery levels
4. FOR class summary reports, THE Assessment_System SHALL include: class statistics (mean, median, highest, lowest), item analysis charts, MELC mastery distribution
5. FOR school-level reports, THE Assessment_System SHALL aggregate data across sections and include: overall performance trends, competency gaps, comparative statistics
6. THE Assessment_System SHALL brand reports with school name and logo (if configured)
7. WHEN generating a report, THE Assessment_System SHALL save the PDF to device storage and present the Android system share sheet for the saved PDF

### Requirement 6: Advanced Analytics and Item Analysis

**User Story:** As a Teacher, I want advanced assessment analytics, so that I can improve question quality and identify learning gaps

The analytics and item-analysis views described below are surfaced on the exam's Analytics tab.

#### Acceptance Criteria

1. FOR each exam question, THE Assessment_System SHALL calculate difficulty index as percentage of students answering correctly
2. FOR each exam question, THE Assessment_System SHALL calculate discrimination index using upper 27% and lower 27% score groups
3. THE Assessment_System SHALL classify discrimination index values: Poor (< 0.20), Fair (0.20-0.29), Good (0.30-0.39), Excellent (≥ 0.40)
4. WHEN viewing item analysis for a question, THE Assessment_System SHALL display an item response curve plotting the proportion of students answering the question correctly (y-axis, 0.0 to 1.0) against total-score group (x-axis, ordered from lowest to highest score group)
5. THE Assessment_System SHALL identify learning gaps by detecting MELCs with mastery below 75% across the section
6. THE Assessment_System SHALL provide visual analytics: bar charts for answer distribution, line charts for performance trends, pie charts for mastery distribution
7. WHEN analyzing question quality, THE Assessment_System SHALL flag questions with discrimination index below 0.20 or difficulty below 30% or above 90%

### Requirement 7: Section and Class Management

**User Story:** As a Teacher, I want to manage multiple sections per subject, so that I can organize students by class groups

#### Acceptance Criteria

1. WITHIN each Subject_Folder, THE Assessment_System SHALL support creating multiple sections with names (e.g., "Grade 7-A", "Grade 7-B")
2. WHEN creating a section, THE Assessment_System SHALL require section name (maximum 50 characters) and optional capacity (1-100 students)
3. THE Assessment_System SHALL allow assigning students to sections via class roster management
4. THE Assessment_System SHALL support exactly these batch operations and no others: add multiple students from a CSV file, move selected students between sections, and duplicate a section
5. WHEN viewing a section, THE Assessment_System SHALL display section statistics: enrolled count, average performance, assessment history
6. THE Assessment_System SHALL allow exams to be assigned to specific sections or multiple sections simultaneously
7. THE Assessment_System SHALL persist section data with foreign key relationships to students and exams

### Requirement 8: Comprehensive Student Profiles

**User Story:** As a Teacher, I want detailed student profiles, so that I can track individual progress over time

#### Acceptance Criteria

1. FOR each student, THE Assessment_System SHALL store: student ID (maximum 20 characters), full name (maximum 100 characters), grade level, contact information (optional)
2. THE Assessment_System SHALL maintain a performance history including: all exam scores, MELC mastery progression, and, for each assessment assigned to the student's section, whether a scanned result exists for that student (recorded) or is absent (missing)
3. WHEN viewing a student profile, THE Assessment_System SHALL display: overall average, competency mastery matrix, performance trends (line chart), a list of strengths (MELCs where the student's Competency_Mastery is Proficient or Advanced), and a list of weaknesses (MELCs where the student's Competency_Mastery is Developing or Approaching)
4. THE Assessment_System SHALL calculate student competency mastery by aggregating scores across all exams mapping to each MELC
5. THE Assessment_System SHALL allow adding profile notes (maximum 500 characters per note) with timestamps
6. THE Assessment_System SHALL support attaching a profile photo (JPEG/PNG up to 2 MB)
7. FOR ALL student data operations, THE Assessment_System SHALL maintain referential integrity in the Room database

### Requirement 9: Curriculum Tracking and Pacing

**User Story:** As a Teacher, I want to track MELC coverage, so that I can ensure curriculum completion within the school year

#### Acceptance Criteria

1. THE Assessment_System SHALL display a curriculum tracking dashboard showing MELCs coverage by quarter
2. FOR each subject, THE Assessment_System SHALL indicate MELC status: "Not Assessed", "Partially Assessed", "Fully Assessed"
3. THE Assessment_System SHALL calculate coverage percentage as: (assessed MELCs / total MELCs) × 100
4. WHEN viewing pacing guide, THE Assessment_System SHALL show recommended timeline for MELC coverage based on DepEd standards
5. THE Assessment_System SHALL identify behind-schedule MELCs when current date exceeds recommended coverage date
6. THE Assessment_System SHALL allow Teachers to mark MELCs as "Covered" or "Skipped" with optional notes
7. THE Assessment_System SHALL provide quarterly summary reports showing coverage progress

### Requirement 10: Recycle Bin and Data Recovery

**User Story:** As a Teacher, I want a recycle bin for deleted items, so that I can recover accidentally deleted data

#### Acceptance Criteria

1. WHEN a Teacher deletes an exam, subject folder, or section, THE Assessment_System SHALL move the item to the Recycle_Bin instead of permanent deletion
2. THE Assessment_System SHALL retain deleted items in the Recycle_Bin for 30 days
3. WHEN viewing the Recycle_Bin, THE Assessment_System SHALL display: item name, original location, deletion date, item type
4. THE Assessment_System SHALL allow restoring items from the Recycle_Bin to their original location
5. THE Assessment_System SHALL allow permanent deletion with confirmation dialog: "This action cannot be undone. Permanently delete?"
6. WHEN 30 days elapse, THE Assessment_System SHALL automatically purge items from the Recycle_Bin
7. THE Assessment_System SHALL mark deleted items in the database with deletion timestamp and "is_deleted" flag

### Requirement 11: CSV and Excel Import/Export

**User Story:** As a Teacher, I want to import and export data in CSV/Excel format, so that I can integrate with other school systems

#### Acceptance Criteria

1. THE Assessment_System SHALL support exporting exam results to CSV format with columns: student_id, name, exam_name, score, percentage, date
2. THE Assessment_System SHALL support exporting class rosters to CSV format with columns: student_id, name, grade_level, section
3. THE Assessment_System SHALL support exporting MELC mastery data to CSV format with columns: student_id, name, melc_code, mastery_level, percentage
4. WHEN importing student rosters, THE Assessment_System SHALL accept CSV files with headers: student_id, name, grade_level
5. WHEN importing data, THE Assessment_System SHALL validate required fields and display error messages for invalid rows
6. THE Assessment_System SHALL support Excel files (.xlsx) for both import and export with the same column structures
7. WHEN exporting data, THE Assessment_System SHALL save files to the device Downloads folder with descriptive names (e.g., "exam_results_2024_01_15.csv")

### Requirement 12: Teacher Dashboard and Insights

**User Story:** As a Teacher, I want a comprehensive dashboard, so that I can see at-a-glance statistics and actionable insights

#### Acceptance Criteria

1. THE Assessment_System SHALL display a Teacher dashboard as the home screen after subject folder selection
2. THE Dashboard SHALL show statistics: total exams created, total students enrolled, total assessments completed, current quarter
3. THE Dashboard SHALL display performance trends over the selected date range (Requirement 12.6): average scores per interval, competency mastery distribution, and assessment frequency, where the interval is daily for "This Week", weekly for "This Month" and "This Quarter", and monthly for "This Year"
4. THE Dashboard SHALL identify action items: learning gaps requiring intervention, behind-schedule MELCs, flagged questions needing review
5. THE Dashboard SHALL show recent activity: last 5 scanned sheets, recently created exams, recently added students
6. WHEN viewing trends, THE Assessment_System SHALL provide exactly these date range filters: "This Week", "This Month", "This Quarter", "This Year", and SHALL set the trend interval granularity per the selected range as defined in Requirement 12.3
7. THE Dashboard SHALL refresh statistics when underlying data changes (after scanning, grading, or data operations)

### Requirement 13: Offline-First Architecture

**User Story:** As a Teacher, I want the system to work completely offline, so that I can use it without internet connectivity

#### Acceptance Criteria

1. THE Assessment_System SHALL perform all operations without requiring internet connectivity: exam creation, scanning, grading, reporting, analytics
2. THE Assessment_System SHALL store all data locally in the Room database
3. THE Assessment_System SHALL cache the bundled MELC_Dataset (defined in Requirement 2.1) locally during initial installation
4. THE Assessment_System SHALL keep every feature fully available offline; the app has no server-side component and never depends on network connectivity to function
5. THE Assessment_System SHALL NOT transmit user or student data to any server operated by the app provider; users own their data and retain sole custody of it
6. THE Assessment_System SHALL provide data portability exclusively through the local Backup and Restore mechanism defined in Requirement 14, allowing users to store backup files in the storage location or cloud service of their own choosing
7. FOR ALL data operations, THE Assessment_System SHALL ensure data integrity through Room transactions

### Requirement 14: User-Owned Data Backup and Restore

**User Story:** As a Teacher, I want to back up and restore my data to a location I control, so that I own and protect my data without relying on any provider-hosted server

#### Acceptance Criteria

1. THE Assessment_System SHALL provide "Backup Data" and "Restore Data" options in the Settings → Backup Management screen
2. WHEN a Teacher backs up, THE Assessment_System SHALL export the complete encrypted database to a backup file written to device storage that the user can move to any location or cloud service they choose
3. THE Backup file SHALL include all user data: exams, answer keys, student records, results, subject folders, and sections
4. THE Assessment_System SHALL allow the Teacher to restore from a previously created backup file, validating the file before applying it
5. THE Assessment_System SHALL keep all backup and restore operations entirely on-device, with no data sent to any provider-operated server
6. THE Assessment_System SHALL make Backup and Restore available to all users regardless of subscription tier
7. WHERE the device is offline, THE Assessment_System SHALL perform Backup and Restore normally, since these operations require no network connectivity

### Requirement 15: Free Tier with Limitations

**User Story:** As a Teacher, I want to try the system for free with limitations, so that I can evaluate it before subscribing

#### Acceptance Criteria

1. THE Assessment_System SHALL allow Free_Users to create up to 3 subject folders
2. THE Assessment_System SHALL allow Free_Users to create up to 5 exams per subject folder
3. THE Assessment_System SHALL allow Free_Users to scan up to 30 answer sheets per exam
4. THE Assessment_System SHALL allow Free_Users to generate basic PDF reports (individual and class summary only)
5. WHERE a Free_User attempts to exceed limits, THE Assessment_System SHALL display upgrade prompt: "Upgrade to Premium for unlimited [feature]"
6. THE Assessment_System SHALL provide Free_Users access to: item analysis, MELC mapping (view only), student profiles, basic dashboard
7. THE Assessment_System SHALL restrict Free_Users from: advanced analytics, school-level reports, custom templates

### Requirement 16: Premium Subscription (₱100/month)

**User Story:** As a Teacher, I want to subscribe to Premium, so that I can access unlimited features and advanced capabilities

#### Acceptance Criteria

1. THE Assessment_System SHALL offer Premium subscription at ₱100 per month via Google Play In-App Billing
2. WHEN a Teacher initiates subscription, THE Assessment_System SHALL redirect to Google Play payment interface
3. WHEN subscription is confirmed, THE Assessment_System SHALL unlock all Premium features immediately
4. THE Assessment_System SHALL grant Premium_Users unlimited: subject folders, exams, scans, students, sections
5. THE Assessment_System SHALL grant Premium_Users access to: advanced analytics, school-level reports, custom templates, curriculum tracking, pacing guides
6. THE Assessment_System SHALL verify subscription status on app launch by querying Google Play Billing
7. WHEN subscription expires or is cancelled, THE Assessment_System SHALL revert to Free tier and display re-subscription prompt

### Requirement 17: Preserve Existing Scanner Functionality

**User Story:** As a Teacher, I want the existing exam scanning features to remain fully functional, so that the transformation doesn't break current workflows

#### Acceptance Criteria

1. THE Assessment_System SHALL maintain the Existing_Scanner camera capture functionality using CameraX
2. THE Assessment_System SHALL maintain ML text recognition for student ID and name extraction using ML Kit
3. THE Assessment_System SHALL maintain auto-grading with answer key comparison
4. THE Assessment_System SHALL maintain the existing item analysis bar charts showing answer distribution
5. THE Assessment_System SHALL maintain the grading view with color-coded answer review
6. THE Assessment_System SHALL maintain the existing Room database entities: ExamEntity, AnswerKeyEntity, StudentEntity, StudentAnswerEntity
7. THE Assessment_System SHALL maintain the premium glassmorphism UI design system and color palette

### Requirement 18: Database Schema Expansion

**User Story:** As a Developer, I want to expand the database schema, so that new features integrate with existing data structures

#### Acceptance Criteria

1. THE Assessment_System SHALL include SubjectFolderEntity table with columns: id, name, settings_json, created_at, is_deleted, deleted_at
2. THE Assessment_System SHALL include SectionEntity table with columns: id, subject_folder_id, name, capacity, created_at, is_deleted, deleted_at
3. THE Assessment_System SHALL include MelcEntity table with columns: id, code, description, grade_level, subject, quarter
4. THE Assessment_System SHALL include QuestionMelcMappingEntity table with columns: id, exam_id, question_number, melc_id
5. THE Assessment_System SHALL include StudentMelcMasteryEntity table with columns: id, student_id, melc_id, mastery_level, percentage, last_updated
6. THE Assessment_System SHALL include TemplateEntity table with columns: id, name, file_path, file_type, created_at
7. THE Assessment_System SHALL implement an additive database migration from the shipped schema version 5 to version 6 that registers the student_enrollments, melc_coverage, student_notes, and sync_logs tables without renaming, dropping, or recreating any existing table, preserving all existing data

### Requirement 19: Performance Optimization for Low-End Devices

**User Story:** As a Teacher with a budget device, I want fast performance, so that I can use the system smoothly

#### Acceptance Criteria

1. THE Assessment_System SHALL launch within 3 seconds on devices with 2GB RAM running Android 8.0
2. WHEN loading exam lists with 100+ exams, THE Assessment_System SHALL display results within 1 second using pagination (20 items per page)
3. WHEN scanning answer sheets, THE Assessment_System SHALL process images within 5 seconds using background coroutines
4. THE Assessment_System SHALL limit image resolution to 1920×1080 pixels to reduce memory consumption
5. WHEN generating a PDF report for up to 100 students on a device with 2GB RAM running Android 8.0, THE Assessment_System SHALL complete generation without an OutOfMemoryError (Note: incremental page-by-page rendering is the recommended implementation technique)
6. THE Assessment_System SHALL cache frequently accessed data (MELCs database, subject folders) in memory with LRU eviction
7. WHEN executing any database query against a dataset of up to 100 exams and 1000 students on a device with 2GB RAM running Android 8.0, THE Assessment_System SHALL complete the query within 500ms (Note: query execution time is logged to support performance monitoring)

### Requirement 20: Internationalization Support

**User Story:** As a Filipino Teacher, I want the system to support Filipino language, so that I can use it in my native language

#### Acceptance Criteria

1. THE Assessment_System SHALL support two languages: English (default) and Filipino (Tagalog)
2. THE Assessment_System SHALL allow Teachers to switch language via Settings menu
3. WHEN language is changed, THE Assessment_System SHALL immediately update all UI text without requiring app restart
4. THE Assessment_System SHALL translate: navigation labels, button text, dialog messages, error messages, report headers
5. THE Assessment_System SHALL maintain MELC descriptions in English as per DepEd standards
6. THE Assessment_System SHALL persist language preference in SharedPreferences
7. THE Assessment_System SHALL default to system language if available, otherwise English

### Requirement 21: Answer Sheet Pretty Printer

**User Story:** As a Teacher, I want to generate printable answer sheets, so that students have standardized forms for taking exams

#### Acceptance Criteria

1. WHEN a Teacher creates an exam, THE Assessment_System SHALL generate a printable answer sheet PDF
2. THE Pretty_Printer SHALL format answer sheets with: exam name, date, student info fields (name, ID, section), question bubbles (A-G based on exam configuration), QR code
3. THE Pretty_Printer SHALL arrange bubbles in a grid layout optimized for bubble detection (minimum 8mm diameter, 5mm spacing)
4. THE Pretty_Printer SHALL include instruction text: "Shade circles completely. Use black or blue pen."
5. THE Pretty_Printer SHALL support custom layouts when Teachers upload templates
6. THE Pretty_Printer SHALL validate that generated PDFs are readable and printable on standard printers
7. FOR ALL valid exam configurations, THE Assessment_System SHALL ensure that printing, filling, scanning, and processing answer sheets produces accurate results (round-trip property)

### Requirement 22: Parser for Scanned Answer Sheets

**User Story:** As a Developer, I want a robust parser for scanned images, so that bubble detection is accurate and reliable

> **NOTE (implementation scope):** Detection currently targets answer sheets printed by the app (known `AnswerSheetPrettyPrinter` geometry) and assumes a square-on scan. Perspective correction / deskew for angled handheld photos is future work and does not weaken any acceptance criterion below.

#### Acceptance Criteria

1. WHEN a Teacher scans an answer sheet on a device with 2GB RAM running Android 8.0, THE Parser SHALL detect the QR code within 1 second
2. WHEN QR detection succeeds, THE Parser SHALL extract exam metadata and validate against database
3. WHEN QR detection fails, THE Parser SHALL fall back to bubble sheet processing
4. WHEN processing an answer sheet of up to 100 questions on a device with 2GB RAM running Android 8.0, THE Parser SHALL detect shaded bubbles within 5 seconds using image processing algorithms (threshold, contour detection)
5. THE Parser SHALL classify bubble states: "Empty" (0-20% filled), "Partial" (21-79% filled), "Shaded" (80-100% filled)
6. WHEN multiple bubbles are shaded for a question, THE Parser SHALL mark the answer as "Invalid - Multiple"
7. THE Parser SHALL return structured data: student_id, student_name, question_answers array, detection_confidence scores

### Requirement 23: Round-Trip Validation for Answer Sheets

**User Story:** As a Developer, I want round-trip validation, so that I can verify answer sheet generation and scanning work correctly together

#### Acceptance Criteria

1. THE Assessment_System SHALL provide a testing mode for round-trip validation
2. WHEN round-trip testing is executed, THE Assessment_System SHALL generate an answer sheet, simulate filling bubbles, scan the simulated sheet, and verify detected answers match filled answers
3. THE Assessment_System SHALL test round-trip for: all bubble configurations (A-B through A-G), all question counts (5, 10, 20, 50, 100), QR code presence/absence
4. FOR ALL valid exam configurations, THE Assessment_System SHALL ensure parsing then printing then parsing produces equivalent results (round-trip property)
5. WHEN round-trip validation fails, THE Assessment_System SHALL log error details: expected answers, detected answers, confidence scores, image artifacts
6. THE Assessment_System SHALL display round-trip test results in a developer settings screen
7. WHEN round-trip validation runs across the test matrix defined in Requirement 23.3, THE Assessment_System SHALL achieve at least 95% accuracy, measured as the percentage of questions whose detected answer equals the filled answer across all tested sheets

### Requirement 24: Data Integrity and Validation

**User Story:** As a Teacher, I want data validation, so that I cannot enter invalid information that breaks the system

#### Acceptance Criteria

1. WHEN creating exams, THE Assessment_System SHALL validate: name is not empty, question count is between 1 and 200
2. WHEN creating students, THE Assessment_System SHALL validate: student ID is unique within section, name is not empty
3. WHEN mapping MELCs, THE Assessment_System SHALL validate: selected MELC exists in database, mapping is not duplicate
4. WHEN uploading templates, THE Assessment_System SHALL validate: file size does not exceed 10 MB, file format is supported, file is not corrupted
5. WHEN importing CSV, THE Assessment_System SHALL validate: required columns are present, data types are correct, foreign key references exist
6. IF validation fails, THE Assessment_System SHALL display specific error message indicating the violation
7. THE Assessment_System SHALL prevent saving invalid data to the database

### Requirement 25: Security and Privacy

**User Story:** As a Teacher, I want student data to be secure, so that privacy is protected

#### Acceptance Criteria

1. THE Assessment_System SHALL store all sensitive data in the encrypted Room database using SQLCipher
2. WHEN the app is launched and WHEN the app returns to the foreground after being backgrounded, THE Assessment_System SHALL require a successful device credential or biometric authentication (device PIN, pattern, password, or fingerprint) before displaying any student records
3. THE Assessment_System SHALL NOT transmit student data over any network; all data remains on-device except for user-initiated backup files the user controls
4. THE Assessment_System SHALL not transmit student data to third parties
5. THE Assessment_System SHALL allow Teachers to permanently delete student records with data erasure confirmation
6. THE Assessment_System SHALL comply with data minimization principles: collect only necessary information
7. THE Assessment_System SHALL provide a privacy policy accessible from the Settings menu (via About iScan)

### Requirement 26: Accessibility Compliance

**User Story:** As a Teacher with visual impairment, I want the system to be accessible, so that I can use it effectively

#### Acceptance Criteria

1. THE Assessment_System SHALL provide content descriptions for all interactive UI elements (buttons, images, charts)
2. THE Assessment_System SHALL support TalkBack screen reader with logical reading order
3. THE Assessment_System SHALL maintain minimum touch target size of 48dp × 48dp for all interactive elements
4. THE Assessment_System SHALL provide sufficient color contrast: minimum 4.5:1 for normal text, 3:1 for large text
5. THE Assessment_System SHALL allow text scaling up to 200% without breaking layouts
6. THE Assessment_System SHALL avoid relying solely on color to convey information (use icons and labels)
7. THE Assessment_System SHALL provide haptic feedback for critical actions (scan complete, delete confirmation)

### Requirement 27: Error Handling and Recovery

**User Story:** As a Teacher, I want graceful error handling, so that the app doesn't crash or lose my data

#### Acceptance Criteria

1. WHEN a transient local operation (e.g., file export/import or image processing) fails, THE Assessment_System SHALL retry up to 3 times with exponential backoff before surfacing an error
2. WHEN database operations fail, THE Assessment_System SHALL rollback transactions and display error message
3. WHEN image processing fails, THE Assessment_System SHALL log error details and allow retry or manual entry
4. IF the app crashes during a scanning session in progress or during exam editing, THEN THE Assessment_System SHALL persist that in-progress scan data or exam edit to temporary storage and offer to restore it on next launch
5. WHEN storage space is insufficient, THE Assessment_System SHALL display warning: "Low storage. Please free up space."
6. THE Assessment_System SHALL log errors to local file for debugging without exposing sensitive data
7. WHEN critical errors occur, THE Assessment_System SHALL provide "Report Issue" option to contact support

### Requirement 28: Onboarding and User Guidance

**User Story:** As a new Teacher user, I want onboarding guidance, so that I can learn how to use the system effectively

#### Acceptance Criteria

1. WHEN a Teacher launches the app for the first time, THE Assessment_System SHALL display a welcome tutorial explaining key features
2. THE Tutorial SHALL cover: creating subjects, setting up exams, mapping MELCs, scanning sheets, viewing reports
3. THE Assessment_System SHALL provide contextual tooltips for complex features (discrimination index, mastery levels)
4. THE Assessment_System SHALL offer a "Help" section accessible from Settings with FAQ and video tutorials
5. THE Assessment_System SHALL include sample data: 1 pre-populated subject folder, 2 sample exams, 5 sample students with results
6. THE Assessment_System SHALL allow skipping the tutorial with "Skip" button
7. THE Assessment_System SHALL allow re-accessing the tutorial from Settings → Help → "Show Tutorial Again"

### Requirement 29: Backup and Restore

**User Story:** As a Teacher, I want to backup and restore my data, so that I can recover from device loss or migration

#### Acceptance Criteria

1. THE Assessment_System SHALL provide "Backup Data" option in Settings
2. WHEN backing up, THE Assessment_System SHALL export complete database as encrypted ZIP file to device storage
3. THE Backup SHALL include: all tables, images, templates, configuration
4. THE Assessment_System SHALL name backups with timestamp: "offline_assessment_backup_2024_01_15_14_30.zip"
5. THE Assessment_System SHALL provide "Restore Data" option in Settings
6. WHEN restoring, THE Assessment_System SHALL validate backup file integrity before applying
7. WHEN restoration completes, THE Assessment_System SHALL restart the app to reload data

### Requirement 30: Notification System

**User Story:** As a Teacher, I want notifications for important events, so that I stay informed about system activities

#### Acceptance Criteria

1. WHEN a data backup completes successfully, THE Assessment_System SHALL display notification: "Backup completed successfully"
2. WHEN a data backup fails, THE Assessment_System SHALL display notification: "Backup failed. Please try again."
3. WHEN one or more Recycle_Bin items are within 3 days of the 30-day retention limit defined in Requirement 10.2, THE Assessment_System SHALL display a notification using the template "{count} items will be permanently deleted in {days} days", substituting the actual item count and remaining days
4. WHEN storage space is low (below 100 MB), THE Assessment_System SHALL display notification: "Low storage. Back up and clear old data."
5. THE Assessment_System SHALL allow Teachers to configure notification preferences in Settings
6. THE Assessment_System SHALL respect system-level Do Not Disturb settings
7. THE Assessment_System SHALL display in-app banners for non-critical notifications instead of system notifications

