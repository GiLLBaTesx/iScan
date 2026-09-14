# 🛡️ Trial Abuse Prevention System

## Overview
This document explains how the iScan app prevents users from abusing the 14-day free trial by creating multiple accounts on the same device.

## 🎯 Problem
Without protection, users could:
1. Sign up for 14-day trial
2. When trial expires, create new email account
3. Sign up again for another 14-day trial
4. Repeat indefinitely (never pay)

## ✅ Solution: Multi-Layer Device Fingerprinting

### Layer 1: Device Fingerprinting
**File**: `TrialGuard.kt`

Creates unique device identifier using:
```kotlin
- Android ID (Settings.Secure.ANDROID_ID)
- Device manufacturer (e.g., Samsung, Google)
- Device model (e.g., Galaxy S23, Pixel 7)
- Device brand
- Device product name
```

These are combined and hashed (SHA-256) to create a stable fingerprint that:
- ✅ Persists across app reinstalls
- ✅ Survives data clearing
- ✅ Remains consistent for the device
- ❌ Changes only with factory reset (acceptable trade-off)

### Layer 2: Server-Side Tracking
**Firestore Collections**:

#### `device_trials` collection
Tracks every device that starts a trial:
```javascript
{
  deviceId: "hashed_fingerprint",
  userId: "firebase_user_id",
  email: "user@example.com",
  trialStartedAt: 1704067200000,
  trialCompletedAt: 1705276800000, // null if not completed
  trialUsed: true, // Flag: has this device completed trial?
  deviceModel: "Pixel 7",
  deviceManufacturer: "Google",
  androidVersion: "34",
  lastUpdated: 1705276800000
}
```

#### `trial_violations` collection
Logs abuse attempts:
```javascript
{
  deviceId: "hashed_fingerprint",
  attemptedEmail: "newuser@example.com",
  reason: "Attempted signup on device that already completed trial",
  timestamp: 1705276900000,
  deviceModel: "Pixel 7",
  deviceManufacturer: "Google"
}
```

### Layer 3: Signup Flow Protection

**When user tries to sign up**:

```kotlin
1. Calculate device fingerprint
   ↓
2. Check Firestore: Has this device used trial?
   ↓
3a. YES → Block signup, show error message
       → Log violation to Firestore
       → Suggest: "Sign in with existing account or subscribe"
   
3b. NO → Allow signup
       → Create Firebase Auth account
       → Save user profile
       → Register device trial in Firestore (trialUsed = false)
       → Start 14-day trial
```

**When trial expires**:
```kotlin
1. Mark subscription as expired
   ↓
2. Call TrialGuard.flagTrialComplete()
   ↓
3. Update Firestore: Set trialUsed = true for this device
   ↓
4. Device is now permanently flagged
   ↓
5. Future signup attempts on this device will be blocked
```

## 📊 Firestore Security Rules

Add these rules to prevent tampering:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Device trial tracking - write once, read anytime
    match /device_trials/{deviceId} {
      // Anyone can check if device has used trial (needed for signup)
      allow read: if true;
      
      // Only allow creation if trialUsed is false (first trial)
      allow create: if request.resource.data.trialUsed == false;
      
      // Only allow updates to flag trial as complete (one-way operation)
      allow update: if request.auth != null 
                    && request.resource.data.trialUsed == true
                    && resource.data.trialUsed == false;
      
      // Never allow deletion
      allow delete: if false;
    }
    
    // Trial violation logs - write only
    match /trial_violations/{violationId} {
      allow create: if true; // Allow logging violations
      allow read: if false;  // Only admins can read (via Firebase Console)
      allow update, delete: if false;
    }
    
    // User profiles
    match /users/{userId}/profile/{document=**} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## 🔒 Strength Assessment

### ✅ Strong Against:
1. **Multiple email accounts** - Device is flagged, new accounts blocked
2. **App reinstall** - Device fingerprint persists
3. **Clear app data** - Fingerprint based on hardware, not app data
4. **Sign out and sign up** - Device check happens before account creation
5. **Wait for trial to expire** - Device flagged when trial completes

### ⚠️ Weak Against (Edge Cases):
1. **Factory reset** - Wipes ANDROID_ID (rare, acceptable)
2. **Device change** - User buys new phone (legitimate use case)
3. **Custom ROMs** - May generate new ANDROID_ID (small % of users)
4. **Emulators** - Can be detected and blocked separately if needed

### 💡 Additional Hardening (Optional):
If abuse becomes a problem, add:
```kotlin
// Email domain blocking (disposable emails)
- Block temporary email providers (10minutemail.com, etc.)

// IP address tracking
- Flag if multiple accounts from same IP (requires backend)

// Behavioral analysis
- Unusual signup patterns (too many accounts in short time)

// Phone number verification
- Require SMS verification (costs money but very effective)

// Payment method fingerprinting
- If they ever paid, track payment method
```

## 📱 User Experience

### Legitimate Users:
- ✅ Smooth signup experience
- ✅ Clear trial countdown
- ✅ Can use trial once per device
- ✅ Can still sign in with existing account

### Abuse Attempts:
- ❌ Blocked at signup with clear message
- ❌ Violation logged for monitoring
- ✅ Directed to sign in or subscribe

### Error Message:
```
"This device has already used the 14-day trial. 
Please sign in with your existing account or subscribe to continue."
```

## 🚀 Implementation Checklist

- [x] Create `TrialGuard.kt` utility
- [x] Update `AuthRepository.kt` to check device trials
- [x] Update `AuthViewModel.kt` to handle trial abuse
- [ ] Test signup flow with trial protection
- [ ] Deploy Firestore security rules
- [ ] Add Firestore indexes for queries
- [ ] Monitor `trial_violations` collection
- [ ] Set up alerts for abuse patterns
- [ ] Add admin dashboard to review violations
- [ ] Consider adding phone verification if abuse increases

## 📈 Monitoring

Track these metrics in Firebase Console:

```
1. trial_violations collection size
   - High growth = abuse attempts

2. device_trials where trialUsed = true
   - Total devices that completed trial

3. Auth user creation vs device_trials creation
   - Should be 1:1 ratio
   - If auth >> device_trials = integration issue

4. Multiple auth accounts with same deviceId
   - Shows blocked abuse attempts
```

## 🔧 Configuration

### Adjust Trial Duration:
```kotlin
// In AuthRepository.kt
private val TRIAL_DURATION_MILLIS = TimeUnit.DAYS.toMillis(14) // Change 14 to X days
```

### Enable Debug Logging:
```kotlin
// In TrialGuard.kt
private const val DEBUG = true // Show device fingerprint in logs
```

## 💰 Cost Considerations

### Firestore Usage:
- **Reads**: 1 per signup attempt (check device trial)
- **Writes**: 1 per trial start, 1 per trial complete
- **Storage**: ~500 bytes per device record

**Example**: 10,000 users/month
- Reads: 10,000 (well within free tier)
- Writes: 20,000 (within free tier)
- Storage: 5MB (negligible)

**Cost**: FREE for most apps (within Firebase free tier)

## 🎓 Educational Value

This system teaches:
1. Device fingerprinting techniques
2. Server-side validation (never trust client)
3. Multi-layer security
4. Firestore security rules
5. Abuse prevention strategies
6. Balance between security and UX

---

**Status**: ✅ Implemented and ready for testing
**Last Updated**: 2026-09-04
**Maintainer**: Development Team
