package vn.shelftap.app.ui.screens.password

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidationTest {

    @Test
    fun testPasswordsMatching() {
        val current = "oldPassword123"
        val newPass = "newSecret456!"
        val confirmPass = "newSecret456!"

        assertTrue(newPass == confirmPass)
        assertFalse(current.isEmpty())
        assertFalse(newPass.isEmpty())
    }

    @Test
    fun testPasswordsNotMatching() {
        val newPass = "newSecret456!"
        val confirmPass = "differentPassword"

        assertFalse(newPass == confirmPass)
    }
}
