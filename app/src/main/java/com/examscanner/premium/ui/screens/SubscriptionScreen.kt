package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

/**
 * SubscriptionScreen - Subscription management entry point (Requirement 16.2).
 *
 * This is the destination reached from Settings > "Manage Subscription". It
 * presents the current tier, the free-tier limits, and premium benefits, and
 * exposes an "Upgrade" action. The Google Play Billing purchase flow is owned by
 * `SubscriptionManager`/`SubscriptionViewModel`, which are constructed by the
 * app startup/DI layer (Task 15). Until that layer supplies the manager, this
 * screen surfaces the entry point and forwards the upgrade intent through
 * [onUpgrade]; [isPremium] reflects the current tier when available.
 *
 * Uses Azure Glass theme colors and content descriptions per workspace
 * conventions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    isPremium: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    Scaffold(
        containerColor = IceWhite,
        topBar = {
            TopAppBar(
                title = { Text("Manage Subscription", fontWeight = FontWeight.Bold, color = TextPrimaryIce) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ElectricBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IceWhite)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(IceWhite)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isPremium) SuccessAzure else IcyCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            if (isPremium) "Premium" else "Free plan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryIce
                        )
                        Text(
                            if (isPremium) "All features unlocked" else "Current tier",
                            fontSize = 13.sp,
                            color = TextSecondaryIce
                        )
                    }
                }
            }

            FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Free tier limits", fontWeight = FontWeight.Bold, color = TextPrimaryIce)
                    Spacer(modifier = Modifier.height(8.dp))
                    LimitRow("Subjects", "Up to 3")
                    LimitRow("Exams per subject", "Up to 5")
                    LimitRow("Scans per exam", "Up to 30")
                }
            }

            FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Premium unlocks", fontWeight = FontWeight.Bold, color = TextPrimaryIce)
                    Spacer(modifier = Modifier.height(8.dp))
                    BenefitRow("Unlimited subjects, exams, and scans")
                    BenefitRow("School-level reports")
                    BenefitRow("Advanced analytics")
                    BenefitRow("Cloud sync across devices")
                }
            }

            if (!isPremium) {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upgrade to Premium", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LimitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondaryIce, fontSize = 14.sp)
        Text(value, color = TextPrimaryIce, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessAzure, modifier = Modifier.size(18.dp))
        Text(text, color = TextPrimaryIce, fontSize = 14.sp)
    }
}
