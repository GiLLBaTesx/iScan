# 🎨 ScanKey Design Recreation Plan

## Design Analysis

### Color Palette
**Background:**
- Primary: `#0F1419` (Very dark blue-gray)
- Surface: `#1A1F26` (Dark card background)
- Card: `#232931` (Elevated cards)

**Accent:**
- Coral/Salmon: `#FF6B6B` (Primary buttons, badges)
- Green: `#51CF66` (Success, correct answers)
- Blue: `#4DABF7` (Info, secondary actions)
- Orange: `#FFB84D` (Warnings, draft status)

**Answer Colors:**
- A: Red/Coral `#FF6B6B`
- B: Blue `#4DABF7`
- C: Orange `#FFB84D`
- D: Deep Red `#FF5252`
- E: Purple `#9775FA`

### Key Design Features

#### 1. **Exam List Screen**
- Dark header with workspace name
- "LIVE SYNC" badge (green)
- Coral "+" floating button
- Status tabs: "All Exams, Midterms, Quizzes, Archived"
- Large stat cards showing: Graded count, Active, Accuracy
- Exam cards with:
  - Subject name + average %
  - Questions count + sheets scanned
  - Progress bar with percentage
  - Action buttons (Quick Scan, View Report)
  - Draft key status badge
  - Unanswered questions counter

#### 2. **Live Scanner Screen**
- Real-time OMR detection overlay
- Student info header (name, ID, form, score)
- OMR LOCK percentage indicator
- Floating action buttons (lightning, grid, close)
- Answer bubbles overlaid on camera view
- Bottom stats: Correct, Alt, Wrong, Read %
- Large coral camera button
- "Manual Fix" and "Batch" buttons

#### 3. **Item Analysis Screen**
- Tabs: "Scores & Students" | "Item Analysis"
- Key selection with auto-calibration badge
- Per-question analysis:
  - Question number + Key
  - Correct percentage with health badge
  - Color-coded bars for each answer option
  - Distractor analysis
- Student roster with:
  - Student card with rank/score
  - Color-coded performance
  - Review badge for flagged students

#### 4. **Master Key Editor**
- Sheet template selector
- Metadata field chips (Name, Date, Class, Student ID)
- Default points configurator
- Answer matrix Q1-Q10 visible
- Multi-color answer buttons
- Points per question
- Primary/Alt answer indicators
- Proctor tips panel

---

## Implementation Plan

### Phase 1: Dark Theme Foundation
- [x] Create dark color palette
- [ ] Add dark theme toggle
- [ ] Update MaterialTheme
- [ ] Create dark glassmorphism components

### Phase 2: Main Screens Redesign
- [ ] Exam List (ScanKey style)
- [ ] Exam Detail (with stats cards)
- [ ] Edit Answer Key (dark mode)
- [ ] Live Scanner overlay

### Phase 3: Advanced Features
- [ ] Item analysis screen
- [ ] Master key editor with color answers
- [ ] Student roster with rankings
- [ ] Export/share functionality

### Phase 4: Polish
- [ ] Animations and transitions
- [ ] Loading states
- [ ] Empty states
- [ ] Error states

---

## Screen Breakdown

### 1. Exam List Screen (ScanKey Style)

```
┌─────────────────────────────────────┐
│ ScanKey ©                    🔔 👤 │
│ JAYSON SUYAT WORKSPACE             │
│                                     │
│ MON 10 AUG                          │
│ Exams                    LIVE SYNC  │
│                                     │
│ ┌─────────────────────────┐    ┌─┐ │
│ │ OMR-KEY ENGINE ⭐       │    │+│ │
│ │ New Exam Session        │    └─┘ │
│ │ Scan or upload answer keys…│     │
│ └─────────────────────────┘        │
│                                     │
│ LIVE RECOGNITION | OMR 100-KEY     │
│ [Sheet Preview]                    │
│ 4 Corner Anchors Locked | Inspect  │
│                                     │
│ ┌──────┬──────┬──────────────┐    │
│ │ 928  │  4   │   99.4%      │    │
│ │+12   │ 2    │ Critical     │    │
│ │today │cohort│              │    │
│ └──────┴──────┴──────────────┘    │
│                                     │
│ [All] [Midterms] [Quizzes] [Archive]│
│                                     │
│ Active Rosters & Keys  SORT BY DATE │
│                                     │
│ ┌─────────────────────────────────┐│
│ │ Science Midterm    Avg 48%  📄 🔒││
│ │ 20 Questions · 24/24 Scanned   ││
│ │ EVALUATION PROGRESS      96%   ││
│ │ [Quick Scan] [View Report]     ││
│ └─────────────────────────────────┘│
│                                     │
│ ┌─────────────────────────────────┐│
│ │ Physics Exam      Avg 94%  📄 🔒││
│ │ 100 Questions · 18 Sheets      ││
│ │ EVALUATION PROGRESS      45%   ││
│ │ [Continue Scan] [Analysis]     ││
│ └─────────────────────────────────┘│
│                                     │
│ ┌─────────────────────────────────┐│
│ │ Biology Finals    Draft Key    ││
│ │ 50 Questions · 10/50 Keyed     ││
│ │ Q10: 🔴  Q11: ?   40 Unanswered││
│ │ [Finish Answer Key] [Scan Key] ││
│ └─────────────────────────────────┘│
│                                     │
│ ┌─────────────────────────────────┐│
│ │ Calculus | Quiz #3  Avg 88%    ││
│ │ 15 Questions · 32 Sheets       ││
│ │ EVALUATION PROGRESS 100% ✓     ││
│ │ [Review 32 Papers] [Stats]     ││
│ └─────────────────────────────────┘│
│                                     │
│ [Exams] [Scan] [Analysis] [Students]│
└─────────────────────────────────────┘
```

