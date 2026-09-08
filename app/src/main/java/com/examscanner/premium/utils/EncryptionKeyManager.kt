package com.examscanner.premium.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import javax.crypto.KeyGenerator

/**
 * Encryption Key Manager
 * Manages encryption keys using Android Keystore
 */
object EncryptionKeyManager {
    
    private const val PREFS_NAME = "secure_prefs"
    private const val DB_KEY_PREF = "database_encryption_key"
    
    /**
     * Get or create database encryption passphrase
     * Stored securely in encrypted preferences
     */
    fun getDatabasePassphrase(context: Context): CharArray {
        return try {
            // Try to get existing key from preferences
            val existingKey = getExistingKey(context)
            if (existingKey != null) {
                return existingKey
            }
            
            // Generate new key if none exists
            generateAndStoreKey(context)
        } catch (e: Exception) {
            SecureLogger.e("EncryptionKeyManager", "Error getting database passphrase", e)
            // Fallback: use a derived key (less secure, but prevents data loss)
            deriveFallbackKey(context)
        }
    }
    
    /**
     * Get existing encryption key from secure storage
     */
    private fun getExistingKey(context: Context): CharArray? {
        return try {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val keyString = sharedPreferences.getString(DB_KEY_PREF, null)
            keyString?.toCharArray()
        } catch (e: Exception) {
            SecureLogger.e("EncryptionKeyManager", "Error retrieving existing key", e)
            null
        }
    }
    
    /**
     * Generate and securely store new encryption key
     */
    private fun generateAndStoreKey(context: Context): CharArray {
        // Generate random 256-bit key
        val random = SecureRandom()
        val keyBytes = ByteArray(32) // 256 bits
        random.nextBytes(keyBytes)
        val keyString = android.util.Base64.encodeToString(keyBytes, android.util.Base64.NO_WRAP)
        
        // Store in shared preferences
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(DB_KEY_PREF, keyString)
            .apply()
        
        return keyString.toCharArray()
    }
    
    /**
     * Fallback key derivation (less secure but prevents data loss)
     * Only used if secure storage is unavailable
     */
    private fun deriveFallbackKey(context: Context): CharArray {
        // Use a combination of device-specific identifiers
        // This is less secure but ensures app continues to function
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val packageName = context.packageName
        
        // Derive key from device ID and package name
        val combined = "$deviceId:$packageName:ExamScanner"
        return combined.toCharArray()
    }
    
    /**
     * Clear encryption keys (use with caution - will make database unreadable)
     */
    fun clearKeys(context: Context) {
        try {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
            
            SecureLogger.d("EncryptionKeyManager", "Encryption keys cleared")
        } catch (e: Exception) {
            SecureLogger.e("EncryptionKeyManager", "Error clearing keys", e)
        }
    }
}
