package net.maiatoday.tagspotter.core.sync

/**
 * Maps raw authentication error codes or messages to human-readable user-friendly messages.
 * Handles both the JVM REST API error messages and the Web (Wasm) JS SDK error codes.
 */
fun mapAuthError(rawMessage: String, defaultMessage: String): String {
    val upper = rawMessage.uppercase()
    return when {
        // Common / JVM REST error messages / codes
        upper.contains("EMAIL_NOT_FOUND") -> "No account found with this email address."
        upper.contains("INVALID_PASSWORD") -> "Incorrect password. Please try again."
        upper.contains("INVALID_LOGIN_CREDENTIALS") -> "Incorrect email or password."
        upper.contains("USER_DISABLED") -> "This user account has been disabled."
        upper.contains("INVALID_EMAIL") -> "Please enter a valid email address."
        upper.contains("EMAIL_EXISTS") -> "An account with this email address already exists."
        upper.contains("WEAK_PASSWORD") -> "The password is too weak. It must be at least 6 characters."
        upper.contains("MISSING_PASSWORD") -> "Please enter your password."
        upper.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Too many failed attempts. Please try again later."
        upper.contains("USER_NOT_FOUND") -> "No account found with this email address."
        
        // Web / WASM error codes or messages
        upper.contains("AUTH/INVALID-CREDENTIAL") -> "Incorrect email or password."
        upper.contains("AUTH/USER-NOT-FOUND") -> "No account found with this email address."
        upper.contains("AUTH/WRONG-PASSWORD") -> "Incorrect password. Please try again."
        upper.contains("AUTH/EMAIL-ALREADY-IN-USE") -> "An account with this email address already exists."
        upper.contains("AUTH/INVALID-EMAIL") -> "Please enter a valid email address."
        upper.contains("AUTH/WEAK-PASSWORD") -> "The password is too weak. It must be at least 6 characters."
        upper.contains("AUTH/USER-DISABLED") -> "This user account has been disabled."
        upper.contains("AUTH/TOO-MANY-REQUESTS") -> "Too many failed attempts. Please try again later."
        upper.contains("AUTH/NETWORK-REQUEST-FAILED") -> "Network error. Please check your connection."
        upper.contains("AUTH/OPERATION-NOT-ALLOWED") -> "This sign-in method is not enabled."
        
        else -> {
            if (rawMessage.isNotBlank() && !rawMessage.startsWith("{") && !rawMessage.startsWith("[")) {
                rawMessage.replace("Firebase:", "").trim()
            } else {
                defaultMessage
            }
        }
    }
}
