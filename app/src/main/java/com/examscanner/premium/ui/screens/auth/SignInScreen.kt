package com.examscanner.premium.ui.screens.auth

import androidx.compose.foundation.background
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.layout.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.text.KeyboardActions
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.verticalScroll
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.Icons
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.filled.ArrowBack
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.filled.Visibility
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.filled.VisibilityOff
import com.examscanner.premium.ui.theme.*
import androidx.compose.material3.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.runtime.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.Alignment
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.Modifier
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.focus.FocusDirection
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.graphics.Brush
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.platform.LocalFocusManager
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.input.ImeAction
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.input.KeyboardType
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.input.VisualTransformation
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.style.TextAlign
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.unit.dp
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.ui.components.GlassmorphicCard
import com.examscanner.premium.ui.components.PrimaryActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current
    
    fun validateAndSubmit() {
        var hasError = false
        
        if (email.isBlank()) {
            emailError = "Email is required"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email address"
            hasError = true
        } else {
            emailError = null
        }
        
        if (password.isBlank()) {
            passwordError = "Password is required"
            hasError = true
        } else {
            passwordError = null
        }
        
        if (!hasError) {
            onSignIn(email.trim(), password)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F4FF),
                        Color(0xFFE6EEFF),
                        Color(0xFFD6E4FF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "Welcome Back",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                
                Text(
                    text = "Sign in to continue",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Form Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                emailError = null
                            },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                        
                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                passwordError = null
                            },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it) } },
                            visualTransformation = if (passwordVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff 
                                        else Icons.Default.Visibility,
                                        "Toggle password visibility"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    validateAndSubmit()
                                }
                            )
                        )
                        
                        // Forgot Password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onForgotPasswordClick) {
                                Text(
                                    text = "Forgot Password?",
                                    color = ElectricBlue,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                
                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sign In Button
                PrimaryActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sign Up Link
                TextButton(onClick = onSignUpClick) {
                    Text(
                        text = "Don't have an account? Sign Up",
                        color = ElectricBlue,
                        fontSize = 15.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
