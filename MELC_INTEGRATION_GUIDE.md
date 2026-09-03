# 📚 MELC Integration Guide

## Overview
MELCs (Most Essential Learning Competencies) are DepEd competency standards that can be tagged to exam questions. Once tagged, they appear throughout the app to help teachers track competency coverage.

---

## ✅ Where MELCs Appear in the App

### 1. **Edit Answer Key Screen** 
**Location:** Exam Detail → ⋮ Menu → "Edit answer key"

**What shows:**
- **Tag MELC Button** - Below each question's A/B/C/D/E buttons
  - **Before tagging:** Light blue gradient button with book icon 📖, says "Tag MELC"
  - **After tagging:** Green gradient button with checkmark ✓, says "MELC Tagged ✓"
  - **Below button:** Green badge showing MELC code (e.g., "M7AL-IIa-1")

**User Flow:**
1. Tap "Tag MELC" button below any question
2. MELC Selector dialog opens with search and filters
3. Select a competency
4. Button turns green with checkmark
5. MELC code displays below in green badge

---

### 2. **MELC Selector Dialog**
**Location:** Edit Answer Key → Tap "Tag MELC" button

**Features:**
- ✅ Search by code or description
- ✅ Filter by subject
- ✅ Filter by quarter
- ✅ Shows all 264 Filipino MELC competencies
- ✅ Color-coded chips (Subject: Blue, Grade: Purple, Quarter: Orange)
- ✅ Selected MELC shows green checkmark
- ✅ Can clear/remove MELC tagging

**Display:**
- Modern glassmorphic dialog with gradient blue header
- Each MELC shows:
  - **Code:** (e.g., "M7AL-IIa-1")
  - **Description:** Full competency text
  - **Chips:** Subject | Grade Level | Quarter

---

### 3. **Exam Detail Screen (Menu)**
**Location:** Exam Detail → ⋮ Menu

**What shows:**
- Menu item: "Map questions to MELCs" with blue book icon ✓
- This navigates to Edit Answer Key screen where teacher can tag

**Purpose:** Quick access to MELC mapping from exam overview

---

### 4. **Analytics Dashboard (Smart Dashboard MVP)**
**Location:** Exam Detail → "See Insights" button

**What shows:**
- **MELC-specific analytics** when MELCs are tagged:
  - Competency coverage breakdown
  - Performance by competency
  - Which MELCs students struggle with
  - Mastery levels per MELC

**Data passed:**
```kotlin
questionMelcMappings: Map<Int, MelcEntity>
// Maps question number → MELC entity
```

**Features:**
- AI-powered insights based on MELC performance
- Identifies competency gaps
- Helps teachers see which learning objectives need reteaching

---

### 5. **Database Storage**
**Location:** SQLite database (`exam_database`)

**Tables:**
- **`melcs` table:** Stores all 264 MELC competencies
  - `id`: Primary key
  - `code`: MELC code (e.g., "M7AL-IIa-1")
  - `description`: Full competency text
  - `subject`: Subject name
  - `gradeLevel`: Grade level (7, 8, 9, 10)
  - `quarter`: Quarter number (1, 2, 3, 4)

- **`question_melc_mappings` table:** Links questions to MELCs
  - `examId`: Foreign key to exams
  - `questionNumber`: Question number (1-50)
  - `melcId`: Foreign key to melcs table

**Query Methods:**
```kotlin
// Get all MELCs
dao.getAllMelcs(): Flow<List<MelcEntity>>

// Get mappings for an exam
dao.getQuestionMelcMappings(examId: Long): Map<Int, MelcEntity>

// Save mappings
dao.insertQuestionMelcMappings(mappings: List<QuestionMelcMapping>)
```

---

## 🔄 Data Flow

### Tagging a MELC:
```
1. User taps "Tag MELC" button
   ↓
2. EditKeyScreen calls onMelcClick()
   ↓
3. MelcSelectorDialog opens
   ↓
4. User searches/filters and selects MELC
   ↓
5. onSelect(melc) callback updates local state
   ↓
6. User taps Save (✓) button in header
   ↓
7. onSaveMelcMappings() called in MainActivity
   ↓
8. ViewModel saves to database
   ↓
9. Database stores mapping
   ↓
10. MELC appears in green badge on Edit screen
```

### Loading Existing MELCs:
```
1. User opens Edit Answer Key
   ↓
2. MainActivity loads current mappings:
   currentMelcMappings = viewModel.getQuestionMelcMappings(examId)
   ↓
3. Passed to EditKeyScreen as parameter
   ↓
4. Each question shows MELC if mapped
   ↓
5. Button displays as green with checkmark
   ↓
6. Green badge shows MELC code below
```

---

## 🎨 Visual Indicators

### MELC Status Colors:
| Status | Color | Icon | Text |
|--------|-------|------|------|
| **Not tagged** | Light blue gradient | 📖 Book | "Tag MELC" |
| **Tagged** | Green gradient | ✓ Checkmark | "MELC Tagged ✓" |
| **MELC code badge** | Light green background | - | MELC code text |

### MELC Chips (in selector):
| Type | Color | Example |
|------|-------|---------|
| **Subject** | Blue | Math, Science, English |
| **Grade Level** | Purple | Grade 7, Grade 8 |
| **Quarter** | Orange | Q1, Q2, Q3, Q4 |

