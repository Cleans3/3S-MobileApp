package com.library.catalogue.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.library.catalogue.data.model.Book

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["title"]),
        Index(value = ["author"]),
        Index(value = ["genre"]),
        Index(value = ["isbn"]),
        Index(value = ["isAvailable"]),
        Index(value = ["genre", "isAvailable"])
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val publicationYear: Int,
    val description: String,
    val coverUrl: String,
    val coverColorHex: String,
    val totalCopies: Int,
    val availableCopies: Int,
    val isAvailable: Boolean = availableCopies > 0,
    val rating: Float,
    val pages: Int
) {
    fun toDomainModel(): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            isbn = isbn,
            genre = genre,
            publicationYear = publicationYear,
            description = description,
            coverUrl = coverUrl,
            coverColorHex = coverColorHex,
            totalCopies = totalCopies,
            availableCopies = availableCopies,
            rating = rating,
            pages = pages
        )
    }

    companion object {
        fun fromDomainModel(book: Book): BookEntity {
            return BookEntity(
                id = book.id,
                title = book.title,
                author = book.author,
                isbn = book.isbn,
                genre = book.genre,
                publicationYear = book.publicationYear,
                description = book.description,
                coverUrl = book.coverUrl,
                coverColorHex = book.coverColorHex,
                totalCopies = book.totalCopies,
                availableCopies = book.availableCopies,
                isAvailable = book.isAvailable,
                rating = book.rating,
                pages = book.pages
            )
        }
    }
}
