package net.maiatoday.tagspotter.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthErrorMapperTest {

    @Test
    fun testMapAuthErrorJvmCodes() {
        assertEquals(
            "No account found with this email address.",
            mapAuthError("EMAIL_NOT_FOUND", "Default error")
        )
        assertEquals(
            "Incorrect password. Please try again.",
            mapAuthError("INVALID_PASSWORD", "Default error")
        )
        assertEquals(
            "Incorrect email or password.",
            mapAuthError("INVALID_LOGIN_CREDENTIALS", "Default error")
        )
        assertEquals(
            "An account with this email address already exists.",
            mapAuthError("EMAIL_EXISTS", "Default error")
        )
        assertEquals(
            "The password is too weak. It must be at least 6 characters.",
            mapAuthError("WEAK_PASSWORD : Password should be at least 6 characters", "Default error")
        )
    }

    @Test
    fun testMapAuthErrorWasmCodes() {
        assertEquals(
            "Incorrect email or password.",
            mapAuthError("auth/invalid-credential", "Default error")
        )
        assertEquals(
            "No account found with this email address.",
            mapAuthError("auth/user-not-found", "Default error")
        )
        assertEquals(
            "An account with this email address already exists.",
            mapAuthError("auth/email-already-in-use", "Default error")
        )
        assertEquals(
            "Too many failed attempts. Please try again later.",
            mapAuthError("Firebase: Error (auth/too-many-requests).", "Default error")
        )
    }

    @Test
    fun testMapAuthErrorFallback() {
        assertEquals(
            "Some strange message",
            mapAuthError("Firebase: Some strange message", "Default error")
        )
        assertEquals(
            "Default error",
            mapAuthError("{\"error\": \"raw json\"}", "Default error")
        )
    }
}
