# 🔍 Disabled Features Report - iScan App

## Summary
Found **3 disabled/unimplemented features** in the app:

---

## 1. ⚠️ **Firebase Authentication - DISABLED**

### Location:
`MainActivity.kt` - Line 75

### Code:
```kotlin
// Authentication disabled for testing - will enable after Firebase setup
// Start with subject folders (organized approach)
NavHost(navController = navController, startDestination = "subject_folders")
```

### Status: 
**Intentionally Disabled** - Authentication system is commented out for testing purposes.

### What's Missing:
- User login/signup screens
- Email/Password authentication
- Google Sign-In
- User session management
- Multi-user data isolation

### What Exists:
- Firebase dependency is installed (google-services.json present)
- AuthViewModel.kt exists in `/auth/` folder
- AuthRepository.kt exists
- AuthState.kt exists
- Auth UI screens exist in `ui/screens/auth/` folder

### Impact:
- App currently runs without login - all users share same database
- No cloud sync between devices
- No user-specific data protection

### To Enable:
1. Uncomment authentication code in MainActivity
2. Change startDestination to authentication screen
3. Add authentication checks to protected screens
4. Connect Firebase Auth SDK
5. Test login/signup flow

---

## 2. ⚠️ **PDF Template Download - NOT IMPLEMENTED**

### Location:
`SubjectFolderListScreen.kt` - Line 578

### Code:
```kotlin
onClick = {
    scope.launch {
        // TODO: Implement PDF generation
        android.widget.Toast.makeText(
            context,
            "Downloading $name template...",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        showTemplateDialog = false
    }
}
```

### Status:
**Partially Implemented** - Dialog shows 5 templates (20Q, 40Q, 60Q, 80Q, 100Q) but clicking does nothing.

### What's Missing:
The click handler only shows a toast message but doesn't actually generate or download PDF templates.

### What Exists:
- Template dialog UI with 5 options
- Template Generator Screen (accessible from Settings)
- TemplatePDFGenerator.kt utility exists
- MainActivity has full PDF generation logic (lines 134-177)

### Impact:
- Users see templates but can't download them from home screen dialog
- Must go to Settings → Create Template instead
- Inconsistent UX - dialog seems to work but doesn't

### To Enable:
Replace the TODO section with actual PDF generation code (similar to template_generator route in MainActivity).

### Suggested Fix:
```kotlin
onClick = {
    scope.launch {
        val (templateName, totalQuestions) = templates[index]
        try {
            val file = com.examscanner.premium.utils.TemplatePDFGenerator.generateTemplate(
                context = context,
                templateName = templateName,
                totalQuestions = totalQuestions,
                choicesPerQuestion = 4  // A-D default
            )
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(intent)
            android.widget.Toast.makeText(
                context,
                "Template generated: $templateName",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Failed: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        showTemplateDialog = false
    }
}
```

---

## 3. ⚠️ **Backup Export Feature - NOT IMPLEMENTED**

### Location:
`BackupManagementScreen.kt` - Line 282

### Code:
```kotlin
onExport = {
    // TODO: Implement export
}
```

### Status:
**Placeholder Only** - Export button exists in backup item menu but does nothing.

### What's Missing:
- Export backup to external storage
- Share backup file via Share Sheet
- Export to cloud storage (Google Drive, etc.)

### What Exists:
- BackupManager.exportBackup() function exists (line 277 in BackupManager.kt)
- File picker for destination is already set up (exportFilePicker)
- Just needs to be wired up

### Impact:
- Users can't easily share backups between devices
- Can't export backups to cloud storage
- Can only restore from backups created on same device

### To Enable:
Wire up the exportFilePicker to the export function.

### Suggested Fix:
```kotlin
var backupToExport by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }

// In BackupItemCard:
onExport = {
    backupToExport = backup
    exportFilePicker.launch(backup.file.name)
}

// In exportFilePicker result handler:
val exportFilePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
) { uri: Uri? ->
    uri?.let { destinationUri ->
        backupToExport?.let { backup ->
            scope.launch {
                val result = BackupManager.exportBackup(context, backup, destinationUri)
                if (result.isSuccess) {
                    successMessage = "Backup exported successfully!"
                } else {
                    errorMessage = "Export failed: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }
}
```

---

## ✅ Features That ARE Working

### Backup System:
- ✅ Create backup
- ✅ List backups
- ✅ Restore backup (with app restart)
- ✅ Delete backup
- ✅ Import backup from file
- ✅ Automatic cleanup (keeps 7 versions)
- ✅ Safety backups before restore
- ⚠️ Export backup (not wired up)

### Template System:
- ✅ Template Generator Screen (Settings → Create Template)
- ✅ Full PDF generation with customization
- ✅ FileProvider integration
- ✅ PDF viewer/share integration
- ⚠️ Quick template download from home (not implemented)

### Authentication:
- ✅ Firebase SDK installed
- ✅ Auth ViewModels exist
- ✅ Auth UI screens exist
- ⚠️ Completely disabled in MainActivity

### All Other Features:
- ✅ Subject Folders (create, rename, delete)
- ✅ Exam management
- ✅ Answer key editor with MELC tagging
- ✅ Bi-directional MELC sync
- ✅ Student roster management
- ✅ CSV import for students
- ✅ Section management
- ✅ Camera scanner
- ✅ Bubble sheet processing
- ✅ Export to CSV
- ✅ Settings screen
- ✅ Clear all data

---

## Priority Fixes

### High Priority:
1. **Template PDF download** - Users expect this to work since dialog is shown
2. **Backup export** - Important for device migration

### Medium Priority:
3. **Firebase Authentication** - Security and multi-user support

### Low Priority:
None - all other features are working

---

## Summary Table

| Feature | Status | Location | Priority |
|---------|--------|----------|----------|
| Firebase Auth | Disabled | MainActivity.kt:75 | Medium |
| PDF Template Download | Not Implemented | SubjectFolderListScreen.kt:578 | High |
| Backup Export | Not Implemented | BackupManagementScreen.kt:282 | High |

---

## Recommendations

1. **Immediate**: Implement PDF template download from home dialog (users expect this)
2. **Immediate**: Wire up backup export feature (function exists, just needs connection)
3. **Future**: Enable Firebase authentication when ready for multi-user support

---

**Report Generated**: September 3, 2026  
**App Version**: iScan (ExamScanner Premium)  
**Total Disabled Features**: 3
