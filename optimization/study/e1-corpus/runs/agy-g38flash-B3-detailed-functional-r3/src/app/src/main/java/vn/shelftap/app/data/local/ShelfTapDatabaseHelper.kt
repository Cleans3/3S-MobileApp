package vn.shelftap.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.TitleDetail

class ShelfTapDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    private val gson = Gson()

    companion object {
        private const val DATABASE_NAME = "shelftap.db"
        private const val DATABASE_VERSION = 1

        // Table Loans
        private const val TABLE_LOANS = "loans"
        private const val COL_LOAN_ID = "loan_id"
        private const val COL_COPY_ID = "copy_id"
        private const val COL_TITLE_ID = "title_id"
        private const val COL_TITLE = "title"
        private const val COL_BARCODE = "barcode"
        private const val COL_OUT_AT = "out_at"
        private const val COL_DUE_AT = "due_at"
        private const val COL_RETURNED_AT = "returned_at"
        private const val COL_RENEWALS = "renewals"
        private const val COL_RENEWABLE = "renewable"

        // Table History
        private const val TABLE_HISTORY = "history"
        private const val COL_HIST_ID = "id"

        // Table MemberProfile
        private const val TABLE_PROFILE = "member_profile"
        private const val COL_MEMBER_ID = "member_id"
        private const val COL_FULL_NAME = "full_name"
        private const val COL_EMAIL = "email"
        private const val COL_MEMBER_TYPE = "member_type"
        private const val COL_STATUS = "status"
        private const val COL_HISTORY_ENABLED = "history_enabled"
        private const val COL_MUST_CHANGE_PASSWORD = "must_change_password"

        // Table TitleDetail
        private const val TABLE_TITLES = "title_details"
        private const val COL_ISBN = "isbn"
        private const val COL_AUTHORS_JSON = "authors_json"
        private const val COL_SUBJECTS_JSON = "subjects_json"
        private const val COL_COVER_URL = "cover_url"
        private const val COL_COPIES_AVAILABLE = "copies_available"
        private const val COL_COPIES_TOTAL = "copies_total"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_LOANS (
                $COL_LOAN_ID TEXT PRIMARY KEY,
                $COL_COPY_ID TEXT,
                $COL_TITLE_ID TEXT,
                $COL_TITLE TEXT,
                $COL_BARCODE TEXT,
                $COL_OUT_AT TEXT,
                $COL_DUE_AT TEXT,
                $COL_RETURNED_AT TEXT,
                $COL_RENEWALS INTEGER,
                $COL_RENEWABLE INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE_ID TEXT,
                $COL_TITLE TEXT,
                $COL_OUT_AT TEXT,
                $COL_RETURNED_AT TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PROFILE (
                $COL_MEMBER_ID TEXT PRIMARY KEY,
                $COL_FULL_NAME TEXT,
                $COL_EMAIL TEXT,
                $COL_MEMBER_TYPE TEXT,
                $COL_STATUS TEXT,
                $COL_HISTORY_ENABLED INTEGER,
                $COL_MUST_CHANGE_PASSWORD INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TITLES (
                $COL_TITLE_ID TEXT PRIMARY KEY,
                $COL_ISBN TEXT,
                $COL_TITLE TEXT,
                $COL_AUTHORS_JSON TEXT,
                $COL_SUBJECTS_JSON TEXT,
                $COL_COVER_URL TEXT,
                $COL_COPIES_AVAILABLE INTEGER,
                $COL_COPIES_TOTAL INTEGER
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOANS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROFILE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TITLES")
        onCreate(db)
    }

    // --- Loans Caching ---

    @Synchronized
    fun saveLoans(loans: List<Loan>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_LOANS, null, null)
            for (loan in loans) {
                val cv = ContentValues().apply {
                    put(COL_LOAN_ID, loan.loanId)
                    put(COL_COPY_ID, loan.copyId)
                    put(COL_TITLE_ID, loan.titleId)
                    put(COL_TITLE, loan.title)
                    put(COL_BARCODE, loan.barcode)
                    put(COL_OUT_AT, loan.outAt)
                    put(COL_DUE_AT, loan.dueAt)
                    put(COL_RETURNED_AT, loan.returnedAt)
                    put(COL_RENEWALS, loan.renewals)
                    put(COL_RENEWABLE, if (loan.renewable) 1 else 0)
                }
                db.insertWithOnConflict(TABLE_LOANS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun updateLoan(loan: Loan) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_LOAN_ID, loan.loanId)
            put(COL_COPY_ID, loan.copyId)
            put(COL_TITLE_ID, loan.titleId)
            put(COL_TITLE, loan.title)
            put(COL_BARCODE, loan.barcode)
            put(COL_OUT_AT, loan.outAt)
            put(COL_DUE_AT, loan.dueAt)
            put(COL_RETURNED_AT, loan.returnedAt)
            put(COL_RENEWALS, loan.renewals)
            put(COL_RENEWABLE, if (loan.renewable) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_LOANS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun getLoans(): List<Loan> {
        val loans = mutableListOf<Loan>()
        val db = readableDatabase
        val cursor = db.query(TABLE_LOANS, null, null, null, null, null, "$COL_DUE_AT ASC")
        cursor.use {
            while (it.moveToNext()) {
                loans.add(
                    Loan(
                        loanId = it.getString(it.getColumnIndexOrThrow(COL_LOAN_ID)),
                        copyId = it.getString(it.getColumnIndexOrThrow(COL_COPY_ID)),
                        titleId = it.getString(it.getColumnIndexOrThrow(COL_TITLE_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        barcode = it.getString(it.getColumnIndexOrThrow(COL_BARCODE)),
                        outAt = it.getString(it.getColumnIndexOrThrow(COL_OUT_AT)),
                        dueAt = it.getString(it.getColumnIndexOrThrow(COL_DUE_AT)),
                        returnedAt = it.getString(it.getColumnIndexOrThrow(COL_RETURNED_AT)),
                        renewals = it.getInt(it.getColumnIndexOrThrow(COL_RENEWALS)),
                        renewable = it.getInt(it.getColumnIndexOrThrow(COL_RENEWABLE)) == 1
                    )
                )
            }
        }
        return loans
    }

    // --- History Caching ---

    @Synchronized
    fun saveHistory(items: List<HistoryItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_HISTORY, null, null)
            for (item in items) {
                val cv = ContentValues().apply {
                    put(COL_TITLE_ID, item.titleId)
                    put(COL_TITLE, item.title)
                    put(COL_OUT_AT, item.outAt)
                    put(COL_RETURNED_AT, item.returnedAt)
                }
                db.insert(TABLE_HISTORY, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun getHistory(): List<HistoryItem> {
        val history = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "$COL_HIST_ID ASC")
        cursor.use {
            while (it.moveToNext()) {
                history.add(
                    HistoryItem(
                        titleId = it.getString(it.getColumnIndexOrThrow(COL_TITLE_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        outAt = it.getString(it.getColumnIndexOrThrow(COL_OUT_AT)),
                        returnedAt = it.getString(it.getColumnIndexOrThrow(COL_RETURNED_AT))
                    )
                )
            }
        }
        return history
    }

    // --- Profile Caching ---

    @Synchronized
    fun saveProfile(profile: MemberProfile) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_MEMBER_ID, profile.memberId)
            put(COL_FULL_NAME, profile.fullName)
            put(COL_EMAIL, profile.email)
            put(COL_MEMBER_TYPE, profile.memberType)
            put(COL_STATUS, profile.status)
            put(COL_HISTORY_ENABLED, if (profile.historyEnabled) 1 else 0)
            put(COL_MUST_CHANGE_PASSWORD, if (profile.mustChangePassword) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_PROFILE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun getProfile(): MemberProfile? {
        val db = readableDatabase
        val cursor = db.query(TABLE_PROFILE, null, null, null, null, null, null, "1")
        cursor.use {
            if (it.moveToFirst()) {
                return MemberProfile(
                    memberId = it.getString(it.getColumnIndexOrThrow(COL_MEMBER_ID)),
                    fullName = it.getString(it.getColumnIndexOrThrow(COL_FULL_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(COL_EMAIL)),
                    memberType = it.getString(it.getColumnIndexOrThrow(COL_MEMBER_TYPE)),
                    status = it.getString(it.getColumnIndexOrThrow(COL_STATUS)),
                    historyEnabled = it.getInt(it.getColumnIndexOrThrow(COL_HISTORY_ENABLED)) == 1,
                    mustChangePassword = it.getInt(it.getColumnIndexOrThrow(COL_MUST_CHANGE_PASSWORD)) == 1
                )
            }
        }
        return null
    }

    @Synchronized
    fun updateHistoryEnabled(enabled: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_HISTORY_ENABLED, if (enabled) 1 else 0)
        }
        db.update(TABLE_PROFILE, cv, null, null)
    }

    // --- Title Details Caching ---

    @Synchronized
    fun saveTitleDetail(detail: TitleDetail) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_TITLE_ID, detail.titleId)
            put(COL_ISBN, detail.isbn)
            put(COL_TITLE, detail.title)
            put(COL_AUTHORS_JSON, gson.toJson(detail.authors))
            put(COL_SUBJECTS_JSON, gson.toJson(detail.subjects))
            put(COL_COVER_URL, detail.coverUrl)
            put(COL_COPIES_AVAILABLE, detail.copiesAvailable)
            put(COL_COPIES_TOTAL, detail.copiesTotal)
        }
        db.insertWithOnConflict(TABLE_TITLES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun getTitleDetail(titleId: String): TitleDetail? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TITLES, null, "$COL_TITLE_ID = ?", arrayOf(titleId), null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val authorsJson = it.getString(it.getColumnIndexOrThrow(COL_AUTHORS_JSON))
                val subjectsJson = it.getString(it.getColumnIndexOrThrow(COL_SUBJECTS_JSON))
                val stringListType = object : TypeToken<List<String>>() {}.type
                val authors: List<String> = try {
                    gson.fromJson(authorsJson, stringListType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                val subjects: List<String> = try {
                    gson.fromJson(subjectsJson, stringListType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                return TitleDetail(
                    titleId = it.getString(it.getColumnIndexOrThrow(COL_TITLE_ID)),
                    isbn = it.getString(it.getColumnIndexOrThrow(COL_ISBN)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                    authors = authors,
                    subjects = subjects,
                    coverUrl = it.getString(it.getColumnIndexOrThrow(COL_COVER_URL)),
                    copiesAvailable = it.getInt(it.getColumnIndexOrThrow(COL_COPIES_AVAILABLE)),
                    copiesTotal = it.getInt(it.getColumnIndexOrThrow(COL_COPIES_TOTAL))
                )
            }
        }
        return null
    }
}
