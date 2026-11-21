package vn.shelftap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class Title(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int
)

@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int
) {
    fun asTitle() = Title(id, isbn, title, authors, year, subjects, coverUrl, copiesAvailable)
    companion object { fun from(t: Title) = TitleEntity(t.id, t.isbn, t.title, t.authors, t.year, t.subjects, t.coverUrl, t.copiesAvailable) }
}

data class TitlesResponse(
    val items: List<Title>,
    val page: Int,
    val size: Int,
    val total: Int
)
