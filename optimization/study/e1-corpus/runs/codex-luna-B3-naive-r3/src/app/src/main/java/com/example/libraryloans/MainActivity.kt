package com.example.libraryloans

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.content.Context
import java.util.Locale

class MainActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(247, 248, 246)
        window.navigationBarColor = Color.rgb(247, 248, 246)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(LibraryView(this))
    }
}

private data class Book(val title: String, val author: String, val due: String, val days: String, val color: Int, val kind: Int)

private class LibraryView(context: Context) : View(context) {
    private val bg = Color.rgb(247, 248, 246)
    private val ink = Color.rgb(27, 41, 53)
    private val muted = Color.rgb(106, 121, 120)
    private val green = Color.rgb(44, 110, 87)
    private val pale = Color.rgb(221, 233, 225)
    private val orange = Color.rgb(232, 155, 97)
    private val books = listOf(
        Book("The Overstory", "Richard Powers", "Due in 4 days", "4 days left", Color.rgb(223, 231, 215), 0),
        Book("Tomorrow, and Tomorrow, and Tomorrow", "Gabrielle Zevin", "Due in 12 days", "12 days left", Color.rgb(239, 212, 182), 1)
    )
    private val history = listOf(
        Book("The Midnight Library", "Matt Haig", "Returned 18 Aug 2024", "", Color.rgb(199, 217, 224), 2),
        Book("Pachinko", "Min Jin Lee", "Returned 03 Jul 2024", "", Color.rgb(229, 210, 205), 3),
        Book("Braiding Sweetgrass", "Robin Wall Kimmerer", "Returned 22 May 2024", "", Color.rgb(216, 225, 191), 4)
    )
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val type = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tab = 0
    private var detail: Book? = null
    private var renewed = false
    private var downX = 0f
    private var downY = 0f

