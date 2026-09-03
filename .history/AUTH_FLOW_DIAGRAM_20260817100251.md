# 🔐 Authentication Flow Visualization

## App Launch Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     USER OPENS APP                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              AuthViewModel.checkAuthStatus()                │
│                  (Automatic on launch)                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                Check Firebase Auth Token                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
            ┌───────────────┴───────────────┐
            ↓                               ↓
┌───────────────────────┐       ┌───────────────────────┐
│   No Auth Token       │       │   Has Auth Token      │
│   (Not Signed In)     │       │   (Signed In)         │
└───────────────────────┘       └───────────────────────┘
            ↓                               ↓
            ↓                               ↓
┌───────────────────────┐       ┌───────────────────────┐
│   Navigate to:        │       │  Fetch User Profile   │
│   "welcome"           │       │  from Firestore       │
└───────────────────────┘       └───────────────────────┘
            ↓                               ↓
            ↓                   ┌───────────┴───────────┐
┌───────────────────────┐       ↓                       ↓
│                       │   ┌─────────┐          ┌──────────┐
│  WELCOME SCREEN       │   │ Success │          │  Failed  │
│                       │   └─────────┘          └──────────┘
│  📝 Offline           │       ↓                       ↓
│     Assessment        │       ↓                       ↓
│                       │   ┌─────────────┐      ┌──────────┐
│  ✓ Works offline     │   │  Navigate   │      │ Sign Out │
│  ☁️ Cloud sync       │   │  to Home    │      │ & Show   │
│  📊 MELC tracking    │   │  Screen     │      │ Welcome  │
│  💰 ₱100/month       │   └─────────────┘      └──────────┘
│                       │
│  [Get Started Free]  │
│  [Sign In]           │
│                       │
└───────────────────────┘
```

---

## Sign Up Flow

```
┌─────────────────────────────────────────────────────────────┐
│                 USER CLICKS "GET STARTED"                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    SIGN UP SCREEN                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Full Name:      [Test Teacher          ]            │  │
│  │  Email:          [test@teacher.com      ]            │  │
│  │  Password:       [••••••••              ] 👁         │  │
│  │  Confirm Pass:   [••••••••              ] 👁         │  │
│  │  School:         [Test School           ] (optional) │  │
│  │                                                       │  │
│  │              [Create Account]                         │  │
│  │                                                       │  │
│  │         Already have account? Sign In                │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   User clicks "Create Account"
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Validate Form (Frontend)                       │
│  ✓ All required fields filled?                             │
│  ✓ Valid email format?                                     │
│  ✓ Password ≥ 6 characters?                                │
│  ✓ Passwords match?                                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   ┌────────┴────────┐
                   ↓                 ↓
            ┌──────────┐      ┌──────────┐
            │ Invalid  │      │  Valid   │
            └──────────┘      └──────────┘
                   ↓                 ↓
            Show error         AuthViewModel.signUp()
            on form                  ↓
                            ┌────────────────────┐
                            │ AuthRepository     │
                            │ .signUp()          │
                            └────────────────────┘
                                     ↓
┌─────────────────────────────────────────────────────────────┐
│              FIREBASE OPERATIONS (Backend)                  │
│                                                             │
│  1. Create Firebase Auth account                           │
│     auth.createUserWithEmailAndPassword()                  │
│                                                             │
│  2. Update display name                                    │
│     user.updateProfile()                                   │
│                                                             │
│  3. Calculate trial expiration                             │
│     trialExpiresAt = now + 14 days                         │
│                                                             │
│  4. Create user profile in Firestore                       │
│     users/{uid}/profile/data → {                           │
│       email, displayName, schoolName,                      │
│       subscriptionStatus: "TRIAL",                         │
│       subscriptionExpiresAt: timestamp                     │
│     }                                                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   ┌────────┴────────┐
                   ↓                 ↓
            ┌──────────┐      ┌──────────┐
            │  Error   │      │ Success  │
            └──────────┘      └──────────┘
                   ↓                 ↓
            Show error          AuthState =
            message            Authenticated
                                    ↓
                            LaunchedEffect
                            detects change
                                    ↓
