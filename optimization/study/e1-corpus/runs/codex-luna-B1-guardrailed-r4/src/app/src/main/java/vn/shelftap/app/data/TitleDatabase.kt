package vn.shelftap.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Fts4
import kotlinx.coroutines.flow.Flow

class ListConverters {
    @TypeConverter fun fromList(value: List<String>): String = value.joinToString("\u001f")
    @TypeConverter fun toList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split("\u001f")
}

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
    val authorsText: String,
    val subjectsText: String
) {
    fun asDomain() = Title(id, isbn, title, authors, year, subjects, coverUrl, copiesAvailable)
    companion object { fun from(value: Title) = TitleEntity(value.id, value.isbn, value.title, value.authors, value.year, value.subjects, value.coverUrl, value.copiesAvailable, value.authors.joinToString(" "), value.subjects.joinToString(" ")) }
}

@Fts4(contentEntity = TitleEntity::class)
@Entity(tableName = "title_search")
data class TitleSearchEntity(val title: String, val isbn: String, val authorsText: String, val subjectsText: String)

@Dao
interface TitleDao {
    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<TitleEntity>>

    @Query("SELECT t.* FROM titles t JOIN title_search s ON t.rowid = s.rowid WHERE title_search MATCH :match ORDER BY t.title COLLATE NOCASE")
    fun observeSearch(match: String): Flow<List<TitleEntity>>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun find(id: String): TitleEntity?

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(values: List<TitleEntity>)
}

@Database(entities = [TitleEntity::class, TitleSearchEntity::class], version = 1, exportSchema = false)
@TypeConverters(ListConverters::class)
abstract class TitleDatabase : RoomDatabase() {
    abstract fun titles(): TitleDao
    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, TitleDatabase::class.java, "shelftap.db").build()
    }
}