    init { setBackgroundColor(bg); type.typeface = Typeface.create("sans", Typeface.NORMAL) }
    private fun d(v: Float) = v * resources.displayMetrics.density
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        type.textSize = d(size); type.color = color
        type.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s, d(x), d(y), type)
    }
    private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, radius: Float = 0f) {
        p.color = color; p.style = Paint.Style.FILL
        c.drawRoundRect(d(l), d(t), d(r), d(b), d(radius), d(radius), p)
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(bg)
        if (detail != null) { drawDetail(c, detail!!); return }
        drawHeader(c)
        if (tab == 0) drawLoans(c) else drawHistory(c)
        drawNav(c)
    }
    private fun drawHeader(c: Canvas) {
        text(c, "My library", 24f, 39f, 27f, ink, true)
        text(c, "Good morning, Alex", 24f, 64f, 14f, muted)
        // profile circle
        p.color = pale; c.drawCircle(d(342f), d(43f), d(20f), p)
        text(c, "A", 336f, 49f, 15f, green, true)
        text(c, "Your reading, all in one place", 24f, 100f, 14f, muted)
        box(c, 24f, 116f, 366f, 164f, Color.WHITE, 12f)
        text(c, "3", 42f, 143f, 25f, green, true)
        text(c, "active loans", 42f, 155f, 11f, muted)
        line(c, 130f, 128f, 130f, 153f, Color.rgb(226, 231, 228))
        text(c, "8", 163f, 143f, 25f, ink, true)
        text(c, "books read", 163f, 155f, 11f, muted)
        line(c, 260f, 128f, 260f, 153f, Color.rgb(226, 231, 228))
        text(c, "12", 287f, 143f, 25f, ink, true)
        text(c, "day streak", 287f, 155f, 11f, muted)
        tabs(c)
    }
    private fun tabs(c: Canvas) {
        text(c, "Current loans", 27f, 207f, 14f, if (tab == 0) green else muted, tab == 0)
        text(c, "Reading history", 190f, 207f, 14f, if (tab == 1) green else muted, tab == 1)
        box(c, if (tab == 0) 24f else 187f, 218f, if (tab == 0) 133f else 320f, 221f, green, 2f)
    }
    private fun drawLoans(c: Canvas) {
        text(c, "BORROWED NOW", 24f, 251f, 11f, muted, true)
        drawBookCard(c, books[0], 24f, 264f, true)
        drawBookCard(c, books[1], 24f, 390f, false)
        text(c, "Renewals are available 3 days before your due date.", 24f, 546f, 12f, muted)
    }
    private fun drawHistory(c: Canvas) {
        text(c, "RECENTLY READ", 24f, 251f, 11f, muted, true)
        drawHistoryRow(c, history[0], 24f, 268f)
        drawHistoryRow(c, history[1], 24f, 365f)
        drawHistoryRow(c, history[2], 24f, 462f)
    }
    private fun cover(c: Canvas, book: Book, x: Float, y: Float, w: Float = 66f, h: Float = 94f) {
        box(c, x, y, x + w, y + h, book.color, 5f)
        p.color = Color.argb(42, 27, 41, 53); p.strokeWidth = d(1f); p.style = Paint.Style.STROKE
        c.drawRoundRect(d(x), d(y), d(x+w), d(y+h), d(5f), d(5f), p)
        p.style = Paint.Style.FILL
        val label = when (book.kind) { 0 -> "THE\nOVERSTORY"; 1 -> "TOMORROW\n×3"; 2 -> "THE\nMIDNIGHT\nLIBRARY"; 3 -> "PACHINKO"; else -> "BRAIDING\nSWEETGRASS" }
        val parts = label.split("\n")
        parts.forEachIndexed { i, s -> text(c, s, x + 7f, y + 23f + i * 10f, 7f, ink, true) }
        line(c, x + 7f, y + h - 16f, x + w - 7f, y + h - 16f, ink)
    }
    private fun drawBookCard(c: Canvas, book: Book, x: Float, y: Float, first: Boolean) {
        box(c, x, y, 366f, y + 112f, Color.WHITE, 12f)
        cover(c, book, x + 13f, y + 9f, 65f, 94f)
        text(c, book.title, x + 94f, y + 30f, if (book.title.length > 24) 14f else 16f, ink, true)
        text(c, book.author, x + 94f, y + 51f, 13f, muted)
        text(c, if (first && renewed) "Due in 18 days" else book.due, x + 94f, y + 76f, 12f, if (first && !renewed) orange else green, true)
        if (first) {
            box(c, 275f, y + 83f, 352f, y + 103f, if (renewed) pale else green, 10f)
            text(c, if (renewed) "Renewed" else "Renew loan", 290f, y + 97f, 10f, if (renewed) green else Color.WHITE, true)
        } else text(c, "View details  ›", x + 94f, y + 98f, 12f, green, true)
    }
    private fun drawHistoryRow(c: Canvas, book: Book, x: Float, y: Float) {
        box(c, x, y, 366f, y + 82f, Color.WHITE, 12f)
        cover(c, book, x + 12f, y + 8f, 46f, 66f)
        text(c, book.title, x + 75f, y + 30f, 15f, ink, true)
        text(c, book.author, x + 75f, y + 49f, 12f, muted)
        text(c, book.due, x + 75f, y + 67f, 11f, muted)
        text(c, "›", 344f, y + 46f, 22f, muted)
    }
    private fun drawNav(c: Canvas) {
        val y = height / resources.displayMetrics.density - 64f
        line(c, 0f, y, 390f, y, Color.rgb(228, 233, 230))
        navIcon(c, 62f, y + 25f, "⌂", tab == 0)
        navIcon(c, 195f, y + 25f, "▤", false)
        navIcon(c, 328f, y + 25f, "♡", false)
        text(c, "Library", 48f, y + 49f, 10f, if (tab == 0) green else muted, tab == 0)
        text(c, "Discover", 178f, y + 49f, 10f, muted)
        text(c, "Saved", 314f, y + 49f, 10f, muted)
    }
    private fun navIcon(c: Canvas, x: Float, y: Float, s: String, selected: Boolean) { text(c, s, x - 9f, y, 24f, if (selected) green else muted, true) }
    private fun drawDetail(c: Canvas, book: Book) {
        text(c, "‹", 22f, 43f, 32f, ink)
        text(c, "Book details", 60f, 39f, 20f, ink, true)
        cover(c, book, 38f, 84f, 112f, 158f)
        text(c, book.title, 174f, 116f, 21f, ink, true)
        text(c, book.author, 174f, 143f, 14f, muted)
        text(c, "Fiction · 2023", 174f, 170f, 12f, muted)
        box(c, 174f, 190f, 274f, 218f, pale, 14f)
        text(c, "AVAILABLE", 190f, 207f, 10f, green, true)
        text(c, "About this book", 24f, 286f, 18f, ink, true)
        text(c, "A story about the books we carry, the lives", 24f, 314f, 13f, muted)
        text(c, "we might have lived, and finding your way", 24f, 334f, 13f, muted)
        text(c, "back to the present.", 24f, 354f, 13f, muted)
        box(c, 24f, 389f, 366f, 437f, green, 13f)
        text(c, "Borrow this book", 132f, 419f, 14f, Color.WHITE, true)
    }
    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) { p.color = color; p.strokeWidth = d(1f); c.drawLine(d(x1), d(y1), d(x2), d(y2), p) }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) { downX = e.x; downY = e.y; return true }
        if (e.action == MotionEvent.ACTION_UP) {
            val x = e.x / resources.displayMetrics.density; val y = e.y / resources.displayMetrics.density
            if (detail != null) { if (y < 70f) { detail = null; invalidate() }; return true }
            if (y in 175f..235f) { tab = if (x > 160f) 1 else 0; invalidate(); return true }
            if (tab == 0 && y in 264f..376f) {
                if (x > 260f && y > 330f) { renewed = true; invalidate() } else { detail = books[0]; invalidate() }
                return true
            }
            if (tab == 0 && y in 390f..510f) { detail = books[1]; invalidate(); return true }
            if (tab == 1 && y in 260f..560f) { val i = ((y - 268f) / 97f).toInt().coerceIn(0, 2); detail = history[i]; invalidate(); return true }
        }
        return true
    }
}
