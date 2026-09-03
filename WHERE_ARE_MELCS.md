# 📍 Where Are the MELCs? (Simple Answer)

## Quick Answer

**MELCs are in 3 places:**

### 1. **In the Database** (Backend)
📁 File: `/app/src/main/java/com/examscanner/premium/data/SampleMelcsData.kt`

135+ DepEd competencies loaded automatically when you first open the app.

---

### 2. **Map Questions to MELCs** (New Feature!) ✅
🎯 **How to access:**
```
Home → Subject Folder → Select Exam → Tap ⋮ menu → "Map questions to MELCs"
```

**What it does:**
- Tag each question with a DepEd competency
- Search through 135+ MELCs
- Save mappings for analysis

**Example:** "Question 5 tests **M7NS-Ia-1** (Rational Numbers)"

---

### 3. **View MELC Insights** (After Mapping + Scanning)
🤖 **How to access:**
```
Home → Subject Folder → Select Exam → "See Insights" button → "What to Reteach Now" tab
```

**What it shows:**
- Which competencies students struggle with
- AI suggestions for reteaching
- Student groupings by MELC performance

---

## Visual Map

```
YOUR APP
│
├── 📂 MELC Database (Hidden - Backend)
│   └── SampleMelcsData.kt
│       • 135+ MELCs
│       • Grades 7-10
│       • Math, English, Science, Filipino, AP
│
├── 🎯 MELC Mapping (New!) ← YOU MAP HERE
│   └── Exam Detail → ⋮ Menu → "Map questions to MELCs"
│       • Tag Question 1 → M7NS-Ia-1
│       • Tag Question 2 → M7AL-If-1
│       • Tag Question 3 → S7MT-Ia-1
│       • etc.
│
└── 🤖 MELC Analysis ← YOU VIEW HERE
    └── Exam Detail → "See Insights" → "What to Reteach Now"
        • "45% struggle with Rational Numbers"
        • "Reteach M7NS-Ia-1 to 12 students"
        • AI intervention suggestions
```

---

## Step-by-Step: Using MELCs

### Before Scanning Students:

**Step 1:** Create exam
**Step 2:** Set answer key
**Step 3:** **Map questions to MELCs** ← NEW!
- Go to Exam Detail
- Tap ⋮ menu (top right)
- Select "Map questions to MELCs"
- For each question, pick which DepEd competency it tests
- Tap SAVE

### After Scanning Students:

**Step 4:** Scan bubble sheets as usual
**Step 5:** **View MELC insights**
- Go to Exam Detail
- Tap "See Insights" button
- Check "What to Reteach Now" tab
- See which competencies need reteaching

---

## Example MELC Codes

### Math Grade 7:
- **M7NS-Ia-1:** Describe integers, rational numbers, irrational numbers
- **M7AL-IIa-1:** Solve linear equations and inequalities

### Science Grade 7:
- **S7MT-Ia-1:** Describe components of scientific investigation
- **S7LT-Ia-1:** Describe levels of biological organization

### English Grade 7:
- **E7RC-Ia-1:** Recognize types of text (narrative, expository)
- **E7WC-Ia-1:** Use the writing process to develop a paragraph

---

## Why Use MELCs?

### Without MELCs:
❌ "Your class average is 65%" (not helpful)
❌ "Question 5 was hard" (so what?)

### With MELCs:
✅ "45% of students struggle with **Rational Numbers**"
✅ "Reteach **Solving Linear Equations** to 12 specific students"
✅ "Your class has mastered **Integers** (85% proficiency)"

**More actionable. More aligned with DepEd curriculum.**

---

## Summary

**Where are MELCs?**
1. Database: ✅ Loaded automatically (135+ MELCs)
2. Mapping UI: ✅ Exam → ⋮ Menu → "Map questions to MELCs"
3. Analysis: ✅ Exam → "See Insights" → "What to Reteach Now"

**All wired up and ready to use!** 🎉
