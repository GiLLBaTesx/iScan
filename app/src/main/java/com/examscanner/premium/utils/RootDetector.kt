package com.examscanner.premium.utils

import android.os.Build
import java.io.File
import java.io.IOException

/**
 * Root Detection Utility
 * Detects if the device is rooted/jailbroken
 */
object RootDetector {
    
    /**
     * Check if device is rooted using multiple detection methods
     */
    fun isDeviceRooted(): Boolean {
        return checkRootBinaries() || 
               checkRootApps() || 
               checkTestKeys() ||
               checkSuCommand()
    }
    
    /**
     * Method 1: Check for common root binary files
     */
    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/Magisk.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        return paths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Method 2: Check for common root management apps
     */
    private fun checkRootApps(): Boolean {
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk"
        )
        
        return rootApps.any { packageName ->
            try {
                File("/data/data/$packageName").exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Method 3: Check if build tags contain "test-keys"
     * Official Android builds use "release-keys"
     */
    private fun checkTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }
    
    /**
     * Method 4: Try to execute su command
     */
    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()
            output.isNotEmpty()
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get root detection details for debugging
     */
    fun getRootDetails(): String {
        val details = mutableListOf<String>()
        
        if (checkRootBinaries()) {
            details.add("Root binaries detected")
        }
        if (checkRootApps()) {
            details.add("Root apps detected")
        }
        if (checkTestKeys()) {
            details.add("Test-keys build detected")
        }
        if (checkSuCommand()) {
            details.add("SU command available")
        }
        
        return if (details.isEmpty()) {
            "No root indicators found"
        } else {
            details.joinToString(", ")
        }
    }
    
    /**
     * Check if device is an emulator
     * Emulators are often used for testing/hacking
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT)
    }
}
