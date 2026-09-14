package com.examscanner.premium.ui.screens.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Onboarding page data model
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val features: List<String>
)

/**
 * Onboarding content - 4 pages explaining key features
 */
object OnboardingContent {
    val pages = listOf(
        OnboardingPage(
            title = "Scan Answer Sheets Instantly",
            description = "Transform traditional paper exams into digital grades in seconds",
            icon = Icons.Default.CameraAlt,
            features = listOf(
                "Scan multiple-choice bubble sheets",
                "Automatic answer detection",
                "Works offline, no internet needed",
                "Process entire classes in minutes"
            )
        ),
        OnboardingPage(
            title = "Grade with Precision",
            description = "Set answer keys and get instant, accurate results",
            icon = Icons.Default.CheckCircle,
            features = listOf(
                "Create custom answer keys",
                "Automatic grading and scoring",
                "Track student performance",
                "Flag problematic answers"
            )
        ),
        OnboardingPage(
            title = "Powerful Analytics",
            description = "Gain insights into student performance and item difficulty",
            icon = Icons.Default.Analytics,
            features = listOf(
                "Item analysis with difficulty metrics",
                "Student performance tracking",
                "MELC competency mapping",
                "Export to Excel and PDF"
            )
        ),
        OnboardingPage(
            title = "Secure & Private",
            description = "Your data stays safe with military-grade encryption",
            icon = Icons.Default.Security,
            features = listOf(
                "AES-256 database encryption",
                "Data stored locally on device",
                "No cloud sync required",
                "Complete privacy and control"
            )
        )
    )
}