---

## 📊 MELC Analytics Features

### Current Analytics (Smart Dashboard):
- **Competency Coverage:** Shows which MELCs are covered in the exam
- **Performance by MELC:** Average scores for questions tagged to each MELC
- **Mastery Analysis:** Identifies which competencies students have mastered
- **Gap Analysis:** Shows which MELCs need reteaching

### Future Enhancements:
- [ ] MELC coverage report (PDF export)
- [ ] Track MELC mastery across multiple exams
- [ ] Suggest questions for under-covered MELCs
- [ ] Quarter-by-quarter competency tracking
- [ ] Class-level MELC portfolio

---

## 🔧 Technical Implementation

### Key Files:
```
app/src/main/java/com/examscanner/premium/
├── data/
│   ├── MelcEntity.kt              # MELC data model
│   ├── QuestionMelcMapping.kt     # Mapping data model
│   ├── SampleMelcsData.kt         # 264 MELC definitions
│   ├── AppDatabase.kt             # Room database with MELC DAOs
│   └── ExamRepository.kt          # MELC query/save methods
├── ui/screens/
│   ├── EditKeyScreen.kt           # Shows MELC tags, save button
│   ├── MelcSelectorDialog.kt      # MELC picker with search
│   ├── ExamDetailScreen.kt        # Menu item to map MELCs
│   └── SmartDashboardMVP.kt       # MELC analytics
└── ExamScannerApplication.kt      # Seeds MELCs on first launch
```

### Database Schema:
```sql
-- MELCs table
CREATE TABLE melcs (
    id INTEGER PRIMARY KEY,
    code TEXT NOT NULL,
    description TEXT NOT NULL,
    subject TEXT NOT NULL,
    gradeLevel INTEGER NOT NULL,
    quarter INTEGER NOT NULL
);

-- Question-MELC mappings
CREATE TABLE question_melc_mappings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    examId INTEGER NOT NULL,
    questionNumber INTEGER NOT NULL,
    melcId INTEGER NOT NULL,
    FOREIGN KEY(examId) REFERENCES exams(id) ON DELETE CASCADE,
    FOREIGN KEY(melcId) REFERENCES melcs(id),
    UNIQUE(examId, questionNumber)
);
```

---

## ✨ User Benefits

### For Teachers:
1. **Standards Alignment:** Ensures exams align with DepEd MELCs
2. **Competency Tracking:** See which learning objectives are covered
3. **Data-Driven Teaching:** Identify which competencies need reteaching
4. **Report Generation:** Export MELC coverage for lesson planning
5. **Compliance:** Meet DepEd requirements for competency-based assessment

### Competitive Advantage vs ZipGrade:
| Feature | Exam Scanner | ZipGrade |
|---------|--------------|----------|
| **MELC Integration** | ✅ Built-in | ❌ No |
| **Filipino Context** | ✅ DepEd-aligned | ❌ US-focused |
| **Competency Analytics** | ✅ AI-powered | ❌ Basic stats |
| **Standards Tracking** | ✅ Per question | ❌ None |

---

## 🚀 Future MELC Features (Roadmap)

### Phase 1: Current (✅ Completed)
- [x] Tag MELCs to questions
- [x] Visual indicators (badges, colors)
- [x] Search and filter MELCs
- [x] Save/load mappings
- [x] Show in analytics

### Phase 2: Enhanced Analytics
- [ ] MELC mastery heat map
- [ ] Track student progress across MELCs over time
- [ ] Identify learning gaps by MELC
- [ ] Compare class performance by competency

### Phase 3: Reporting
- [ ] PDF export with MELC breakdown
- [ ] Quarter summary reports
- [ ] Competency portfolio for each student
- [ ] Share reports with school admin

### Phase 4: AI Recommendations
- [ ] Suggest MELCs based on exam content
- [ ] Auto-detect competencies from question text
- [ ] Recommend practice questions for weak MELCs
- [ ] Generate MELC-aligned test banks

---

## 📝 MELC Data Sample

**Example MELC:**
```kotlin
MelcEntity(
    id = 1,
    code = "M7AL-IIa-1",
    description = "Represents real-life situations using rational algebraic expressions",
    subject = "Mathematics",
    gradeLevel = 7,
    quarter = 2
)
```

**Total MELCs in Database:** 264 competencies covering:
- Math (Grades 7-10)
- Science (Grades 7-10)
- English (Grades 7-10)
- Filipino (Grades 7-10)

---

## 🎯 Summary

**MELC tags appear in:**
1. ✅ Edit Answer Key screen (green badge + button)
2. ✅ MELC Selector dialog (full competency list)
3. ✅ Exam Detail menu (quick access)
4. ✅ Analytics Dashboard (performance by competency)
5. ✅ Database (persistent storage)

**Key Feature:** Once a MELC is tagged to a question, it's:
- Saved to database ✓
- Displayed with green badge ✓
- Included in analytics ✓
- Available across app sessions ✓
- Visible on any screen showing that exam ✓

**Unique Selling Point:** No other exam scanner app in the Philippines has DepEd MELC integration built-in! 🇵🇭
