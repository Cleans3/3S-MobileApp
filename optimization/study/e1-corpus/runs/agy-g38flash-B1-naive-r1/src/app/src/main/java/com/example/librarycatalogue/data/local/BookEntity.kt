package com.example.librarycatalogue.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["title"]),
        Index(value = ["author"]),
        Index(value = ["genre"]),
        Index(value = ["isbn"]),
        Index(value = ["publicationYear"])
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
    val pageCount: Int,
    val description: String,
    val shelfLocation: String,
    val isAvailable: Boolean,
    val coverColorSeed: Long,
    val coverImageUrl: String? = null
)
