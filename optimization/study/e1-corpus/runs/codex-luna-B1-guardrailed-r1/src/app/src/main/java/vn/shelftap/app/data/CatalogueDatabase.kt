package vn.shelftap.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.Context

class CatalogueDatabase(context: Context) : SQLiteOpenHelper(context, "catalogue.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE titles (
            id TEXT PRIMARY KEY NOT NULL, isbn TEXT NOT NULL, title TEXT NOT NULL,
            authors TEXT NOT NULL, year INTEGER NOT NULL, subjects TEXT NOT NULL,
            cover_url TEXT, copies_available INTEGER NOT NULL)""")
        db.execSQL("CREATE VIRTUAL TABLE titles_fts USING fts4(id, title, authors, isbn, subjects, content='titles')")
        db.execSQL("CREATE TRIGGER titles_ai AFTER INSERT ON titles BEGIN INSERT INTO titles_fts(rowid,id,title,authors,isbn,subjects) VALUES (new.rowid,new.id,new.title,new.authors,new.isbn,new.subjects); END")
        db.execSQL("CREATE TRIGGER titles_ad AFTER DELETE ON titles BEGIN INSERT INTO titles_fts(titles_fts,rowid,id,title,authors,isbn,subjects) VALUES ('delete',old.rowid,old.id,old.title,old.authors,old.isbn,old.subjects); END")
        db.execSQL("CREATE TRIGGER titles_au AFTER UPDATE ON titles BEGIN INSERT INTO titles_fts(titles_fts,rowid,id,title,authors,isbn,subjects) VALUES ('delete',old.rowid,old.id,old.title,old.authors,old.isbn,old.subjects); INSERT INTO titles_fts(rowid,id,title,authors,isbn,subjects) VALUES (new.rowid,new.id,new.title,new.authors,new.isbn,new.subjects); END")
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
                    put("subjects", item.subjects.joinToString("\u001f")); put("cover_url", item.coverUrl)
                    put("copies_available", item.copiesAvailable)
                }
                db.insertWithOnConflict("titles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun page(query: String, page: Int, size: Int): List<Title> {
        val db = readableDatabase
        val args = if (query.isBlank()) emptyArray() else arrayOf("${ftsQuery(query)}*")
        val sql = if (query.isBlank()) "SELECT id,isbn,title,authors,year,subjects,cover_url,copies_available FROM titles ORDER BY title COLLATE NOCASE LIMIT $size OFFSET ${page * size}"
        else "SELECT t.id,t.isbn,t.title,t.authors,t.year,t.subjects,t.cover_url,t.copies_available FROM titles t JOIN titles_fts f ON f.rowid=t.rowid WHERE titles_fts MATCH ? ORDER BY t.title COLLATE NOCASE LIMIT $size OFFSET ${page * size}"
        return db.rawQuery(sql, args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toTitle()) }
        }
    }

    fun byId(id: String): Title? = readableDatabase.query("titles", null, "id=?", arrayOf(id), null, null, null).use { c ->
        if (c.moveToFirst()) c.toTitle() else null
    }

    private fun android.database.Cursor.toTitle() = Title(
        getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("isbn")),
        getString(getColumnIndexOrThrow("title")), getString(getColumnIndexOrThrow("authors")).split("\u001f").filter(String::isNotBlank),
        getInt(getColumnIndexOrThrow("year")), getString(getColumnIndexOrThrow("subjects")).split("\u001f").filter(String::isNotBlank),
        getString(getColumnIndexOrThrow("cover_url")), getInt(getColumnIndexOrThrow("copies_available"))
    )

    private fun ftsQuery(value: String): String = value.trim().split(Regex("\\s+")).joinToString(" ") { token ->
        token.replace(Regex("[^\\p{L}\\p{N}_-]"), "")
    }.trim()
}
