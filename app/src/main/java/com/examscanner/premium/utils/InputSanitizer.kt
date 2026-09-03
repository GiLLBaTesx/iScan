package com.examscanner.premium.utils

/**
 * Input Sanitization Utilities
 * Prevents injection attacks and path traversal
 */
object InputSanitizer {
    
    /**
     * Sanitize file names to prevent path traversal
     * Removes special characters that could be exploited
     */
    fun sanitizeFileName(input: String): String {
        return input
            .replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")  // Only alphanumeric, underscore, hyphen, dot
            .replace(Regex("\\.{2,}"), "_")  // Prevent ".." path traversal
            .take(255)  // Max file name length on most systems
            .trim('_', '.')  // Remove leading/trailing special chars
            .ifEmpty { "unnamed_file" }  // Fallback for empty strings
    }
    
    /**
     * Sanitize student names to prevent XSS and injection
     * Preserves common name characters while removing dangerous ones
     */
    fun sanitizeStudentName(input: String): String {
        return input
            .trim()
            .take(100)  // Reasonable name length limit
            .replace(Regex("[<>\"'%;&\\\\]"), "")  // Remove potential XSS/injection chars
            .replace(Regex("\\s{2,}"), " ")  // Normalize multiple spaces
            .ifEmpty { "Unknown" }  // Fallback for empty names
    }
    
    /**
     * Sanitize exam/subject names
     * Allows more characters but prevents injection
     */
    fun sanitizeExamName(input: String): String {
        return input
            .trim()
            .take(200)  // Reasonable exam name length
            .replace(Regex("[<>\"'%;\\\\]"), "")  // Remove dangerous characters
            .replace(Regex("\\s{2,}"), " ")
            .ifEmpty { "Untitled Exam" }
    }
    
    /**
     * Sanitize email addresses
     * Basic validation to prevent malformed inputs
     */
    fun sanitizeEmail(input: String): String {
        return input
            .trim()
            .lowercase()
            .take(254)  // RFC 5321 max email length
            .filter { it.isLetterOrDigit() || it in "@.-_+" }
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        return email.matches(emailRegex)
    }
    
    /**
     * Sanitize integer inputs with bounds
     */
    fun sanitizeInt(input: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int? {
        return input.toIntOrNull()?.coerceIn(min, max)
    }
    
    /**
     * Sanitize grade level input
     */
    fun sanitizeGradeLevel(input: String): String {
        return input
            .trim()
            .take(20)
            .replace(Regex("[^A-Za-z0-9\\s-]"), "")
            .ifEmpty { "Unknown" }
    }
    
    /**
     * Remove SQL injection patterns (additional safety layer)
     * Note: Room already parameterizes queries, but this adds defense-in-depth
     */
    fun removeSqlInjectionPatterns(input: String): String {
        return input
            .replace(Regex("(?i)(--|#|/\\*|\\*/|;|'|\"|\\\"|union|select|insert|update|delete|drop|create|alter|exec|execute)"), "")
    }
    
    /**
     * Sanitize phone numbers (keep digits only)
     */
    fun sanitizePhoneNumber(input: String): String {
        return input.filter { it.isDigit() || it in "+()-. " }
            .take(20)
    }
}
