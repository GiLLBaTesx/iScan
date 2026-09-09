package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.layout.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.verticalScroll
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.Icons
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.filled.ArrowBack
import com.examscanner.premium.ui.theme.*
import androidx.compose.material3.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.runtime.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.Modifier
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.graphics.Brush
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.unit.dp
import com.examscanner.premium.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFC),
                        Color(0xFFF0F4F8)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ElectricBlue.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "🔒 Your Privacy Matters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All student data is stored locally on your device. We do not collect, access, or share any information.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                }
                
                Text(
                    text = "Last Updated: September 4, 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
                
                // Introduction
                SectionTitle("Introduction")
                SectionText(
                    "Offline Assessment is committed to protecting the privacy of teachers and students. " +
                    "This Privacy Policy explains how we collect, use, store, and protect information when you use our mobile application."
                )
                
                // Data Controller
                SectionTitle("Data Controller")
                HighlightCard(
                    "You (the teacher) are the data controller for your students' information. " +
                    "We (the app developers) do not have access to any data stored in the app."
                )
                
                // Developer Responsibilities
                SectionTitle("Developer Responsibilities")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ElectricBlue.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "As the app developers, we commit to:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text("✓ No data collection or external transmission", style = MaterialTheme.typography.bodySmall)
                        Text("✓ No tracking, analytics, or advertising", style = MaterialTheme.typography.bodySmall)
                        Text("✓ Local storage only architecture", style = MaterialTheme.typography.bodySmall)
                        Text("✓ Regular security updates and bug fixes", style = MaterialTheme.typography.bodySmall)
                        Text("✓ Transparent privacy policy updates", style = MaterialTheme.typography.bodySmall)
                        Text("✓ Prompt response to security issues", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "We are NOT responsible for:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("• User compliance with laws and policies", style = MaterialTheme.typography.bodySmall)
                        Text("• Device security (lost/stolen devices)", style = MaterialTheme.typography.bodySmall)
                        Text("• How users share exported data", style = MaterialTheme.typography.bodySmall)
                        Text("• Data loss due to user error", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // What We Collect
                SectionTitle("Information We Store Locally")
                SectionText("The app allows you to store the following on YOUR device:")
                BulletPoint("Student names and identification numbers")
                BulletPoint("Exam scores and results")
                BulletPoint("Answer sheet scans (processed locally)")
                BulletPoint("Subject folders and assessments")
                BulletPoint("DepEd MELCs mappings")
                
                // What We DON'T Collect
                SectionTitle("What We DO NOT Collect")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓ No personal information collected", style = MaterialTheme.typography.bodyMedium)
                        Text("✓ No location tracking", style = MaterialTheme.typography.bodyMedium)
                        Text("✓ No contact access", style = MaterialTheme.typography.bodyMedium)
                        Text("✓ No data shared with third parties", style = MaterialTheme.typography.bodyMedium)
                        Text("✓ No external server uploads", style = MaterialTheme.typography.bodyMedium)
                        Text("✓ No analytics or tracking", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                // Data Storage
                SectionTitle("How We Store Your Data")
                SectionText("All data is stored locally on your Android device:")
                BulletPoint("Encrypted SQLite database in app-private storage")
                BulletPoint("No internet connection required")
                BulletPoint("No data transmitted to external servers")
                BulletPoint("Protected by Android OS security")
                
                // Camera Access
                SectionTitle("Camera Access")
                SectionText(
                    "Camera is only used for scanning answer sheets. Photos are processed immediately " +
                    "and discarded. We do not save raw photos unless you explicitly export them."
                )
                
                // Data Security
                SectionTitle("Your Responsibility")
                HighlightCard(
                    "As the data controller, you are responsible for:\n\n" +
                    "• Securing your device with PIN/password/biometric lock\n" +
                    "• Backing up important data to secure cloud storage\n" +
                    "• Not sharing your device with unauthorized persons\n" +
                    "• Complying with your school's data protection policies"
                )
                
                // Data Sharing
                SectionTitle("Data Sharing")
                SectionText(
                    "We do NOT share any data. The app allows YOU to share if you choose:\n\n" +
                    "• Export backup files (you decide where)\n" +
                    "• Share reports via email (you choose recipients)\n" +
                    "• Print or export PDFs (under your control)"
                )
                
                // Children's Privacy
                SectionTitle("Children's Privacy & Teacher Responsibilities")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9500).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "This app is designed for teacher use only.",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("Teachers must:", fontWeight = FontWeight.SemiBold)
                        Text("• Obtain necessary consent from parents/guardians")
                        Text("• Comply with DepEd regulations and school policies")
                        Text("• Follow Philippine Data Privacy Act (RA 10173)")
                        Text("• Only collect data necessary for assessment")
                        Text("• Use student IDs instead of full names when possible")
                    }
                }
                
                // Best Practices
                SectionTitle("Security Best Practices")
                SectionText("To protect student privacy:")
                BulletPoint("Enable device lock (PIN/password/fingerprint)")
                BulletPoint("Enable device encryption (default on modern Android)")
                BulletPoint("Create regular backups to secure cloud storage")
                BulletPoint("Delete student data after school year ends")
                BulletPoint("Don't leave device unattended")
                BulletPoint("Use 'Clear All Data' feature (creates safety backup)")
                
                // Your Rights
                SectionTitle("Your Rights (Philippine Data Privacy Act)")
                BulletPoint("Access: View all data at any time")
                BulletPoint("Rectification: Edit or correct information")
                BulletPoint("Erasure: Delete records or clear all data")
                BulletPoint("Data Portability: Export in CSV, PDF, or database backup")
                BulletPoint("Object: Stop using the app at any time")
                
                // Compliance
                SectionTitle("Legal Compliance")
                SectionText("This app helps you comply with:")
                BulletPoint("Philippine Data Privacy Act (RA 10173)")
                BulletPoint("DepEd Data Privacy Guidelines")
                BulletPoint("FERPA principles")
                BulletPoint("GDPR principles (where applicable)")
                
                HighlightCard(
                    "Important: You (the teacher) are responsible for ensuring your use of the app " +
                    "complies with your school's policies and applicable laws."
                )
                
                // Contact
                SectionTitle("Contact Information")
                SectionText(
                    "For privacy questions:\n\n" +
                    "• Contact your school's Data Protection Officer\n" +
                    "• Philippine National Privacy Commission: https://privacy.gov.ph"
                )
                
                // Consent
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ElectricBlue.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "By Using This App",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You acknowledge that you have read and understood this Privacy Policy, " +
                            "and you accept responsibility as the data controller for your students' information.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Offline Assessment v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1C1E),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF3A3A3C)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = ElectricBlue
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF3A3A3C),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HighlightCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9500).copy(alpha = 0.05f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1C1C1E),
            modifier = Modifier.padding(16.dp)
        )
    }
}
