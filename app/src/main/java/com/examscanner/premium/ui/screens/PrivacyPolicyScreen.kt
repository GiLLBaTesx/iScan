package com.examscanner.premium.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.theme.IceBlue
import com.examscanner.premium.ui.theme.TextPrimaryIce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = "Privacy Policy",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Last Updated: September 4, 2026",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Introduction
            SectionTitle("1. Introduction")
            SectionText(
                "iScan Technologies ('we', 'our', or 'us') is committed to protecting your privacy. " +
                "This Privacy Policy explains how we collect, use, and safeguard your information when you use " +
                "the iScan mobile application ('the App')."
            )
            
            SectionText(
                "By using iScan, you agree to the collection and use of information in accordance with this policy. " +
                "If you do not agree with our policies and practices, please do not use the App."
            )
            
            // Information We Collect
            SectionTitle("2. Information We Collect")
            
            SubSectionTitle("2.1 Information You Provide")
            SectionText(
                "When you create an account, we collect:"
            )
            BulletPoint("Email address")
            BulletPoint("Display name")
            BulletPoint("School name (optional)")
            BulletPoint("Grade level and subjects taught (optional)")
            BulletPoint("Password (encrypted and never stored in plain text)")
            
            SubSectionTitle("2.2 Student Data")
            SectionText(
                "When you use the exam scanning features:"
            )
            BulletPoint("Student names and IDs (provided by you)")
            BulletPoint("Exam scores and answers")
            BulletPoint("Scanned answer sheet images (stored locally)")
            BulletPoint("Performance analytics and statistics")
            
            SubSectionTitle("2.3 Device Information")
            SectionText(
                "We collect limited device information for security and trial management:"
            )
            BulletPoint("Device model and manufacturer")
            BulletPoint("Android version")
            BulletPoint("Unique device identifier (hashed for privacy)")
            BulletPoint("App version and crash reports")
            
            SubSectionTitle("2.4 Usage Data")
            SectionText(
                "We collect anonymous usage statistics to improve the App:"
            )
            BulletPoint("Feature usage frequency")
            BulletPoint("Performance metrics")
            BulletPoint("Error logs (anonymized)")
            
            // How We Use Your Information
            SectionTitle("3. How We Use Your Information")
            SectionText("We use the collected information for:")
            BulletPoint("Providing and maintaining the App's functionality")
            BulletPoint("User authentication and account management")
            BulletPoint("Storing your exam data securely on your device")
            BulletPoint("Preventing trial abuse and fraudulent usage")
            BulletPoint("Improving app performance and user experience")
            BulletPoint("Sending important service announcements")
            BulletPoint("Providing customer support")
            
            // Data Storage and Security
            SectionTitle("4. Data Storage and Security")
            
            SubSectionTitle("4.1 Local Storage")
            SectionText(
                "ALL student exam data is stored locally on your device using military-grade SQLCipher " +
                "encryption (AES-256). This data includes:"
            )
            BulletPoint("Student names and IDs")
            BulletPoint("Exam scores and answers")
            BulletPoint("Answer sheet images")
            BulletPoint("Analytics and reports")
            
            SectionText(
                "⚠️ IMPORTANT: We do NOT upload or sync student exam data to our servers or cloud storage. " +
                "Your students' information stays on your device."
            )
            
            SubSectionTitle("4.2 Cloud Storage")
            SectionText(
                "Only the following data is stored in our secure Firebase servers:"
            )
            BulletPoint("Your account information (email, name, school)")
            BulletPoint("Subscription status and trial period")
            BulletPoint("Device trial usage (for abuse prevention)")
            
            SubSectionTitle("4.3 Security Measures")
            SectionText("We implement industry-standard security measures:")
            BulletPoint("AES-256 encryption for local database")
            BulletPoint("HTTPS/TLS encryption for all network communications")
            BulletPoint("Firebase Authentication with secure password hashing")
            BulletPoint("Android Keystore for encryption key management")
            BulletPoint("Root detection and security warnings")
            BulletPoint("Regular security audits and updates")
            
            // Data Sharing
            SectionTitle("5. Data Sharing and Disclosure")
            SectionText(
                "We DO NOT sell, trade, or rent your personal information to third parties. " +
                "Your student exam data is NEVER shared with anyone."
            )
            
            SectionText("We may share limited information only in these cases:")
            BulletPoint("With your explicit consent")
            BulletPoint("To comply with legal obligations or court orders")
            BulletPoint("To protect our rights, property, or safety")
            BulletPoint("In connection with a merger or acquisition (with user notification)")
            
            // Third-Party Services
            SectionTitle("6. Third-Party Services")
            SectionText("iScan uses the following third-party services:")
            
            BulletPoint("Firebase Authentication (Google) - User authentication")
            BulletPoint("Firebase Firestore (Google) - User profile storage")
            BulletPoint("Google ML Kit - On-device text recognition (offline)")
            
            SectionText(
                "These services have their own privacy policies. Google's privacy policy can be found at: " +
                "https://policies.google.com/privacy"
            )
            
            // Your Rights
            SectionTitle("7. Your Privacy Rights")
            SectionText("You have the right to:")
            BulletPoint("Access your personal data")
            BulletPoint("Correct inaccurate data")
            BulletPoint("Request deletion of your data")
            BulletPoint("Export your data")
            BulletPoint("Withdraw consent for data processing")
            BulletPoint("Object to data processing")
            BulletPoint("Lodge a complaint with supervisory authorities")
            
            // GDPR Rights
            SubSectionTitle("7.1 GDPR Rights (EU Users)")
            SectionText(
                "If you are in the European Union, you have additional rights under GDPR:"
            )
            BulletPoint("Right to data portability")
            BulletPoint("Right to restrict processing")
            BulletPoint("Right to object to automated decision-making")
            
            // Data Retention
            SectionTitle("8. Data Retention")
            SectionText(
                "• Account data: Retained while your account is active" +
                "\n• Deleted account data: Permanently deleted within 30 days" +
                "\n• Local exam data: Stored on your device until you delete it" +
                "\n• Backup data: Stored locally until manually deleted" +
                "\n• Analytics data: Anonymized and retained for 24 months"
            )
            
            // Children's Privacy
            SectionTitle("9. Children's Privacy")
            SectionText(
                "iScan is designed for use by educators (adults 18+). While the App processes student data " +
                "including minors' information, we do not knowingly collect personal information directly from " +
                "children under 13."
            )
            
            SectionText(
                "If you are an educator using iScan with students under 13, you are responsible for:"
            )
            BulletPoint("Obtaining necessary parental/guardian consents")
            BulletPoint("Complying with COPPA and local data protection laws")
            BulletPoint("Ensuring proper data handling and security")
            
            // International Users
            SectionTitle("10. International Data Transfers")
            SectionText(
                "Your account information may be transferred to and maintained on servers located outside your " +
                "country. We ensure appropriate safeguards are in place for such transfers."
            )
            
            SectionText(
                "Student exam data is ONLY stored locally on your device and is never transferred internationally."
            )
            
            // Changes to Privacy Policy
            SectionTitle("11. Changes to This Privacy Policy")
            SectionText(
                "We may update our Privacy Policy from time to time. We will notify you of any changes by:"
            )
            BulletPoint("Posting the new Privacy Policy in the App")
            BulletPoint("Updating the 'Last Updated' date")
            BulletPoint("Sending a notification for significant changes")
            
            SectionText(
                "Continued use of the App after changes constitutes acceptance of the updated policy."
            )
            
            // Contact Information
            SectionTitle("12. Contact Us")
            SectionText(
                "If you have questions about this Privacy Policy or want to exercise your privacy rights, " +
                "please contact us:"
            )
            
            ContactInfo("Email", "privacy@iscan.app")
            ContactInfo("Support", "support@iscan.app")
            ContactInfo("Address", "iScan Technologies, Philippines")
            ContactInfo("Response Time", "Within 48 hours for privacy requests")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = IceBlue // Light blue tint - commonly used in app theme
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔒 Your Privacy is Our Priority",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryIce // Dark text on light blue background
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We are committed to protecting your data and your students' privacy. " +
                                "All exam data stays on your device, encrypted and secure.",
                        fontSize = 14.sp,
                        color = TextPrimaryIce // Dark text on light blue background
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun SubSectionTitle(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SectionText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 4.dp)
    ) {
        Text(
            text = "• ",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ContactInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "$label: ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
