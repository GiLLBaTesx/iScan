# ✅ Backup & Restore Verification Guide

## Current Status
- **App installed**: ✓
- **Database exists**: ✓ (416 KB)
- **Backup created**: ✓ backup_20260903_162309.db

## How to Test Restore Functionality

### Step 1: Check Current Data
1. Open **iScan** app
2. Note down how many **Subject Folders** you have
3. Note down how many **Exams** you have

### Step 2: Create a Backup
1. Tap **⋮ menu** (top right)
2. Tap **Settings**
3. Tap **Manage Backups**
4. Tap blue **"Create Backup"** button (bottom right)
5. Wait for green success message: "Backup created successfully!"
6. You should see your new backup in the list

### Step 3: Modify Data
1. Go back to home screen
2. **Delete one folder** OR **Create a new folder**
3. Confirm the change is visible

### Step 4: Restore the Backup
1. Go back to **Settings → Manage Backups**
2. Find the backup you just created
3. Tap **⋮** (three dots) on that backup
4. Select **"Restore"**
5. Read the warning dialog carefully
6. Tap **"RESTORE"** to confirm

### Step 5: Verify Restore
1. You should see message: **"✓ Restore complete! RESTART the app to see restored data."**
2. **CLOSE the app completely** (swipe away from recent apps)
3. **Reopen iScan**
4. Your data should be back to the state when you created the backup!

## What Should Happen

### During Backup:
- ✓ Copies database file to `/storage/emulated/0/Android/data/com.examscanner.premium/files/backups/`
- ✓ Names file with timestamp: `backup_YYYYMMDD_HHMMSS.db`
- ✓ Verifies backup is valid SQLite database
- ✓ Shows success message
- ✓ Auto-deletes backups older than 7 versions

### During Restore:
- ✓ Creates safety backup: `backup_before_restore_YYYYMMDD_HHMMSS.db`
- ✓ Validates backup file integrity
- ✓ Performs WAL checkpoint to flush changes
- ✓ Deletes WAL/SHM files for clean state
- ✓ Replaces database with backup
- ✓ Shows "RESTART the app" message

### After App Restart:
- ✓ Database is loaded from restored file
- ✓ All folders, exams, and answer keys are from backup
- ✓ Changes made after backup are gone
- ✓ Original data is safe in `backup_before_restore_*.db`

## Technical Details

### Files Created:
```
/storage/emulated/0/Android/data/com.examscanner.premium/files/backups/
├── backup_20260903_162309.db          (Original backup)
├── backup_20260903_170000.db          (Your new backup)
└── backup_before_restore_170500.db    (Safety backup made during restore)
```

### Database Location:
```
/data/data/com.examscanner.premium/databases/
├── exam_scanner_database       (Main database - gets replaced during restore)
├── exam_scanner_database-shm   (Shared memory - deleted during restore)
└── exam_scanner_database-wal   (Write-ahead log - deleted during restore)
```

## Known Limitations

1. **Must restart app** - Room database caches connections, so restore only takes effect after full app restart
2. **No auto-sync** - Backups are local only, not synced to cloud
3. **Requires storage permission** - Android 11+ may need manual permission grant
4. **WAL mode** - Database uses Write-Ahead Logging, so we checkpoint before restore

## Answer to Your Question

### **"Once used to retrieve, does it work?"**

**YES, but with important caveat:**

The restore functionality **WORKS** and will replace your database file, but you **MUST restart the app** to see the restored data. This is because:

1. Room database keeps database connection open
2. Restore replaces the file on disk
3. Room continues using its cached connection
4. Only on app restart does Room reconnect and see the restored data

**Solution Applied:**
- Added clear warning message: "⚠️ You MUST restart the app after restore"
- Success message explicitly says: "RESTART the app to see restored data"
- WAL checkpoint ensures data is flushed before replace
- Safety backup protects against data loss

### The Complete Flow:
```
1. User taps "Restore" → Dialog warns about app restart
2. Confirm restore → Creates safety backup automatically
3. Replaces database → Flushes WAL, deletes SHM
4. Shows success → "RESTART the app to see restored data"
5. User closes app → Swipes away from recent apps
6. User reopens app → Room connects to restored database
7. Success! → All data is from the backup
```

**TL;DR: Yes it works, but app restart is required. We've made this very clear in the UI.**
