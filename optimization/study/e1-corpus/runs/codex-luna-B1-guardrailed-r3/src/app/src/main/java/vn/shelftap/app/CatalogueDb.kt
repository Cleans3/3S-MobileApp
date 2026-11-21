package vn.shelftap.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CatalogueDb(context: Context) : SQLiteOpenHelper(context, "catalogue.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE titles (id TEXT PRIMARY KEY, isbn TEXT, title TEXT NOT NULL, authors TEXT, year INTEGER, subjects TEXT, cover_url TEXT, copies INTEGER)")
        db.execSQL("CREATE VIRTUAL TABLE title_search USING fts4(id, isbn, title, authors, subjects, content='titles')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun replace(titles: List<Title>) { val db = writableDatabase; db.beginTransaction(); try { titles.forEach { title ->
        val values = ContentValues().apply { put("id", title.id); put("isbn", title.isbn); put("title", title.title); put("authors", title.authors.joinToString(SEP)); put("year", title.year); put("subjects", title.subjects.joinToString(SEP)); put("cover_url", title.coverUrl); put("copies", title.copiesAvailable) }
        insertWithOnConflict("titles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }; rebuildIndex(db); db.setTransactionSuccessful() } finally { db.endTransaction() } }

    fun find(query: String): List<Title> = readableDatabase.rawQuery(
        "SELECT t.id,t.isbn,t.title,t.authors,t.year,t.subjects,t.cover_url,t.copies FROM titles t JOIN title_search s ON s.id=t.id WHERE title_search MATCH ? ORDER BY t.title COLLATE NOCASE", arrayOf(ftsQuery(query))
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }
    fun all(): List<Title> = readableDatabase.rawQuery("SELECT id,isbn,title,authors,year,subjects,cover_url,copies FROM titles ORDER BY title COLLATE NOCASE", null).use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }

    private fun rebuildIndex(db: SQLiteDatabase) { db.execSQL("DELETE FROM title_search"); db.execSQL("INSERT INTO title_search(rowid,id,isbn,title,authors,subjects) SELECT rowid,id,isbn,title,authors,subjects FROM titles") }
    private fun read(c: android.database.Cursor) = Title(c.getString(0), c.getString(1), c.getString(2), split(c.getString(3)), c.getInt(4), split(c.getString(5)), c.getString(6), c.getInt(7))
    private fun split(value: String?) = value?.split(SEP)?.filter(String::isNotBlank) ?: emptyList()
    private fun ftsQuery(value: String) = value.trim().split(Regex("\\s+")).filter(String::isNotBlank).joinToString(" ") { "\"${it.replace("\"", "")}" + "*\"" }
    private companion object { const val SEP = "\u001F" }
}
