# Scanner Accuracy Testing Guide

## 🎯 Overview

Test the bubble sheet scanner's accuracy with automated test cases. The system generates answer sheets with known answers, you scan them, and it calculates detection accuracy.

---

## 🚀 How to Access

1. **Open the app**
2. **Tap the ⋮ menu** (three dots) in the top-right corner
3. **Select "Test Scanner Accuracy"**

---

## 📋 Test Cases Available

### 1. **All A Pattern**
- All 20 questions answered with "A"
- Tests: Consistent single-column detection
- **Use Case**: Baseline accuracy test

### 2. **All B Pattern**
- All 20 questions answered with "B"
- Tests: Second column detection consistency
- **Use Case**: Verify non-first-column accuracy

### 3. **Alternating Pattern**
- Alternates between "A" and "B" (A, B, A, B, ...)
- Tests: Switching between adjacent columns
- **Use Case**: Column switching accuracy

### 4. **Random Pattern**
- Mixed answers: A, B, C, D across questions
- Tests: Real-world scenario with varied responses
- **Use Case**: Most realistic test

### 5. **Diagonal Pattern**
- Cycles through A → B → C → D → E
- Tests: Full option range detection
- **Use Case**: Comprehensive coverage of all bubbles

---

## 🧪 Testing Workflow

### Step 1: Generate Test Sheet
1. Select a test case (e.g., "Random Pattern")
2. Tap **"GENERATE PDF"** button
3. Wait for generation (~1-2 seconds)
4. PDF will automatically open

### Step 2: Display Test Sheet
**Option A - Print it:**
- Print the generated PDF
- Use black ink for best results
- Standard A4/Letter paper

**Option B - Display on another device:**
- Open PDF on tablet, laptop, or another phone
- Display at full brightness
- Avoid screen glare

### Step 3: Scan the Sheet
1. Tap **"SCAN NOW"** button
2. Position camera over the test sheet
3. Ensure good lighting
4. Keep camera steady
5. Capture the image

### Step 4: View Results
The system automatically:
- Compares detected answers vs expected answers
- Calculates accuracy percentage
- Shows question-by-question breakdown
- Provides performance rating

---

## 📊 Understanding Results

### Accuracy Report Format:
```
═══════════════════════════════════════
    SCANNER ACCURACY TEST REPORT
═══════════════════════════════════════

Overall Results:
  Total Questions: 20
  ✓ Correct Detections: 18
  ✗ Incorrect Detections: 1
  ○ Missed Detections: 1

  ACCURACY: 90.00%

═══════════════════════════════════════

Detailed Results:

  Q1: ✓ Expected=[A] Detected=[A]
  Q2: ✓ Expected=[B] Detected=[B]
  Q3: ✗ Expected=[C] Detected=[D]
  Q4: ○ Expected=[A] Detected=[NOT DETECTED]
  ...
```

### Result Indicators:
- **✓** = Correct detection (matches expected)
- **✗** = Incorrect detection (wrong answer detected)
- **○** = Missed detection (no answer detected)

### Performance Ratings:
- **95-100%** = ✓ EXCELLENT - Scanner is highly accurate!
- **85-94%** = ✓ GOOD - Scanner performance is acceptable
- **70-84%** = ⚠ FAIR - Scanner needs improvement
- **Below 70%** = ✗ POOR - Scanner requires significant fixes

---

## 🔍 What Gets Tested

### Detection Capabilities:
1. **Bubble Recognition** - Can it find filled circles?
2. **Position Accuracy** - Does it map to correct question/option?
3. **Fill Detection** - Can it distinguish filled vs empty?
4. **Edge Cases** - Partially filled, multiple marks, etc.

### Not Currently Tested:
- QR code scanning (not yet implemented)
- Student ID/Name extraction (ML Kit text recognition)
- Misaligned sheets
- Damaged/crumpled paper

---

## 💡 Tips for Accurate Testing

### Optimal Conditions:
✅ **Good lighting** - Natural daylight or bright indoor light
✅ **Steady camera** - Hold phone still or use stand
✅ **Clear print** - Dark, crisp bubbles
✅ **Flat sheet** - No folds or wrinkles
✅ **Full frame** - Entire sheet visible in camera

### Avoid:
❌ Shadows across the sheet
❌ Glare from screen/glossy paper
❌ Blurry images (shaky hands)
❌ Partial sheet in frame
❌ Poor print quality (faded ink)

---

## 🐛 Troubleshooting

### Low Accuracy (<85%)?

**Check these factors:**

