package com.examscanner.premium.auth

import android.content.Context
import com.examscanner.premium.utils.TrialGuard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Trial duration: 14 days
    private val TRIAL_DURATION_MILLIS = TimeUnit.DAYS.toMillis(14)
    
    val currentUser get() = auth.currentUser
    
    suspend fun signUp(signUpData: SignUpData): Result<UserProfile> {
        return try {
            // STEP 1: Create Firebase Auth user first (need userId for device check)
            val authResult = auth.createUserWithEmailAndPassword(
                signUpData.email,
                signUpData.password
            ).await()
            
            val user = authResult.user ?: return Result.failure(
                Exception("User creation failed")
            )
            
            // STEP 2: Check if device has already used trial
            val trialCheckResult = TrialGuard.hasDeviceUsedTrial(context, user.uid)
            if (trialCheckResult.isSuccess && trialCheckResult.getOrDefault(false)) {
                // Device has already completed a trial - delete the auth account
                user.delete().await()
                
                TrialGuard.logTrialViolation(
                    context,
                    signUpData.email,
                    "Attempted signup on device that already completed trial"
                )
                return Result.failure(
                    TrialAbusedException(
                        "This device has already used the 14-day trial. " +
                        "Please sign in with your existing account or subscribe to continue."
                    )
                )
            }
            
            // STEP 3: Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(signUpData.displayName)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // STEP 4: Calculate trial expiration (14 days from now)
            val trialExpiresAt = System.currentTimeMillis() + TRIAL_DURATION_MILLIS
            
            // STEP 5: Create user profile in Firestore
            val userProfile = UserProfile(
                uid = user.uid,
                email = signUpData.email,
                displayName = signUpData.displayName,
                schoolName = signUpData.schoolName,
                gradeLevel = signUpData.gradeLevel,
                subjects = signUpData.subjects,
                subscriptionStatus = SubscriptionStatus.TRIAL,
                subscriptionExpiresAt = trialExpiresAt,
                createdAt = System.currentTimeMillis()
            )
            
            // STEP 6: Save to Firestore
            firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("data")
                .set(userProfile.toMap())
                .await()
            
            // STEP 7: Register device trial start (track this device)
            TrialGuard.registerTrialStart(context, user.uid, signUpData.email)
            
            Result.success(userProfile)
        } catch (e: TrialAbusedException) {
            Result.failure(e)
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(getAuthErrorMessage(e.errorCode)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(
                Exception("Sign in failed")
            )
            
            // Fetch user profile from Firestore
            val profileDoc = firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("data")
                .get()
                .await()
            
            val userProfile = profileDoc.toUserProfile(user.uid, user.email ?: email)
            Result.success(userProfile)
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(getAuthErrorMessage(e.errorCode)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    suspend fun getCurrentUserProfile(): UserProfile? {
        val user = currentUser ?: return null
        
        return try {
            val profileDoc = firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("data")
                .get()
                .await()
            
            profileDoc.toUserProfile(user.uid, user.email ?: "")
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        val user = currentUser ?: return Result.failure(
            Exception("No user signed in")
        )
        
        return try {
            firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("data")
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isTrialActive(userProfile: UserProfile): Boolean {
        if (userProfile.subscriptionStatus != SubscriptionStatus.TRIAL) {
            return false
        }
        
        val expiresAt = userProfile.subscriptionExpiresAt ?: return false
        return System.currentTimeMillis() < expiresAt
    }
    
    fun getTrialDaysRemaining(userProfile: UserProfile): Int {
        val expiresAt = userProfile.subscriptionExpiresAt ?: return 0
        val remainingMillis = expiresAt - System.currentTimeMillis()
        if (remainingMillis < 0) return 0
        
        return TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt()
    }
    
    /**
     * Mark trial as completed for this device
     * Call this when trial expires or user subscribes
     */
    suspend fun markTrialCompleted() {
        val user = currentUser ?: return
        TrialGuard.flagTrialComplete(context, user.uid)
    }
    
    /**
     * Check if device can start trial
     * Returns error message if device already used trial
     */
    suspend fun canDeviceStartTrial(): Pair<Boolean, String?> {
        val user = currentUser
        if (user == null) {
            return Pair(true, null) // Allow if not authenticated yet
        }
        
        val result = TrialGuard.hasDeviceUsedTrial(context, user.uid)
        return if (result.isSuccess) {
            val hasUsed = result.getOrDefault(false)
            if (hasUsed) {
                Pair(
                    false,
                    "This device has already used the 14-day trial. " +
                    "Please sign in with your existing account or subscribe."
                )
            } else {
                Pair(true, null)
            }
        } else {
            Pair(true, null) // Allow trial on error (fail open)
        }
    }
    
    private fun getAuthErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            else -> "Authentication failed. Please try again."
        }
    }
    
    private fun UserProfile.toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "schoolName" to schoolName,
            "gradeLevel" to gradeLevel,
            "subjects" to subjects,
            "photoUrl" to photoUrl,
            "subscriptionStatus" to subscriptionStatus.name,
            "subscriptionExpiresAt" to subscriptionExpiresAt,
            "createdAt" to createdAt
        )
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(
        uid: String,
        email: String
    ): UserProfile {
        return UserProfile(
            uid = uid,
            email = email,
            displayName = getString("displayName") ?: "",
            schoolName = getString("schoolName"),
            gradeLevel = getString("gradeLevel"),
            subjects = get("subjects") as? List<String> ?: emptyList(),
            photoUrl = getString("photoUrl"),
            subscriptionStatus = SubscriptionStatus.valueOf(
                getString("subscriptionStatus") ?: "TRIAL"
            ),
            subscriptionExpiresAt = getLong("subscriptionExpiresAt"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }
}


/**
 * Exception thrown when trial abuse is detected
 */
class TrialAbusedException(message: String) : Exception(message)
