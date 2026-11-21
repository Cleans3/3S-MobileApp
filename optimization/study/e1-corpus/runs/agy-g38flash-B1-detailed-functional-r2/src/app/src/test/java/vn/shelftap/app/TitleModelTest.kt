package vn.shelftap.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.model.Title

class TitleModelTest {

    @Test
    fun testTitleAvailability() {
        val titleAvailable = Title(
            id = "t1",
            isbn = "978-0141439518",
            title = "Pride and Prejudice",
            authors = listOf("Jane Austen"),
            year = 1813,
            subjects = listOf("Classic", "Romance"),
            coverUrl = "https://api.shelftap.example/covers/t1.jpg",
            copiesAvailable = 3
        )
        assertTrue(titleAvailable.isAvailable)
        assertEquals("Jane Austen", titleAvailable.authorsFormatted)

        val titleUnavailable = titleAvailable.copy(copiesAvailable = 0)
        assertFalse(titleUnavailable.isAvailable)
    }

    @Test
    fun testTitleEntityMapping() {
        val title = Title(
            id = "t2",
            isbn = "978-0451524935",
            title = "1984",
            authors = listOf("George Orwell"),
            year = 1949,
            subjects = listOf("Dystopia", "Political Fiction"),
            coverUrl = null,
            copiesAvailable = 1
        )

        val entity = TitleEntity.fromDomain(title)
        assertEquals("t2", entity.id)
        assertEquals("george orwell", entity.authorsSearchString)
        assertEquals("dystopia political fiction", entity.subjectsSearchString)

        val restored = entity.toDomainModel()
        assertEquals(title, restored)
    }
}
