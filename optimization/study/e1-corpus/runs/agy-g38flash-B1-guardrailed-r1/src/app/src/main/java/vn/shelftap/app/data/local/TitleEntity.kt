package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.model.TitleDto

@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int
)

fun TitleDto.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable
)

fun TitleEntity.toModel(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable
)

fun TitleDto.toModel(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable
)
