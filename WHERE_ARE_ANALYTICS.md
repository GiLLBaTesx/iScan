# 📊 Where Are the Analytics? (Quick Guide)

## 🎯 Simple Answer

Your analytics are in the **"See Insights"** button on the Exam Detail screen!

---

## 📱 How to Access Analytics

### Step-by-Step:
```
1. Open app
2. Home → Tap a subject folder
3. Tap an exam (one with students scanned)
4. See the orange "See Insights" button
5. Tap it → Full-screen SmartDashboardMVP opens
6. Choose from 3 tabs:
   - RETEACH (Competency-based priorities)
   - ANALYSIS (Psychometric item analysis)
   - GROUPS (Intervention student groups)
```

---

## 🖼️ Visual Guide

### Current Screen Layout:

```
┌─────────────────────────────────────────────┐
│  ← Math Midterm Exam                    ⋮  │ ← Top bar
├─────────────────────────────────────────────┤
│                                             │
│  ┌───────────────────────────────────────┐ │
│  │    30         │        72%            │ │ ← Stats card
│  │  Students     │   Class Average       │ │
│  └───────────────────────────────────────┘ │
│                                             │
│  ┌─────────────┐  ┌──────────────────────┐│
│  │ 📷 SCAN MORE│  │ 🤖 SEE INSIGHTS      ││ ← Buttons
│  └─────────────┘  │    AI powered        ││
│                   └──────────────────────┘│
│                        ↑                    │
│                   TAP THIS!                 │
│                                             │
│  ┌───────────────────────────────────────┐ │
│  │ #1  Juan dela Cruz          95%  🟢  │ │ ← Student list
│  ├───────────────────────────────────────┤ │
│  │ #2  Maria Santos            88%  🟢  │ │
│  ├───────────────────────────────────────┤ │
│  │ #3  Pedro Reyes             72%  🟡  │ │
│  └───────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## 🤖 What Opens: SmartDashboardMVP

When you tap "See Insights," you get a **full-screen overlay** with 3 tabs:

### Tab 1: RETEACH (Competency-Based Analytics)
```
┌─────────────────────────────────────────────┐
│  ← 🤖 Smart Report - Math Midterm       📄 │
├─────────────────────────────────────────────┤
│  [RETEACH] [ANALYSIS] [GROUPS]              │
├─────────────────────────────────────────────┤
│                                             │
│  💡 AI-Suggested Action Plan                │
│  Top 3 competencies needing intervention    │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │ 🔴 URGENT - Reteach This Week        │  │
│  │      35% class mastery               │  │
│  │                                      │  │
│  │ M7NS-Ia-1: Rational Numbers          │  │
│  │ Mathematics · Grade 7 · Q1           │  │
│  │                                      │  │
│  │ Questions: 5, 7, 12                  │  │
│  │ Students: 18                         │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │ 🟡 IMPORTANT - Next 2 Weeks          │  │
│  │      55% class mastery               │  │
│  │                                      │  │
│  │ M7AL-If-1: Algebraic Expressions     │  │
│  │ ... (details)                        │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### Tab 2: ANALYSIS (Psychometric Analytics)
```
┌─────────────────────────────────────────────┐
│  [RETEACH] [ANALYSIS] [GROUPS]              │
├─────────────────────────────────────────────┤
│                                             │
│  Understanding the Indices                  │
│  • Difficulty: % who got it right           │
│  • Discrimination: Separates high/low       │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │ 🟡 Q5  Key: B                 12/30  │  │
│  │                                      │  │
│  │ Difficulty     0.40  MODERATE 🟡     │  │
│  │ Discrimination 0.35  GOOD     👍     │  │
│  │                                      │  │
│  │ Answer Distribution:                 │  │
│  │ A ████░░░░░░░░░░ 8 (27%)            │  │
│  │ B ████████████░░ 12 (40%) ← KEY     │  │
│  │ C ████░░░░░░░░░░ 5 (17%)            │  │
│  │ D ████░░░░░░░░░░ 5 (17%)            │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### Tab 3: GROUPS (Intervention Groups)
```
┌─────────────────────────────────────────────┐
│  [RETEACH] [ANALYSIS] [GROUPS]              │
├─────────────────────────────────────────────┤
│                                             │
│  👥 Intervention Groups                     │
│  Students grouped by competency gaps        │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │ M7NS-Ia-1: Rational Numbers      35% │  │
│  │ Describe integers, rational numbers  │  │
│  │                                      │  │
│  │ Students needing support (18):       │  │
│  │ • Juan dela Cruz - 33%               │  │
│  │ • Maria Santos - 17%                 │  │
│  │ • Pedro Reyes - 50%                  │  │
│  │ ... (15 more)                        │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## 📂 Where Is CompetencyAnalysisScreen?

