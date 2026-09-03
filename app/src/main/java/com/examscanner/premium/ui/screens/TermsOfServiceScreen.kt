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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
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
                text = "Terms of Service",
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
            SectionTitle("1. Acceptance of Terms")
            SectionText(
                "By accessing and using iScan ('the App', 'Service'), you accept and agree to be bound by " +
                "these Terms of Service. If you do not agree to these terms, please do not use the App."
            )
            
            SectionText(
                "These terms apply to all users including educators, administrators, and any person accessing " +
                "the Service."
            )
            
            // Eligibility
            SectionTitle("2. Eligibility")
            SectionText(
                "You must be at least 18 years old to use iScan. By using the Service, you represent and " +
                "warrant that:"
            )
            BulletPoint("You are 18 years of age or older")
            BulletPoint("You have the legal capacity to enter into these terms")
            BulletPoint("You are a licensed educator or authorized to process student data")
            BulletPoint("Your use complies with applicable laws and regulations")
            
            // License and Usage
            SectionTitle("3. License and Usage Rights")
            
            SubSectionTitle("3.1 Grant of License")
            SectionText(
                "Subject to your compliance with these Terms, we grant you a limited, non-exclusive, " +
                "non-transferable, revocable license to use iScan for legitimate educational purposes."
            )
            
            SubSectionTitle("3.2 Trial Period")
            SectionText(
                "New users receive a 14-day free trial. After the trial period, continued use requires " +
                "an active subscription. Terms:"
            )
            BulletPoint("One trial per device")
            BulletPoint("Trial begins upon account creation")
            BulletPoint("No credit card required for trial")
            BulletPoint("Automatic expiration after 14 days")
            
            SubSectionTitle("3.3 Permitted Uses")
            SectionText("You may use iScan to:")
            BulletPoint("Scan and grade student answer sheets")
            BulletPoint("Generate exam analytics and reports")
            BulletPoint("Store exam data locally on your device")
            BulletPoint("Export data in supported formats (PDF, Excel)")
            BulletPoint("Create and manage exam templates")
            
            SubSectionTitle("3.4 Prohibited Uses")
            SectionText("You may NOT:")
            BulletPoint("Share your account with others")
            BulletPoint("Reverse engineer or decompile the App")
            BulletPoint("Remove or modify copyright notices")
            BulletPoint("Use the App for illegal or unauthorized purposes")
            BulletPoint("Abuse the trial period system")
            BulletPoint("Attempt to circumvent security measures")
            BulletPoint("Use automated systems to access the Service")
            BulletPoint("Sell, resell, or commercialize the Service")
            
            // Subscription and Payment
            SectionTitle("4. Subscription and Payment")
            
            SubSectionTitle("4.1 Subscription Plans")
            SectionText(
                "After the trial period, you must subscribe to continue using iScan. " +
                "Subscription plans and pricing are displayed in the App."
            )
            
            SubSectionTitle("4.2 Billing")
            BulletPoint("Subscriptions are billed in advance")
            BulletPoint("Auto-renewal unless cancelled")
            BulletPoint("Prices subject to change with 30 days notice")
            BulletPoint("No refunds for partial periods")
            
            SubSectionTitle("4.3 Cancellation")
            SectionText(
                "You may cancel your subscription at any time. Upon cancellation:"
            )
            BulletPoint("Access continues until end of billing period")
            BulletPoint("No pro-rated refunds")
            BulletPoint("Local data remains accessible (view-only)")
            BulletPoint("New exams cannot be created")
            
            // Data and Privacy
            SectionTitle("5. Data and Privacy")
            
            SubSectionTitle("5.1 Your Responsibilities")
            SectionText("You are responsible for:")
            BulletPoint("Obtaining necessary consents to process student data")
            BulletPoint("Complying with applicable data protection laws (GDPR, COPPA, etc.)")
            BulletPoint("Maintaining the security of your device")
            BulletPoint("Backing up your data")
            BulletPoint("Not sharing student data inappropriately")
            
            SubSectionTitle("5.2 Our Responsibilities")
            SectionText("We commit to:")
            BulletPoint("Keep student exam data on your device only")
            BulletPoint("Encrypt all data with industry-standard encryption")
            BulletPoint("Never sell or share your students' data")
            BulletPoint("Maintain reasonable security measures")
            BulletPoint("Comply with our Privacy Policy")
            
            // Intellectual Property
            SectionTitle("6. Intellectual Property")
            SectionText(
                "All content, features, and functionality of iScan, including software, text, graphics, logos, " +
                "and design, are owned by iScan Technologies and protected by international copyright, " +
                "trademark, and intellectual property laws."
            )
            
            SubSectionTitle("6.1 Your Content")
            SectionText(
                "You retain all rights to the student data and exam content you input into iScan. " +
                "We claim no ownership over your data."
            )
            
            // Disclaimers and Limitations
            SectionTitle("7. Disclaimers and Limitations")
            
            SubSectionTitle("7.1 Service Availability")
            SectionText(
                "iScan is provided 'as is' without warranties of any kind. We do not guarantee:"
            )
            BulletPoint("Uninterrupted or error-free operation")
            BulletPoint("100% accuracy in scanning and grading")
            BulletPoint("Compatibility with all devices")
            BulletPoint("Permanent availability of any feature")
            
            SubSectionTitle("7.2 Limitation of Liability")
            SectionText(
                "TO THE MAXIMUM EXTENT PERMITTED BY LAW, ISCAN TECHNOLOGIES SHALL NOT BE LIABLE FOR:"
            )
            BulletPoint("Any indirect, incidental, or consequential damages")
            BulletPoint("Loss of data, profits, or business opportunities")
            BulletPoint("Scanning or grading errors")
            BulletPoint("Device malfunction or data loss")
            BulletPoint("Third-party actions or content")
            
            SectionText(
                "Our total liability shall not exceed the amount you paid for the Service in the last 12 months."
            )
            
            // Accuracy and User Verification
            SectionTitle("8. Accuracy and Verification")
            SectionText(
                "⚠️ IMPORTANT: While iScan uses advanced optical recognition technology, you must:"
            )
            BulletPoint("Verify scanned results for accuracy")
            BulletPoint("Review grades before finalizing")
            BulletPoint("Not rely solely on automated scanning")
            BulletPoint("Manually check critical exams")
            
            SectionText(
                "iScan is a tool to assist educators, not replace human judgment. " +
                "Final grading decisions remain your responsibility."
            )
            
            // Account Termination
            SectionTitle("9. Account Termination")
            
            SubSectionTitle("9.1 Your Right to Terminate")
            SectionText("You may delete your account at any time through the App settings.")
            
            SubSectionTitle("9.2 Our Right to Terminate")
            SectionText("We may suspend or terminate your account if:")
            BulletPoint("You violate these Terms of Service")
            BulletPoint("You abuse the trial period system")
            BulletPoint("Payment fails (for subscribed users)")
            BulletPoint("Required by law or legal request")
            BulletPoint("The Service is discontinued")
            
            SectionText(
                "We will provide reasonable notice before termination unless immediate action is required " +
                "for security or legal reasons."
            )
            
            // Changes to Service
            SectionTitle("10. Changes to Service and Terms")
            SectionText(
                "We reserve the right to:"
            )
            BulletPoint("Modify or discontinue features")
            BulletPoint("Update these Terms of Service")
            BulletPoint("Change pricing with 30 days notice")
            BulletPoint("Add or remove functionality")
            
            SectionText(
                "Continued use after changes constitutes acceptance. Material changes will be announced " +
                "in the App."
            )
            
            // Support and Updates
            SectionTitle("11. Support and Updates")
            SectionText(
                "We provide:"
            )
            BulletPoint("Email support (response within 48 hours)")
            BulletPoint("Regular app updates and improvements")
            BulletPoint("Bug fixes and security patches")
            BulletPoint("Feature enhancements")
            
            SectionText(
                "Support is provided on a best-effort basis. We do not guarantee specific response times " +
                "or resolution timeframes."
            )
            
            // Governing Law
            SectionTitle("12. Governing Law and Dispute Resolution")
            SectionText(
                "These Terms shall be governed by and construed in accordance with the laws of the Philippines, " +
                "without regard to conflict of law principles."
            )
            
            SubSectionTitle("12.1 Dispute Resolution")
            SectionText("In case of disputes:")
            BulletPoint("First, contact us to resolve informally")
            BulletPoint("Mediation may be attempted")
            BulletPoint("Arbitration in the Philippines")
            BulletPoint("Individual basis only (no class actions)")
            
            // Indemnification
            SectionTitle("13. Indemnification")
            SectionText(
                "You agree to indemnify and hold harmless iScan Technologies from any claims, damages, " +
                "or expenses arising from:"
            )
            BulletPoint("Your use of the Service")
            BulletPoint("Your violation of these Terms")
            BulletPoint("Your violation of any third-party rights")
            BulletPoint("Your student data processing activities")
            
            // Severability
            SectionTitle("14. Severability")
            SectionText(
                "If any provision of these Terms is found to be unenforceable or invalid, that provision " +
                "will be limited or eliminated to the minimum extent necessary, and the remaining provisions " +
                "will remain in full force and effect."
            )
            
            // Entire Agreement
            SectionTitle("15. Entire Agreement")
            SectionText(
                "These Terms, together with our Privacy Policy, constitute the entire agreement between you " +
                "and iScan Technologies regarding the Service and supersede all prior agreements."
            )
            
            // Contact Information
            SectionTitle("16. Contact Information")
            SectionText(
                "For questions about these Terms of Service:"
            )
            
            ContactInfo("Email", "legal@iscan.app")
            ContactInfo("Support", "support@iscan.app")
            ContactInfo("Address", "iScan Technologies, Philippines")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📜 By Using iScan, You Agree",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your continued use of iScan constitutes acceptance of these Terms of Service. " +
                                "If you do not agree, please discontinue use and delete your account.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
