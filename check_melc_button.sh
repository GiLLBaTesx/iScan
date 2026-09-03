#!/bin/bash

echo "🔍 MELC Button Diagnostic Tool"
echo "================================"
echo ""

# Clear logs
adb logcat -c
echo "✅ Cleared logs"
echo ""

# Launch app
echo "🚀 Launching app..."
adb shell am start -n com.examscanner.premium/.MainActivity
sleep 3

echo ""
echo "📱 Now do the following:"
echo "   1. Open any exam"
echo "   2. Tap 'Edit Answer Key'"
echo "   3. Wait 2 seconds"
echo "   4. Press ENTER here"
echo ""
read -p "Press ENTER after you've opened Edit Answer Key screen..."

echo ""
echo "📊 Checking logs..."
echo "================================"

# Get logs
adb logcat -d | grep -E "ExamScannerApp|EditKeyScreen" > /tmp/melc_logs.txt

if [ -s /tmp/melc_logs.txt ]; then
    echo "✅ Found logs:"
    cat /tmp/melc_logs.txt
else
    echo "❌ No logs found. App might not be running or logging not working."
    echo ""
    echo "Last 20 app logs:"
    adb logcat -d | grep "examscanner" | tail -20
fi

echo ""
echo "================================"
echo "🔍 MELCs in database check..."
adb shell "run-as com.examscanner.premium sqlite3 /data/data/com.examscanner.premium/databases/exam_database 'SELECT COUNT(*) FROM melcs;'" 2>/dev/null || echo "⚠️  Cannot access database (normal on some devices)"

echo ""
echo "Done! If you see 'Available MELCs: 0', that's the problem."