┌─────────────────────────────────────────────────────────────┐
│         AUTO-NAVIGATE TO HOME SCREEN                        │
│         (Subject Folders)                                   │
│                                                             │
│         ✅ User is now signed in!                           │
│         ✅ 14-day trial started!                            │
│         ✅ Profile saved in cloud!                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Sign In Flow

```
┌─────────────────────────────────────────────────────────────┐
│                 USER CLICKS "SIGN IN"                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    SIGN IN SCREEN                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Email:     [test@teacher.com      ]                  │  │
│  │  Password:  [••••••••              ] 👁               │  │
│  │                                                       │  │
│  │                         Forgot Password?             │  │
│  │                                                       │  │
│  │              [Sign In]                                │  │
│  │                                                       │  │
│  │         Don't have account? Sign Up                  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   User clicks "Sign In"
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Validate Form (Frontend)                       │
│  ✓ Email not blank?                                        │
│  ✓ Valid email format?                                     │
│  ✓ Password not blank?                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   AuthViewModel.signIn()
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              FIREBASE OPERATIONS (Backend)                  │
│                                                             │
│  1. Authenticate with Firebase Auth                        │
│     auth.signInWithEmailAndPassword()                      │
│                                                             │
│  2. Fetch user profile from Firestore                      │
│     users/{uid}/profile/data.get()                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   ┌────────┴────────┐
                   ↓                 ↓
            ┌──────────┐      ┌──────────┐
            │  Error   │      │ Success  │
            │          │      │          │
            │ Wrong    │      │ User     │
            │ password │      │ profile  │
            │          │      │ loaded   │
            └──────────┘      └──────────┘
                   ↓                 ↓
            Show error          AuthState =
            message            Authenticated
                                    ↓
                            LaunchedEffect
                            detects change
                                    ↓
┌─────────────────────────────────────────────────────────────┐
│         AUTO-NAVIGATE TO HOME SCREEN                        │
│         (Subject Folders)                                   │
│                                                             │
│         ✅ User is now signed in!                           │
│         ✅ Welcome back!                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Forgot Password Flow

```
┌─────────────────────────────────────────────────────────────┐
│            USER CLICKS "FORGOT PASSWORD?"                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              FORGOT PASSWORD SCREEN                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Reset Password                                       │  │
│  │  Enter your email to receive a reset link            │  │
│  │                                                       │  │
│  │  Email:  [test@teacher.com      ]                    │  │
│  │                                                       │  │
│  │          [Send Reset Link]                            │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
                User clicks "Send Reset Link"
                            ↓
┌─────────────────────────────────────────────────────────────┐
│         AuthViewModel.sendPasswordResetEmail()              │
│                    ↓                                        │
│         Firebase sends reset email                          │
│         auth.sendPasswordResetEmail()                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
                   ┌────────┴────────┐
                   ↓                 ↓
            ┌──────────┐      ┌──────────┐
            │  Error   │      │ Success  │
            └──────────┘      └──────────┘
                   ↓                 ↓
            Show error        ┌───────────────────┐
            message           │                   │
                              │  ✅ Check Your   │
                              │     Email         │
                              │                   │
                              │  We've sent a    │
                              │  reset link to:  │
                              │                   │
                              │  test@teacher    │
                              │  .com             │
                              │                   │
                              │  [Back to        │
                              │   Sign In]        │
                              │                   │
                              └───────────────────┘
                                      ↓
                              User checks email
                                      ↓
                              Clicks reset link
                                      ↓
                              Opens in browser
                                      ↓
                              Enters new password
                                      ↓
                              Can now sign in!
