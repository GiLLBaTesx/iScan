package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.examscanner.premium.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.examscanner.premium.auth.AppLockManager
import com.examscanner.premium.ui.theme.ElectricBlue
import com.examscanner.premium.ui.theme.IceWhite
import com.examscanner.premium.ui.theme.IcyCyan
import com.examscanner.premium.ui.theme.TextSecondaryIce

/**
 * AppLockGate - wraps the app content behind a device-credential / biometric prompt.
 *
 * Requirement 8.7 / 13.2: student data must not be visible until the teacher passes a device
 * credential or biometric check, both on launch AND on every return-to-foreground. Locking is
 * driven by the ON_START lifecycle event (fires on cold start and each foreground return) and
 * the app is re-locked on ON_STOP so backgrounding always re-arms the gate.
 *
 * While locked, [content] is never composed, so no student records are rendered underneath the
 * lock overlay.
 */
@Composable
fun AppLockGate(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    // If we're not hosted by a FragmentActivity we cannot present BiometricPrompt. Rather than
    // hard-crash, fall through to the content (the manifest/activity guarantees FragmentActivity
    // in production; this only guards previews/tests).
    if (activity == null) {
        content()
        return
    }

    // Use rememberSaveable so an already-passed unlock survives Activity recreation (rotation,
    // dark-mode/font-scale changes, and the app's own per-app locale switch). With plain remember
    // the value reset to false on every config change and the ON_START observer below re-prompted
    // biometrics mid-session. A genuine background still clears it via ON_STOP (see below), so
    // Req 8.7 re-lock-on-background behavior is preserved. prompting/lastError stay transient.
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var prompting by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    fun requestUnlock() {
        if (unlocked || prompting) return
        prompting = true
        lastError = null
        AppLockManager.authenticate(
            activity = activity,
            title = activity.getString(R.string.app_lock_prompt_title),
            subtitle = activity.getString(R.string.app_lock_prompt_subtitle),
            onSuccess = {
                prompting = false
                unlocked = true
            },
            onFailure = { reason ->
                prompting = false
                unlocked = false
                lastError = reason
            }
        )
    }

    // Re-arm on background, prompt on foreground. Registering the observer also fires for the
    // current state, so a cold start (ON_START) triggers the initial prompt.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> requestUnlock()
                Lifecycle.Event.ON_STOP -> {
                    // Backgrounded: hide student data and require re-authentication next time.
                    unlocked = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (unlocked) {
        content()
    } else {
        AppLockScreen(
            errorMessage = lastError,
            onUnlockClick = { requestUnlock() }
        )
    }
}

@Composable
private fun AppLockScreen(
    errorMessage: String?,
    onUnlockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceWhite)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = stringResource(R.string.app_lock_locked_description),
            modifier = Modifier.size(72.dp),
            tint = ElectricBlue
        )

        Text(
            text = stringResource(R.string.app_lock_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )

        Text(
            text = stringResource(R.string.app_lock_message),
            fontSize = 14.sp,
            color = TextSecondaryIce,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = TextSecondaryIce,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .semantics { contentDescription = "Authentication error: $errorMessage" }
            )
        }

        Button(
            onClick = onUnlockClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IcyCyan)
        ) {
            Text(
                text = stringResource(R.string.app_lock_unlock),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}
