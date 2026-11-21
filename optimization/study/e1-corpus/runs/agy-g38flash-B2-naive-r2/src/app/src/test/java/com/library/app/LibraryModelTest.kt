package com.library.app

import com.library.app.data.local.SessionManager
import com.library.app.data.model.MemberProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryModelTest {

    @Test
    fun memberProfile_fullName_formatsCorrectly() {
        val profile = MemberProfile(
            id = "test-1",
            cardNumber = "LIB-1234",
            firstName = "Ada",
            lastName = "Lovelace",
            email = "ada@library.org"
        )
        assertEquals("Ada Lovelace", profile.fullName)
    }

    @Test
    fun memberProfile_defaults_areValid() {
        val profile = MemberProfile(
            id = "test-2",
            cardNumber = "LIB-5678",
            firstName = "Grace",
            lastName = "Hopper",
            email = "grace@library.org"
        )
        assertEquals("ACTIVE", profile.status)
        assertEquals(0, profile.borrowedBooksCount)
        assertEquals(0, profile.overdueBooksCount)
        assertEquals(0.0, profile.finesDue, 0.001)
    }
}
