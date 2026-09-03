package com.examscanner.premium.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.examscanner.premium.utils.SecureLogger

/**
 * AppLockManager - device-credential / biometric gate for student records.
 *
 * Requirement 8.7 / 13.2: WHEN the app is launched and WHEN it returns to the foreground,
 * require a successful device credential OR biometric authentication (device PIN, pattern,
 * password, or fingerprint) before displaying any student data.
 *
 * This reuses the platform BiometricPrompt with the DEVICE_CREDENTIAL fallback so a single
 * prompt covers biometrics AND the device PIN/pattern/password. It logs only through
 * SecureLogger (never android.util.Log) and never logs sensitive values.
 */
object AppLockManager {

    private const val TAG = "AppLockManager"

    /**
     * Authenticators accepted by the gate: strong/weak biometrics plus the device credential
     * (PIN / pattern / password). DEVICE_CREDENTIAL guarantees the user can always satisfy the
     * gate even without enrolled biometrics, and is required on API 26/27 where BIOMETRIC-only
     * + DEVICE_CREDENTIAL combos are unsupported.
     */
    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Whether the device is capable of satisfying the lock (has biometrics enrolled and/or a
     * secured lock screen). When the device has NO secure lock at all we cannot gate, so callers
     * should treat [LockCapability.NONE] as "unlock immediately" rather than blocking access.
     */
    enum class LockCapability {
        /** A biometric and/or device credential is available to authenticate against. */
        AVAILABLE,

        /** No secure lock configured on the device; the gate cannot be enforced. */
        NONE
    }

    fun getCapability(activity: FragmentActivity): LockCapability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> LockCapability.AVAILABLE
            else -> LockCapability.NONE
        }
    }

    /**
     * Present the authentication prompt. Exactly one of [onSuccess] or [onFailure] is invoked.
     *
     * @param onSuccess called after a verified unlock; student data may then be shown.
     * @param onFailure called when the user cancels or authentication errors out; the caller
     *                  MUST keep student data hidden and offer a retry.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (reason: String) -> Unit
    ) {
        if (getCapability(activity) == LockCapability.NONE) {
            // No secure lock exists on this device — cannot enforce the gate. Fail open so the
            // teacher is not permanently locked out of their own local data.
            SecureLogger.w(TAG, "No device lock configured; skipping app-lock gate")
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                SecureLogger.d(TAG, "App unlocked")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Non-fatal, user-driven cancellations shouldn't be treated as an attack; just
                // keep the gate up. We log the code only (no user data).
                SecureLogger.w(TAG, "App unlock failed (code=$errorCode)")
                onFailure(errString.toString())
            }
            // onAuthenticationFailed (single mismatched attempt) is intentionally not overridden:
            // BiometricPrompt lets the user retry within the same prompt.
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            // A negative/cancel button is disallowed when DEVICE_CREDENTIAL is enabled; the
            // system supplies the credential fallback affordance instead.
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to present app-lock prompt", e)
            onFailure(e.message ?: "Unable to start authentication")
        }
    }
}
