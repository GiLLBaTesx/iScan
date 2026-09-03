package com.examscanner.premium.utils

/**
 * Password Security Validator
 * Enforces strong password requirements
 */
object PasswordValidator {
    
    /**
     * Minimum password requirements
     */
    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 128
    
    /**
     * Validate password meets security requirements
     */
    fun validatePassword(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()
        
        // Check length
        if (password.length < MIN_LENGTH) {
            errors.add("Password must be at least $MIN_LENGTH characters")
        }
        
        if (password.length > MAX_LENGTH) {
            errors.add("Password must not exceed $MAX_LENGTH characters")
        }
        
        // Check for uppercase letter
        if (!password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }
        
        // Check for lowercase letter
        if (!password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }
        
        // Check for digit
        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one number")
        }
        
        // Check for special character (optional but recommended)
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val hasSpecialChar = password.any { it in specialChars }
        
        return PasswordValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            strength = calculatePasswordStrength(password),
            hasSpecialChar = hasSpecialChar
        )
    }
    
    /**
     * Calculate password strength (0-4)
     */
    fun calculatePasswordStrength(password: String): PasswordStrength {
        var score = 0
        
        // Length bonus
        when {
            password.length >= 12 -> score += 2
            password.length >= 8 -> score += 1
        }
        
        // Character variety
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (password.any { it in specialChars }) score++
        
        // Complexity bonus
        val uniqueChars = password.toSet().size
        if (uniqueChars >= password.length * 0.6) score++
        
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            score <= 6 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
    
    /**
     * Check for common weak passwords
     */
    fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            "password", "12345678", "password1", "Password1",
            "qwerty123", "abc123456", "welcome1", "letmein1",
            "admin123", "user1234"
        )
        return password.lowercase() in commonPasswords.map { it.lowercase() }
    }
    
    /**
     * Get password strength description
     */
    fun getStrengthDescription(strength: PasswordStrength): String {
        return when (strength) {
            PasswordStrength.WEAK -> "Weak - Not recommended"
            PasswordStrength.MEDIUM -> "Medium - Acceptable"
            PasswordStrength.STRONG -> "Strong - Good password"
            PasswordStrength.VERY_STRONG -> "Very Strong - Excellent!"
        }
    }
    
    /**
     * Get password requirements summary
     */
    fun getRequirements(): List<String> {
        return listOf(
            "At least $MIN_LENGTH characters",
            "At least one uppercase letter (A-Z)",
            "At least one lowercase letter (a-z)",
            "At least one number (0-9)",
            "Special characters recommended (!@#$%^&*)"
        )
    }
}

/**
 * Password validation result
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val strength: PasswordStrength,
    val hasSpecialChar: Boolean
)

/**
 * Password strength levels
 */
enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG
}
