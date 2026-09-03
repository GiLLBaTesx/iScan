# 📋 How to Add Sections - Quick Guide

## Current Situation

Right now, the sections feature exists in the database but **isn't exposed in the UI yet**.

The home screen shows:
```
┌─────────────┐
│ 3 Subjects  │
│ 0 Sections  │ ← Always shows 0 (not implemented in UI)
│ 0 Exams     │
└─────────────┘
```

## Implementation Plan

I'll add sections management in **3 simple steps**:

### Step 1: Add Section Tab to Folder Detail Screen
When you tap on a subject folder, you'll see:
```
┌──────────────────────────┐
│ Mathematics              │
├──────────────────────────┤
│ [Exams] [Sections] tabs  │← NEW: Sections tab
├──────────────────────────┤
│ Section list shows here  │
└──────────────────────────┘
```

### Step 2: Create Section Dialog
Tap "NEW SECTION" button:
```
┌──────────────────────────┐
│ Create Section           │
├──────────────────────────┤
│ Name: [Grade 7-A    ]    │
│ Capacity: [35       ]    │
│                          │
│        [CANCEL] [CREATE] │
└──────────────────────────┘
```

### Step 3: Link Sections to Exams
When creating/scanning exams, optionally select section:
```
┌──────────────────────────┐
│ Scan Answer Sheet        │
├──────────────────────────┤
│ Section: [Grade 7-A ▼]   │← Select section
│                          │
│ [Start Scanning]         │
└──────────────────────────┘
```

## What I'm Implementing Now

### 1. Update ExamViewModel
Add methods:
```kotlin
- getSections(folderId)
- createSection(folderId, name, capacity)
- updateSection(sectionId, name)
- deleteSection(sectionId)
```

### 2. Update SubjectFolderListScreen  
Make stats dynamic:
```kotlin
// Before (static):
Text("0 Sections")

// After (dynamic):
val totalSections = viewModel.getTotalSections()
Text("$totalSections Sections")
```

### 3. Create SectionsManagementDialog
Simple UI to:
- View sections
- Add new section
- Edit section name
- Delete section

### 4. Update FolderExamListScreen
Add sections tab/view

## User Flow Example

### Teacher: Ms. Santos teaches 3 sections of Grade 7 Math

**Step 1: Create Subject**
- Opens app
- Taps "NEW SUBJECT FOLDER"
- Names it "Mathematics"
- ✅ Subject created

**Step 2: Add Sections**
- Taps "Mathematics" folder
- Sees "Sections" button/tab
- Taps "NEW SECTION"
- Adds:
  - "Grade 7-A" (35 students)
  - "Grade 7-B" (38 students)  
  - "Grade 7-C" (32 students)
- ✅ 3 sections created

**Step 3: Create Exam**
- Taps "NEW EXAM"
- Names it "Midterm Exam"
- Sets 50 questions
- ✅ Exam created

**Step 4: Scan Students**
- Opens "Midterm Exam"
- Taps "SCAN SHEETS"
- Optionally selects "Grade 7-A"
- Scans 35 students from Section A
- ✅ Results tagged with section

**Step 5: View Results**
- Can filter by section
- Compare Section A vs B vs C
- See which section needs help

## Why Sections Are Optional

**Don't need sections if:**
- You only have one class
- You teach different subjects (use folders instead)
- You prefer simple organization

**Need sections if:**
- You have multiple classes per subject
- You want to compare performance
- You teach 50+ students total
- You want organized student management

## Technical Implementation

### Database (Already Exists ✅)
```kotlin
@Entity
data class SectionEntity(
    val id: Long,
    val subjectFolderId: Long,
    val name: String,
    val capacity: Int = 50,
    val createdAt: Long,
    val isDeleted: Boolean = false
)
```

### ViewModel Methods (Adding Now 🔧)
```kotlin
class ExamViewModel {
    // NEW: Section management
    fun getSections(folderId: Long): Flow<List<SectionEntity>>
    fun createSection(folderId: Long, name: String, capacity: Int)
    fun updateSection(sectionId: Long, name: String)
    fun deleteSection(sectionId: Long)
    fun getTotalSectionsCount(): Int
}
```

### UI Components (Adding Now 🔧)
```kotlin
// 1. Section list dialog
@Composable
fun SectionsDialog(...)

// 2. Create section dialog  
@Composable
fun CreateSectionDialog(...)

// 3. Section card in list
@Composable
fun SectionCard(...)

// 4. Section selector dropdown
@Composable
fun SectionSelector(...)
```

## Quick Implementation (30 minutes)

I'm implementing the **minimal viable sections feature**:

1. ✅ Add ViewModel methods (5 min)
2. ✅ Make home stats dynamic (5 min)
3. ✅ Add "Manage Sections" to folder menu (10 min)
4. ✅ Create sections dialog (10 min)

**Result**: Teachers can add/manage sections, see count on home screen

## Future Enhancements (Later)

### Phase 2: Full Integration
- Section tab in folder detail
- Section filter in exam results
- Section comparison charts

### Phase 3: Advanced Features
- Student roster per section
- Section schedules
- Section-specific settings

## Summary

**What's Happening:**
- Sections exist in database ✅
- Not exposed in UI yet ⚠️
- I'm adding basic UI now 🔧

**What You'll Get:**
- Simple section management
- Dynamic section count
- Optional section tagging for exams

**Time to Implement:** ~30 minutes
**Complexity:** Low (basic CRUD)
**Value:** High (better organization)

Let me implement this now! 🚀
