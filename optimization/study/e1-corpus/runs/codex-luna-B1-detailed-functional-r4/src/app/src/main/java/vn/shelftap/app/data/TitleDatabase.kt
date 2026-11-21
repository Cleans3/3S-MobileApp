package vn.shelftap.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import vn.shelftap.app.model.Title

class TitleDatabase(context: Context) : SQLiteOpenHelper(context, "shelftap.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE titles (id TEXT PRIMARY KEY, isbn TEXT, title TEXT, authors TEXT, year INTEGER, subjects TEXT, coverUrl TEXT, copiesAvailable INTEGER)")
        db.execSQL("CREATE INDEX title_search ON titles(title, isbn)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun upsert(items: List<Title>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                val values = ContentValues().apply {
                    put("id", item.id); put("isbn", item.isbn); put("title", item.title)
                    put("authors", item.authors.joinToString("\u001f")); put("year", item.year)
                    put("subjects", item.subjects.joinToString("\u001f")); put("coverUrl", item.coverUrl)
                    put("copiesAvailable", item.copiesAvailable)
                }
                db.insertWithOnConflict("titles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun find(query: String): List<Title> {
        val db = readableDatabase
        val needle = "%${query.trim()}%"
        val cursor = if (query.isBlank()) db.query("titles", null, null, null, null, null, "title COLLATE NOCASE")
        else db.query("titles", null, "title LIKE ? OR isbn LIKE ? OR authors LIKE ? OR subjects LIKE ?", arrayOf(needle, needle, needle, needle), null, null, "title COLLATE NOCASE")
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.toTitle()) } }
    }

    fun get(id: String): Title? = readableDatabase.query("titles", null, "id = ?", arrayOf(id), null, null, null).use { c -> if (c.moveToFirst()) c.toTitle() else null }

    private fun android.database.Cursor.toTitle() = Title(
        getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("isbn")), getString(getColumnIndexOrThrow("title")),
        getString(getColumnIndexOrThrow("authors")).split("\u001f").filter(String::isNotBlank), getInt(getColumnIndexOrThrow("year")),
        getString(getColumnIndexOrThrow("subjects")).split("\u001f").filter(String::isNotBlank), getString(getColumnIndexOrThrow("coverUrl")), getInt(getColumnIndexOrThrow("copiesAvailable"))
    )
}
