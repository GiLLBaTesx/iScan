#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "📦 iScan Backup & Restore Test"
echo "═══════════════════════════════════════════════"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

APP_PACKAGE="com.examscanner.premium"
DB_NAME="exam_scanner_database"
BACKUP_DIR="/storage/emulated/0/Android/data/$APP_PACKAGE/files/backups"

echo "${BLUE}Step 1: Check current database${NC}"
DB_SIZE=$(adb shell "run-as $APP_PACKAGE stat -c%s /data/data/$APP_PACKAGE/databases/$DB_NAME 2>/dev/null")
if [ -n "$DB_SIZE" ]; then
    echo "✓ Database found: $(($DB_SIZE / 1024)) KB"
else
    echo "${RED}✗ Database not found!${NC}"
    exit 1
fi
echo ""

echo "${BLUE}Step 2: List existing backups${NC}"
BACKUP_COUNT=$(adb shell "ls $BACKUP_DIR/*.db 2>/dev/null | wc -l")
echo "✓ Found $BACKUP_COUNT backup(s)"
adb shell "ls -lh $BACKUP_DIR/*.db 2>/dev/null | awk '{print \"  - \" \$9 \" (\" \$5 \")\"}'"
echo ""

echo "${BLUE}Step 3: Get database record counts (BEFORE)${NC}"
echo "Getting exam count..."
BEFORE_EXAM_COUNT=$(adb shell "run-as $APP_PACKAGE sqlite3 /data/data/$APP_PACKAGE/databases/$DB_NAME 'SELECT COUNT(*) FROM exams;' 2>/dev/null")
echo "Getting folder count..."
BEFORE_FOLDER_COUNT=$(adb shell "run-as $APP_PACKAGE sqlite3 /data/data/$APP_PACKAGE/databases/$DB_NAME 'SELECT COUNT(*) FROM subject_folders;' 2>/dev/null")
echo "Getting answer key count..."
BEFORE_KEY_COUNT=$(adb shell "run-as $APP_PACKAGE sqlite3 /data/data/$APP_PACKAGE/databases/$DB_NAME 'SELECT COUNT(*) FROM answer_keys;' 2>/dev/null")

echo "${GREEN}Current database state:${NC}"
echo "  • Exams: $BEFORE_EXAM_COUNT"
echo "  • Folders: $BEFORE_FOLDER_COUNT"
echo "  • Answer Keys: $BEFORE_KEY_COUNT"
echo ""

echo "${YELLOW}═══════════════════════════════════════════════${NC}"
echo "${YELLOW}Manual Test Required:${NC}"
echo "${YELLOW}1. Open iScan app${NC}"
echo "${YELLOW}2. Go to Settings → Manage Backups${NC}"
echo "${YELLOW}3. Tap 'Create Backup' button${NC}"
echo "${YELLOW}4. Wait for success message${NC}"
echo "${YELLOW}5. Modify some data (add/delete a folder)${NC}"
echo "${YELLOW}6. Tap ⋮ on the backup you created${NC}"
echo "${YELLOW}7. Select 'Restore'${NC}"
echo "${YELLOW}8. Confirm the restore${NC}"
echo "${YELLOW}9. Close and restart the app${NC}"
echo "${YELLOW}10. Verify your data was restored${NC}"
echo "${YELLOW}═══════════════════════════════════════════════${NC}"
echo ""

read -p "Press Enter after completing the restore test..."
echo ""

echo "${BLUE}Step 4: Get database record counts (AFTER)${NC}"
echo "Getting exam count..."
AFTER_EXAM_COUNT=$(adb shell "run-as $APP_PACKAGE sqlite3 /data/data/$APP_PACKAGE/databases/$DB_NAME 'SELECT COUNT(*) FROM exams;' 2>/dev/null")
echo "Getting folder count..."
AFTER_FOLDER_COUNT=$(adb shell "run-as $APP_PACKAGE sqlite3 /data/data/$APP_PACKAGE/databases/$DB_NAME 'SELECT COUNT(*) FROM subject_folders;' 2>/dev/null")
echo "Getting answer key count..."
AFTER_KEY_COUNT=$(adb shell "run-as $APP_PACKAGE sqlite3 /data/data/$APP_PACKAGE/databases/$DB_NAME 'SELECT COUNT(*) FROM answer_keys;' 2>/dev/null")

echo "${GREEN}After restore:${NC}"
echo "  • Exams: $AFTER_EXAM_COUNT"
echo "  • Folders: $AFTER_FOLDER_COUNT"
echo "  • Answer Keys: $AFTER_KEY_COUNT"
echo ""

echo "${BLUE}Step 5: Check for safety backup${NC}"
SAFETY_BACKUP=$(adb shell "ls $BACKUP_DIR/backup_before_restore_*.db 2>/dev/null | tail -1")
if [ -n "$SAFETY_BACKUP" ]; then
    echo "${GREEN}✓ Safety backup created:${NC}"
    adb shell "ls -lh $SAFETY_BACKUP 2>/dev/null | awk '{print \"  \" \$9 \" (\" \$5 \")\"}'"
else
    echo "${YELLOW}⚠ No safety backup found (may not have restored yet)${NC}"
fi
echo ""

echo "═══════════════════════════════════════════════"
echo "${GREEN}✓ Test Complete${NC}"
echo "═══════════════════════════════════════════════"
echo ""
echo "Expected behavior:"
echo "  1. Backup creates .db file in $BACKUP_DIR"
echo "  2. Restore creates backup_before_restore_*.db safety copy"
echo "  3. Database counts should match backup after app restart"
echo "  4. App shows 'RESTART the app' message"
echo ""
