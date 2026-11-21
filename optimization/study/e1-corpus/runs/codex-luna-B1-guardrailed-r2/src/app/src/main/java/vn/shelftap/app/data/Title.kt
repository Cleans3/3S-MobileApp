package vn.shelftap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val authorSearch: String,
    val subjectSearch: String
)

data class TitleDto(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val year: Int,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int
)

fun TitleDto.toEntity() = TitleEntity(
    id, isbn, title, authors, year, subjects, coverUrl, copiesAvailable,
    authors.joinToString(" "), subjects.joinToString(" ")
)