```

---

## State Management

```
┌─────────────────────────────────────────────────────────────┐
│                    AuthViewModel                            │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  StateFlow<AuthState>                                │  │
│  │                                                      │  │
│  │  • Loading      → Show spinner                      │  │
│  │  • Unauthenticated → Show Welcome/Auth screens      │  │
│  │  • Authenticated(user) → Show Home screen           │  │
│  │  • Error(msg)   → Show error, stay on auth screen   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  Methods:                                                   │
│  • checkAuthStatus()    (on app launch)                     │
│  • signUp(data)                                             │
│  • signIn(email, pass)                                      │
│  • sendPasswordResetEmail(email)                            │
│  • signOut()                                                │
│  • hasActiveSubscription(user) → true/false                 │
│  • getTrialDaysRemaining(user) → 0-14                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
                  Observed by MainActivity
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Navigation                              │
│                                                             │
│  authState.collectAsState()                                 │
│         ↓                                                   │
│  when (authState) {                                         │
│    Loading → "loading" screen                               │
│    Unauthenticated → "welcome" screen                       │
│    Authenticated → "subject_folders" screen                 │
│    Error → "welcome" screen (show error)                    │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   AUTHENTICATION                            │
│                                                             │
│   App Layer          ViewModel          Repository         │
│      ↓                  ↓                   ↓              │
│   UI Screen  →  AuthViewModel  →  AuthRepository           │
│      ↓                  ↓                   ↓              │
│   Button          signUp()            Firebase Auth        │
│   Click           State Flow          Firestore            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      FIREBASE                               │
│                                                             │
│   Firebase Auth               Firestore Database           │
│   ┌─────────────┐             ┌──────────────────┐         │
│   │ Users       │             │ users/           │         │
│   │             │             │   {uid}/         │         │
│   │ Email/Pass  │ ←─linked─→ │     profile/     │         │
│   │ Sessions    │             │       data/      │         │
│   │ Tokens      │             │         {...}    │         │
│   └─────────────┘             └──────────────────┘         │
│                                                             │
│   • Handles authentication    • Stores user data           │
│   • Manages sessions          • Subscription info          │
│   • Password resets           • School info                │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   FIRESTORE SECURITY                        │
│                                                             │
│  Rules:                                                     │
│                                                             │
│  users/{userId}/{document=**}                               │
│    ↓                                                        │
│    allow read, write:                                       │
│      if request.auth != null                                │
│      && request.auth.uid == userId                          │
│                                                             │
│  Translation:                                               │
│  • User must be authenticated (signed in)                   │
│  • User can ONLY access their OWN data                      │
│  • Cannot read/write other users' data                      │
│  • Enforced server-side (can't bypass)                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Example:

User A (uid: abc123)
  ✅ Can read:  users/abc123/profile/data
  ✅ Can write: users/abc123/exams/exam1
  ❌ Cannot read:  users/xyz789/profile/data
  ❌ Cannot write: users/xyz789/exams/exam1

User B (uid: xyz789)
  ✅ Can read:  users/xyz789/profile/data
  ✅ Can write: users/xyz789/exams/exam1
  ❌ Cannot read:  users/abc123/profile/data
  ❌ Cannot write: users/abc123/exams/exam1
```

---

## Trial Management

```
┌─────────────────────────────────────────────────────────────┐
│                  14-DAY FREE TRIAL                          │
│                                                             │
│  On Sign Up:                                                │
│    subscriptionStatus = "TRIAL"                             │
│    subscriptionExpiresAt = now + 14 days                    │
│    createdAt = now                                          │
│                                                             │
│  During Trial:                                              │
│    isTrialActive() → true                                   │
│    getTrialDaysRemaining() → 14, 13, 12, ..., 1, 0         │
│    hasActiveSubscription() → true                           │
│                                                             │
│  After 14 Days:                                             │
│    subscriptionStatus = "EXPIRED"                           │
│    isTrialActive() → false                                  │
│    hasActiveSubscription() → false                          │
│    → Show "Subscribe" prompt                                │
│                                                             │
│  After Payment (Phase 4):                                   │
│    subscriptionStatus = "ACTIVE"                            │
│    subscriptionExpiresAt = now + 30 days                    │
│    hasActiveSubscription() → true                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

**This visual guide shows exactly how authentication works in your app!** 🎨