### 2. Live Scanner Screen

```
┌─────────────────────────────────────┐
│ ←  📷 Live Scanner           👤    │
│                                     │
│ ● Renz Jhen A. Arcillas            │
│   ID: 10004 · Form 102-A     95%   │
│                         (38/40)     │
│                                     │
│ 🔧 OMR LOCK: 99.4%    ⚡ # ✕      │
│                                     │
│ ┌───────────────────────────────┐  │
│ │  ✕ Renz              95%      │  │
│ │                               │  │
│ │  [Camera View with OMR Overlay]│ │
│ │                               │  │
│ │ Q1 ⬚ Ⓐ Ⓑ Ⓒ Ⓓ         ⬚ ✓  │  │
│ │ Q2 ⬚ Ⓐ Ⓑ Ⓒ Ⓓ         ⬚ ✓  │  │
│ │ Q3 ⬚ Ⓐ Ⓑ Ⓒ Ⓓ         ⬚ ✓  │  │
│ │ Q4 ⬚ Ⓐ Ⓑ Ⓒ Ⓓ         ⬚ ✓  │  │
│ │                               │  │
│ │  L                         R  │  │
│ └───────────────────────────────┘  │
│                                     │
│ ● Aligned · Auto-capturing in 0.8s │
│                                     │
│ ● 38 Correct  ● 2 Alt  ● 0 Wrong   │
│ ◉ 100% Read                        │
│                                     │
│ [📝 Manual Fix]  [📷]  [Batch 8/42]│
│                                     │
└─────────────────────────────────────┘
```

### 3. Item Analysis Screen

```
┌─────────────────────────────────────┐
│ ← Science Midterm          COMPLETE │
│   20 Questions · 24/24 Scanned     │
│                                     │
│ [Edit Key] [Print] [Export CSV]    │
│                                     │
│ ┌──────────────────────────────────┐│
│ │ SCANNED  AVG SCORE    REVIEW    ││
│ │   24       58.4%      2 flags   ││
│ │  100%       %                    ││
│ └──────────────────────────────────┘│
│                                     │
│ [Scores & Students] [Item Analysis] │
│                                     │
│ ┌─────────────────────────────────┐│
│ │ 📋 OPTICAL KEY R6 V2    Sheet A ││
│ │ Q1-Q20 Automatic Parity Calib. ││
│ │                                 ││
│ │ 📊 Item Analysis Breakdown      ││
│ │ Shows how students performed    ││
│ │                                 ││
│ │ Q1  Key B        12% Correct 🔴││
│ │ ██████████████████████          ││
│ │ A 14   B 8→1   C 5    D 2      ││
│ │ ⚠️ High distractor bias on A    ││
│ │                                 ││
│ │ Q2  Key B      ● 75% Correct ✓ ││
│ │ ████████████████                ││
│ │ A 2   B 2   C 2   D 6→18       ││
│ │                                 ││
│ │ Q3  Key B     ● 100% Correct ✓ ││
│ │ ████████████████████████        ││
│ │ A 0   B 0→24   C 0    D 0      ││
│ └─────────────────────────────────┘│
│                                     │
│ 📊 Graded Student Roster  RANK ▼   │
│                                     │
│ ┌─────────────────────────────────┐│
│ │ 📄 Gabriela Silang         38%  ││
│ │    ID 10005 · 2 answered        ││
│ │    10003      Bubbles    REVIEW ││
│ └─────────────────────────────────┘│
│                                     │
│ ┌─────────────────────────────────┐│
│ │ 📄 Andres Bonifacio        43%  ││
│ │    ID 10004 · Auto-verified     ││
│ │                         28/60pts││
│ └─────────────────────────────────┘│
│                                     │
│ [🔍 Scan Additional Sheet]          │
│                                     │
│ [Exams] [Scan] [Analysis] [Students]│
└─────────────────────────────────────┘
```

---

## Component Library

### Cards
- **ExamCard**: Dark background, coral accents, progress bars
- **StatCard**: Large numbers, labels, icons
- **StudentCard**: Rank, name, score, status badge
- **QuestionCard**: Question number, key, performance bar

### Buttons
- **PrimaryButton**: Coral background, white text
- **SecondaryButton**: Dark outline, coral text
- **IconButton**: Circular, dark background
- **AnswerButton**: Color-coded (A-E), circular

### Badges
- **StatusBadge**: Live Sync (green), Draft (orange), Locked (blue)
- **PercentageBadge**: Color-coded by performance
- **ReviewBadge**: Red, indicates needs review

### Progress
- **LinearProgress**: Thin, coral color, rounded
- **CircularProgress**: Large, with percentage

---

## Next Steps

Would you like me to:
1. **Start with the dark theme** - Implement the color system
2. **Redesign Exam List** - ScanKey-style with stats and cards
3. **Create live scanner overlay** - Real-time OMR detection UI
4. **Build item analysis** - Per-question breakdown with colors
5. **Master key editor** - Multi-color answer matrix

Let me know which screen you want me to tackle first!
