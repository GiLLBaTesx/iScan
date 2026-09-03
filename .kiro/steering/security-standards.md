---
inclusion: always
---

# Security Standards for iScan

## Overview
Security is critical for iScan as it handles student data and exam results. Follow these standards religiously.

## Critical Security Rules

### 1. Database Encryption
**Status**: ✅ Implemented with SQLCipher

```kotlin
// All Room databases MUST use SQLCipher
val passphrase = EncryptionKeyManager.getDatabasePassphrase()
val database = Room.databaseBuilder(context, AppDatabase::class.java, "exam_db")
    .openHelperFactory(SupportFactory(passphrase))
    .build()
```

**TODO**: Move encryption keys to Android Keystore (currently in SharedPreferences)

### 2. Sensitive Data Handling

#### ✅ DO:
- Store encryption keys in Android Keystore
- Use EncryptedSharedPreferences for sensitive preferences
- Clear sensitive data from memory after use
- Validate all user inputs
- Use parameterized queries (Room handles this)

#### ❌ DON'T:
- Log passwords, tokens, or encryption keys
- Store API keys in code
- Commit `google-services.json` to git (already in .gitignore)
- Store unencrypted student data
- Use plain SharedPreferences for sensitive data

### 3. Authentication & Authorization

```kotlin
// Always verify user is authenticated
val currentUser = FirebaseAuth.getInstance().currentUser
if (currentUser == null) {
    // Redirect to login
    return
}

// Check trial/subscription status before premium features
TrialGuard.isEligible(currentUser.uid) { eligible ->
    if (!eligible) {
        // Show upgrade prompt
        return@isEligible
    }
    // Allow feature access
}
```

**Rules:**
- Never trust client-side auth checks alone
- Always validate trial status before feature access
- Device-based trial tracking (prevent email abuse)
- Require auth before Firestore device_trials access

### 4. Firebase Security Rules

**Firestore Rules** (already implemented):
```javascript
// Device trials - require authentication
match /device_trials/{trialId} {
  allow read: if request.auth != null;
  allow write: if request.auth != null && 
                  request.resource.data.userId == request.auth.uid;
}

// User profiles - own data only
match /users/{userId} {
  allow read, write: if request.auth != null && 
                        request.auth.uid == userId;
}

// Analytics - write only
match /analytics_events/{eventId} {
  allow create: if request.auth != null;
  allow read: if false; // Admin SDK only
}
```

### 5. Network Security

**HTTPS Enforcement** (already configured in `network_security_config.xml`):
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**Rules:**
- All network traffic MUST use HTTPS
- No cleartext traffic allowed
- Trust system certificates only
- Pin certificates for critical APIs (future enhancement)

### 6. Logging Security

```kotlin
// ✅ Use SecureLogger (production-safe)
SecureLogger.d("TAG", "Debug info")
SecureLogger.e("TAG", "Error", throwable)

// ✅ Mark sensitive data explicitly
SecureLogger.sensitive("TAG", "API Key: $key") // Only in debug

// ❌ Never use android.util.Log directly
Log.d("TAG", "Password: $pwd") // ❌ FORBIDDEN
```

**Production Behavior:**
- Debug logs disabled automatically
- Error logs can be sent to Firebase Crashlytics
- Sensitive logs never appear in production

### 7. ProGuard/R8 Obfuscation

**Status**: ✅ Enabled in release builds

```proguard
# Keep security classes from being stripped
-keep class com.examscanner.premium.utils.EncryptionKeyManager { *; }
-keep class com.examscanner.premium.utils.SecureLogger { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }
```

### 8. Root/Emulator Detection

**Status**: ✅ Implemented (4 detection methods)

The app detects:
1. Root binaries (su, busybox)
2. Root management apps (Magisk, SuperSU)
3. Test-keys build
4. Su command execution

**Behavior:**
- Warn users about security risks
- Don't block completely (some legitimate users)
- Log detection for analytics

### 9. Input Validation

```kotlin
// ✅ Validate all user inputs
fun validateExamName(name: String): Boolean {
    return name.isNotBlank() && 
           name.length <= 100 &&
           !name.contains(Regex("[<>\"'/]")) // Prevent injection
}

// ✅ Sanitize file paths
fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
```

### 10. Permissions Management

**Minimal Permissions:**
```xml
<!-- Only essential permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- Scoped storage (Android 10+) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
```

**Runtime Permissions:**
```kotlin
// ✅ Request permissions with rationale
PermissionsScreen(
    permission = android.Manifest.permission.CAMERA,
    rationale = "Camera is needed to scan bubble sheets",
    onGranted = { /* proceed */ }
)
```

## Security Checklist

### Before Every Release:
- [ ] Update ProGuard rules
- [ ] Verify no hardcoded secrets
- [ ] Check .gitignore includes sensitive files
- [ ] Test with ProGuard enabled
- [ ] Review Firebase security rules
- [ ] Scan for security vulnerabilities
- [ ] Test root detection
- [ ] Verify HTTPS enforcement

### Code Review Focus:
- [ ] No Log.d() statements with sensitive data
- [ ] All databases encrypted
- [ ] Auth checks before sensitive operations
- [ ] Input validation on user data
- [ ] No hardcoded API keys
- [ ] Proper error handling (no info leakage)

## Known Security Issues (TODO)

### Priority: HIGH
1. **Encryption keys in SharedPreferences**
   - Current: Stored in plain SharedPreferences
   - Goal: Move to Android Keystore
   - Impact: Medium (device must be compromised to access)

2. **No certificate pinning**
   - Current: Trusting system certificates
   - Goal: Pin Firebase certificates
   - Impact: Low (MITM requires device compromise)

### Priority: MEDIUM
3. **EncryptedSharedPreferences not used**
   - Current: Using plain SharedPreferences for non-critical data
   - Goal: Use EncryptedSharedPreferences
   - Impact: Low (only affects local preferences)

4. **ProGuard rules too broad**
   - Current: Keeping all analytics classes
   - Goal: Selective rules
   - Impact: Low (slightly larger APK)

## Security Incident Response

If a security issue is discovered:

1. **Assess Severity**
   - Critical: Data breach, auth bypass
   - High: Encryption failure, credential leak
   - Medium: Input validation issue
   - Low: Info disclosure

2. **Immediate Actions**
   - Critical/High: Release emergency update
   - Medium: Fix in next release
   - Low: Track for future update

3. **User Notification**
   - Critical: Email all users, in-app alert
   - High: In-app notification
   - Medium/Low: Release notes only

4. **Post-Incident**
   - Document the issue
   - Add regression tests
   - Update security guidelines
   - Review similar code patterns

## Privacy & Compliance

### GDPR Compliance
- ✅ Users control their data
- ✅ Data minimization (only essential data)
- ✅ Right to deletion (account deletion)
- ✅ Data encryption at rest and in transit
- ✅ Privacy Policy disclosure

### COPPA Compliance
- ✅ No collection of student names/ages
- ✅ Minimal data collection
- ✅ Parental consent notice in Privacy Policy

### Data Retention
- User data: Retained until account deletion
- Analytics: 90 days rolling window
- Exam data: User-controlled (local only)
- Backups: User-controlled (local only)

## Security Resources

- Android Security Best Practices: https://developer.android.com/topic/security/best-practices
- OWASP Mobile Security: https://owasp.org/www-project-mobile-security/
- Firebase Security Rules: https://firebase.google.com/docs/rules

---

**Remember**: Security is not optional. When in doubt, err on the side of caution!