1. **Image Quality**
   - Is the image sharp and clear?
   - Try better lighting
   - Hold camera steadier

2. **Print Quality**
   - Are bubbles dark enough?
   - Try reprinting with higher contrast
   - Use black ink, not gray

3. **Camera Distance**
   - Too close? Move back
   - Too far? Move closer
   - Entire sheet should fill 70-80% of frame

4. **Sheet Condition**
   - Is paper wrinkled?
   - Are bubbles smudged?
   - Try a fresh print

### App Crashes?
- Ensure storage permission is granted
- Check available storage space (need ~10MB)
- Try restarting the app

### PDF Won't Open?
- Install a PDF reader (Adobe, Google PDF Viewer)
- Check external storage permission
- File location: `/Android/data/com.examscanner.premium/files/test_sheets/`

---

## 🎯 Recommended Testing Sequence

For comprehensive validation:

1. **Start Simple**: "All A" test
   - Should get 95-100% accuracy
   - If this fails, basic detection is broken

2. **Test Column Variation**: "All B" test
   - Should also get 95-100%
   - Validates non-first-column accuracy

3. **Test Switching**: "Alternating" test
   - Should get 90-100%
   - Validates column transitions

4. **Real World**: "Random Pattern" test
   - Should get 85-95%
   - Most realistic scenario

5. **Full Range**: "Diagonal" test
   - Should get 85-95%
   - Tests all bubble positions (A through E)

**Expected Overall Performance: 85-95% accuracy across all tests**

---

## 📈 Expected Accuracy Targets

### Current Implementation Status:
⚠️ **Scanner is using ML Kit Text Recognition** - NOT optimized bubble detection

**Current Method:**
- ML Kit scans for text/numbers
- NOT specifically designed for bubble sheets
- Accuracy will be lower than specialized algorithms

**Expected Current Performance:**
- Simple patterns (All A/B): 60-80%
- Complex patterns (Random): 40-70%
- With optimal conditions: Up to 80%

### Future Improvements (Planned):
- OpenCV bubble detection: 95-98% target
- Image preprocessing (threshold, contour detection)
- Bubble filling percentage calculation
- Multi-mark detection

---

## 🔧 Technical Details

### Test Sheet Format:
- **Size**: A4 (595×842 pixels in PDF)
- **Questions**: 20 by default
- **Options**: A, B, C, D, E (5 choices)
- **Bubble Size**: 10-pixel radius circles
- **Spacing**: 40 pixels between bubbles
- **Filled Bubbles**: Solid black circle

### File Locations:
- **Generated PDFs**: `/storage/emulated/0/Android/data/com.examscanner.premium/files/test_sheets/`
- **Naming**: `test_sheet_<timestamp>.pdf`
- **Size**: ~50-100KB per PDF

### Code Components:
- **Generator**: `ScannerTestUtility.kt`
- **UI**: `ScannerTestScreen.kt`
- **Integration**: `MainActivity.kt` (route: `scanner_test`)

---

## 📝 Logging Results

### Manual Log Template:
```
Test Date: [DATE]
Test Case: [All A / All B / Alternating / Random / Diagonal]
Lighting: [Indoor / Outdoor / Mixed]
Print Method: [Printed / Screen Display]
Accuracy: [X%]
Correct: [X/20]
Incorrect: [X/20]
Missed: [X/20]
Notes: [Any observations]
```

Keep a testing log to track improvements over time!

---

## 🎓 What This Tells You

### High Accuracy (>90%):
✅ Scanner is production-ready
✅ Can be used for real assessments
✅ Minimal manual correction needed

### Medium Accuracy (70-90%):
⚠️ Scanner works but needs improvement
⚠️ Acceptable for testing/development
⚠️ Some manual verification required

### Low Accuracy (<70%):
❌ Scanner needs significant work
❌ Not ready for real use
❌ Investigate detection algorithm

---

## 🚀 Next Steps After Testing

Based on results:

**If accuracy is good (>85%):**
- Test with real printed sheets
- Try various printers/paper types
- Test in different lighting conditions
- Add more test cases (50, 100 questions)

**If accuracy needs improvement:**
- Review `BubbleSheetProcessor.kt` algorithm
- Consider implementing OpenCV
- Add image preprocessing
- Adjust detection thresholds

---

## 📞 Support

For questions or issues:
1. Check troubleshooting section above
2. Review implementation in `testing/` package
3. Check logs in Android Studio Logcat

---

**Happy Testing! 🎉**

The more you test, the more confident you can be in the scanner's reliability for real-world use.
