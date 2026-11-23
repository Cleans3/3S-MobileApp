package shelftap.optlab.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** SQLite + FTS4 catalog for the tuned search variant (B of P13). The platform SQLite (API 34, 3.39.2) ships FTS3/4 but not FTS5. */
class BookDb private constructor(private val db: SQLiteDatabase) {

    /** Token-prefix AND match in docid order (FTS4 has no built-in rank); with titleOrder the matches are sorted by title (COLLATE NOCASE, id) before LIMIT.
     *  Blank or token-less input returns nothing. */
    fun search(query: String, limit: Int = 50, titleOrder: Boolean = false): List<Book> {  // limit <= 0: no limit
        val match = Regex("[\\p{L}\\p{N}]+").findAll(query).joinToString(" ") { "${it.value}*" }
        if (match.isEmpty()) return emptyList()
        db.rawQuery(
            "SELECT b.id, b.title, b.author, b.isbn, b.year, b.coverIdx FROM book_fts f JOIN book b ON b.id = f.docid " +
                "WHERE book_fts MATCH ?" + (if (titleOrder) " ORDER BY b.title COLLATE NOCASE, b.id" else "") + " LIMIT ?",
            arrayOf(match, if (limit > 0) limit.toString() else "-1"),
        ).use { c ->
            val out = ArrayList<Book>()
            while (c.moveToNext()) out += Book(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getInt(5))
            return out
        }
    }

    /** Runs a SELECT * FROM titles / book style query (id, title, author, isbn, year, coverIdx in the first six columns). */
    fun rows(sql: String, vararg args: String): List<Book> =
        db.rawQuery(sql, args).use { c ->
            val out = ArrayList<Book>()
            while (c.moveToNext()) out += Book(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getInt(5))
            out
        }

    companion object {
        fun file(ctx: Context): File = ctx.getDatabasePath("books.db")

        /**
         * The table the generated app of P13 queries (its Room entity "titles": searchText = lowercased title, isbn, authors; index on title),
         * derived from the seeded book table on first use. Opens read-write because of that one-time step.
         */
        fun openWithTitles(ctx: Context): BookDb {
            val db = SQLiteDatabase.openDatabase(file(ctx).path, null, SQLiteDatabase.OPEN_READWRITE)
            db.execSQL("CREATE TABLE IF NOT EXISTS titles(id INTEGER PRIMARY KEY, title TEXT NOT NULL, author TEXT, isbn TEXT, year INTEGER, coverIdx INTEGER, searchText TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_titles_title ON titles(title)")
            db.execSQL(
                "INSERT INTO titles SELECT id, title, author, isbn, year, coverIdx, lower(title || char(10) || isbn || char(10) || author) " +
                    "FROM book WHERE (SELECT COUNT(*) FROM titles) = 0",
            )
            return BookDb(db)
        }

        fun open(ctx: Context) = BookDb(SQLiteDatabase.openDatabase(file(ctx).path, null, SQLiteDatabase.OPEN_READONLY))

        /** Creates and fills the DB. Returns "rows_ms=.. fts_ms=.. db_bytes=.. fts_bytes=..". */
        fun seed(ctx: Context): String {
            val f = file(ctx)
            f.parentFile!!.mkdirs()
            f.delete()
            val db = SQLiteDatabase.openOrCreateDatabase(f, null)
            try {
                db.execSQL("CREATE TABLE book(id INTEGER PRIMARY KEY, title TEXT, author TEXT, isbn TEXT, year INTEGER, coverIdx INTEGER)")
                val t0 = System.nanoTime()
                db.beginTransaction()
                try {
                    val st = db.compileStatement("INSERT INTO book VALUES (?,?,?,?,?,?)")
                    for (i in 0 until BookGenerator.TOTAL) {
                        val b = BookGenerator.book(i)
                        st.bindLong(1, b.id.toLong()); st.bindString(2, b.title); st.bindString(3, b.author)
                        st.bindString(4, b.isbn); st.bindLong(5, b.year.toLong()); st.bindLong(6, b.coverIdx.toLong())
                        st.executeInsert()
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                val rowsMs = (System.nanoTime() - t0) / 1_000_000
                val bytesBefore = f.length()
                val t1 = System.nanoTime()
                db.execSQL(
                    "CREATE VIRTUAL TABLE book_fts USING fts4(content=\"book\", title, author, tokenize=unicode61, prefix=\"2,3\")",
                )
                db.execSQL("INSERT INTO book_fts(book_fts) VALUES('rebuild')")
                val ftsMs = (System.nanoTime() - t1) / 1_000_000
                return "rows_ms=$rowsMs fts_ms=$ftsMs db_bytes=${f.length()} fts_bytes=${f.length() - bytesBefore}"
            } finally {
                db.close()
            }
        }
    }
}
