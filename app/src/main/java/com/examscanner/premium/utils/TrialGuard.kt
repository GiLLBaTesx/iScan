package com.examscanner.premium.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

/**
 * TrialGuard - Multi-layer trial abuse prevention system
 * 
 * Prevents users from creating multiple accounts to abuse 14-day trial by:
 * 1. Device fingerprinting (ANDROID_ID + hardware identifiers)
 * 2. Server-side trial tracking in Firestore
 * 3. Flagging devices that have completed trials
 * 
 * Strategy: Once a device completes a trial, it's permanently flagged.
 * New accounts on the same device cannot start another trial.
 */
object TrialGuard {
    
    private val firestore = FirebaseFirestore.getInstance()
    private const val COLLECTION_DEVICE_TRIALS = "device_trials"
    private const val COLLECTION_TRIAL_VIOLATIONS = "trial_violations"
    
    /**
     * Get unique device fingerprint
     * Combines multiple identifiers to create a stable device ID
     */
    @SuppressLint("HardwareIds")
    fun getDeviceFingerprint(context: Context): String {
        // Primary: Android ID (persists across factory resets on modern devices)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        // Hardware identifiers
        val deviceModel = Build.MODEL
        val deviceManufacturer = Build.MANUFACTURER
        val deviceBrand = Build.BRAND
        val deviceProduct = Build.PRODUCT
        
        // Combine all identifiers
        val combined = "$androidId-$deviceManufacturer-$deviceModel-$deviceBrand-$deviceProduct"
        
        // Hash to create stable fingerprint
        return hashString(combined)
    }
    
    /**
     * Check if device has already used trial
     * Returns true if device is flagged as having completed trial
     * NOTE: This now requires user to be authenticated
     */
    suspend fun hasDeviceUsedTrial(context: Context, userId: String): Result<Boolean> {
        return try {
            val deviceId = getDeviceFingerprint(context)
            
            val doc = firestore.collection(COLLECTION_DEVICE_TRIALS)
                .document(deviceId)
                .get()
                .await()
            
            if (!doc.exists()) {
                Result.success(false)
            } else {
                // Verify this trial record belongs to current user or check flag
                val recordUserId = doc.getString("userId")
                val trialUsed = doc.getBoolean("trialUsed") ?: false
                
                // If trial is used and it's the same device, block regardless of user
                Result.success(trialUsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register device as having started trial
     * Called when user signs up and starts 14-day trial
     */
    suspend fun registerTrialStart(
        context: Context,
        userId: String,
        email: String
    ): Result<Unit> {
        return try {
            val deviceId = getDeviceFingerprint(context)
            val timestamp = System.currentTimeMillis()
            
            val deviceData = mapOf(
                "deviceId" to deviceId,
                "userId" to userId,
                "email" to email,
                "trialStartedAt" to timestamp,
                "trialUsed" to false, // Not completed yet
                "deviceModel" to Build.MODEL,
                "deviceManufacturer" to Build.MANUFACTURER,
                "androidVersion" to Build.VERSION.SDK_INT.toString(),
                "lastUpdated" to timestamp
            )
            
            firestore.collection(COLLECTION_DEVICE_TRIALS)
                .document(deviceId)
                .set(deviceData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Flag device as having completed trial
     * Called when trial expires or user subscribes
     * This permanently prevents new trials on this device
     */
    suspend fun flagTrialComplete(
        context: Context,
        userId: String
    ): Result<Unit> {
        return try {
            val deviceId = getDeviceFingerprint(context)
            val timestamp = System.currentTimeMillis()
            
            val updates = mapOf(
                "trialUsed" to true,
                "trialCompletedAt" to timestamp,
                "completedByUserId" to userId,
                "lastUpdated" to timestamp
            )
            
            firestore.collection(COLLECTION_DEVICE_TRIALS)
                .document(deviceId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Log trial violation attempt
     * Called when user tries to create new account after trial used
     */
    suspend fun logTrialViolation(
        context: Context,
        attemptedEmail: String,
        reason: String
    ): Result<Unit> {
        return try {
            val deviceId = getDeviceFingerprint(context)
            val timestamp = System.currentTimeMillis()
            
            val violationData = mapOf(
                "deviceId" to deviceId,
                "attemptedEmail" to attemptedEmail,
                "reason" to reason,
                "timestamp" to timestamp,
                "deviceModel" to Build.MODEL,
                "deviceManufacturer" to Build.MANUFACTURER
            )
            
            firestore.collection(COLLECTION_TRIAL_VIOLATIONS)
                .add(violationData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get device trial info for debugging/admin purposes
     */
    suspend fun getDeviceTrialInfo(context: Context): DeviceTrialInfo? {
        return try {
            val deviceId = getDeviceFingerprint(context)
            
            val doc = firestore.collection(COLLECTION_DEVICE_TRIALS)
                .document(deviceId)
                .get()
                .await()
            
            if (doc.exists()) {
                DeviceTrialInfo(
                    deviceId = deviceId,
                    userId = doc.getString("userId") ?: "",
                    email = doc.getString("email") ?: "",
                    trialStartedAt = doc.getLong("trialStartedAt") ?: 0L,
                    trialCompletedAt = doc.getLong("trialCompletedAt"),
                    trialUsed = doc.getBoolean("trialUsed") ?: false
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Hash string using SHA-256
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Generate installation ID (stored locally)
     * Fallback if device fingerprinting fails
     */
    fun getOrCreateInstallationId(context: Context): String {
        val prefs = context.getSharedPreferences("trial_guard", Context.MODE_PRIVATE)
        var installId = prefs.getString("install_id", null)
        
        if (installId == null) {
            installId = UUID.randomUUID().toString()
            prefs.edit().putString("install_id", installId).apply()
        }
        
        return installId
    }
}

/**
 * Data class for device trial information
 */
data class DeviceTrialInfo(
    val deviceId: String,
    val userId: String,
    val email: String,
    val trialStartedAt: Long,
    val trialCompletedAt: Long?,
    val trialUsed: Boolean
)
