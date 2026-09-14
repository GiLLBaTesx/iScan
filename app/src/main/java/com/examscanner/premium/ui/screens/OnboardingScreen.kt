package com.examscanner.premium.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.theme.*
import kotlinx.coroutines.launch

// Required for HorizontalPager
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Onboarding page data model
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * Onboarding content - 4 pages explaining key features
 */
val onboardingPages = listOf(
    OnboardingPage(
        title = "Scan & Grade Instantly",
        description = "Transform your smartphone into a powerful exam scanner. Capture answer sheets with your camera and get instant results.",
        icon = Icons.Default.CameraAlt,
        accentColor = LuminousAzure
    ),
    OnboardingPage(
        title = "Smart Analytics",
        description = "Detailed item analysis, difficulty indices, and student performance insights. Export to Excel or PDF with one tap.",
        icon = Icons.Default.Analytics,
        accentColor = IcyCyan
    ),
    OnboardingPage(
        title = "MELC Integration",
        description = "Tag questions with DepEd MELCs and track competency mastery. Perfect for Filipino educators.",
        icon = Icons.Default.School,
        accentColor = ElectricBlue
    ),
    OnboardingPage(
        title = "Secure & Private",
        description = "All exam data stored locally with military-grade encryption. Your students' information stays on your device.",
        icon = Icons.Default.Security,
        accentColor = SuccessGreen
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    
    // Animated background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundWhite,
                        IceWhite,
                        BackgroundWhite
                    ),
                    startY = animatedOffset * 1000f,
                    endY = (animatedOffset + 1) * 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text("Skip")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    currentPage = page,
                    totalPages = onboardingPages.size
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page indicators
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    PageIndicator(
                        isActive = index == pagerState.currentPage,
                        color = if (index == pagerState.currentPage) 
                            onboardingPages[index].accentColor 
                        else 
                            TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ElectricBlue
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(ElectricBlue, LuminousAzure))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                // Next / Get Started button
                Button(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < onboardingPages.size - 1) "Next" else "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (pagerState.currentPage < onboardingPages.size - 1) 
                            Icons.Default.ArrowForward 
                        else 
                            Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    currentPage: Int,
    totalPages: Int
) {
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon in frosted glass card
        Card(
            modifier = Modifier
                .size(160.dp)
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Blurred background circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .blur(30.dp)
                        .background(
                            page.accentColor.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
                
                // Icon
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = page.accentColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Feature highlights (optional)
        when (currentPage) {
            0 -> FeatureHighlight("Camera", "No special hardware needed")
            1 -> FeatureHighlight("Analytics", "Item analysis & difficulty indices")
            2 -> FeatureHighlight("DepEd", "Aligned with K-12 curriculum")
            3 -> FeatureHighlight("Offline", "Works without internet")
        }
    }
}

@Composable
fun FeatureHighlight(
    label: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = IceWhite
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(LuminousAzure, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    isActive: Boolean,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(
                width = if (isActive) 24.dp else 8.dp,
                height = 8.dp
            )
            .clip(CircleShape)
            .background(color)
    )
}