**Status:** The file exists but is **NOT currently wired** to navigation.

**Location:** `/app/src/main/java/com/examscanner/premium/ui/screens/CompetencyAnalysisScreen.kt`

**What it shows:**
- 2 tabs: BY COMPETENCY / BY QUESTION
- Detailed MELC mastery % with progress bars
- Overall performance level (Advanced/Proficient/etc.)

**Why it's not accessible:**
- It was replaced/integrated into SmartDashboardMVP
- The RETEACH tab in SmartDashboard does the same thing (competency priorities)
- You could add it back as a separate menu item if you want both views

---

## 🔄 How Analytics Flow Works

### Data Path:
```
Student scans answer sheet
    ↓
Answers saved to database
    ↓
ExamDetailScreen loads:
├─ Exam data
├─ Answer keys
├─ Student answers
├─ Question-MELC mappings
    ↓
"See Insights" button appears (if students exist)
    ↓
User taps button
    ↓
showAnalysis = true
    ↓
SmartDashboardMVP renders as full-screen overlay
    ↓
Three tabs calculated:
├─ Tab 1: RETEACH (calculateReteachPriorities)
├─ Tab 2: ANALYSIS (calculateItemAnalysisWithIndices)
└─ Tab 3: GROUPS (calculateInterventionGroups)
```

---

## 💻 Code Location

### ExamDetailScreen.kt (Lines 290-310):
```kotlin
// "See Insights" Button
Button(
    onClick = { showAnalysis = true },
    modifier = Modifier
        .weight(1.5f)
        .height(64.dp),
    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
    shape = RoundedCornerShape(12.dp)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "See Insights",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AI powered",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
```

### ExamDetailScreen.kt (Lines 340-352):
```kotlin
// Full-screen AI Analysis overlay
if (showAnalysis && hasStudents) {
    val studentEntities = students.map { it.student }
    SmartDashboardMVP(
        examId = exam.id,
        examName = exam.name,
        answerKeys = answerKeys,
        studentAnswers = studentAnswers,
        students = studentEntities,
        questionMelcMappings = questionMelcMappings,
        onExportPDF = { /* TODO */ },
        onBack = { showAnalysis = false }
    )
}
```

---

## 🎯 Summary

### Where Analytics Are:
✅ **Location:** Exam Detail → "See Insights" button → SmartDashboardMVP

### What Analytics Include:
✅ **Tab 1:** Competency-based reteach priorities (MELC mastery %)
✅ **Tab 2:** Psychometric item analysis (difficulty/discrimination)
✅ **Tab 3:** Auto-grouped intervention students by weak MELC

### How to Access:
1. Open exam with scanned students
2. Tap orange **"See Insights"** button
3. Choose RETEACH / ANALYSIS / GROUPS tabs

### Files Involved:
- **ExamDetailScreen.kt** - Entry point (button + overlay)
- **SmartDashboardMVP.kt** - Main analytics (960 lines, 3 tabs)
- **CompetencyAnalysisScreen.kt** - Alternative view (580 lines, not wired)

---

## 🚀 Quick Test

**To see analytics right now:**

1. Open your app
2. Go to a subject folder
3. Select an exam that has students scanned
4. Look for the **orange "See Insights"** button below the stats
5. Tap it → Full analytics open!

**If you don't see the button:**
- Make sure the exam has scanned students (not empty)
- The button only shows if `hasStudents = true`

---

## 💡 Recommendation

Consider making analytics MORE discoverable:

### Option 1: Add Menu Item
```
Exam Detail → ⋮ Menu → "View Analytics"
```

### Option 2: Add Tab
```
Exam Detail → Tabs: [Results] [📊 Analytics]
```

### Option 3: Keep Current + Badge
```
"See Insights" button with badge showing:
"🤖 See Insights (3 urgent MELCs)"
```

Currently it's a bit hidden since users might not notice the orange button. Making it more prominent could increase usage!

---

**The analytics ARE there - just tap "See Insights"!** 🎉
