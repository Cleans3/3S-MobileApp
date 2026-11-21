package vn.shelftap.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class CatalogueDb(context: Context) : SQLiteOpenHelper(context, "shelftap.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE titles (id TEXT PRIMARY KEY, isbn TEXT, title TEXT NOT NULL, authors TEXT, year INTEGER, subjects TEXT, coverUrl TEXT, copies INTEGER)")
        db.execSQL("CREATE INDEX title_search ON titles(title, isbn)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun upsert(items: List<Title>) { writableDatabase.beginTransaction(); try { items.forEach { t ->
        val v = ContentValues().apply { put("id", t.id); put("isbn", t.isbn); put("title", t.title); put("authors", t.authors.joinToString("\u001f")); put("year", t.year); put("subjects", t.subjects.joinToString("\u001f")); put("coverUrl", t.coverUrl); put("copies", t.copiesAvailable) }
        writableDatabase.insertWithOnConflict("titles", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }; writableDatabase.setTransactionSuccessful() } finally { writableDatabase.endTransaction() } }

    fun all(): List<Title> = query(null)
    fun search(text: String): List<Title> = query("title LIKE ? OR isbn LIKE ? OR authors LIKE ? OR subjects LIKE ?", Array(4) { "%$text%" })
    fun get(id: String): Title? = query("id = ?", arrayOf(id)).firstOrNull()
    private fun query(where: String?, args: Array<String> = emptyArray()): List<Title> {
        val result = mutableListOf<Title>(); val c = readableDatabase.query("titles", null, where, args, null, null, "title COLLATE NOCASE ASC")
        c.use { while (it.moveToNext()) result += Title(it.s("id"), it.s("isbn"), it.s("title"), split(it.s("authors")), it.getInt(it.getColumnIndexOrThrow("year")), split(it.s("subjects")), it.s("coverUrl").takeIf { value -> value.isNotBlank() }, it.getInt(it.getColumnIndexOrThrow("copies"))) }; return result
    }
    private fun split(value: String) = if (value.isBlank()) emptyList() else value.split("\u001f")
    private fun android.database.Cursor.s(name: String) = getString(getColumnIndexOrThrow(name)) ?: ""
}
