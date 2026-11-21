package com.example.libraryloans

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.roundToInt

data class Book(val title: String, val author: String, val due: String, val color: Int, val renewed: Boolean = false)

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 248, 245)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(LibraryView(this))
    }
}

private class LibraryView(context: android.content.Context) : View(context) {
    private val teal = Color.rgb(21, 107, 97)
    private val ink = Color.rgb(22, 37, 34)
    private val muted = Color.rgb(110, 122, 118)
    private val paper = Color.rgb(247, 248, 245)
    private val line = Color.rgb(228, 233, 230)
    private val books = mutableListOf(
        Book("The Night Circus", "Erin Morgenstern", "Due in 5 days", Color.rgb(57, 73, 108)),
        Book("Braiding Sweetgrass", "Robin Wall Kimmerer", "Due in 12 days", Color.rgb(49, 111, 91)),
        Book("The Vanishing Half", "Brit Bennett", "Due in 18 days", Color.rgb(151, 83, 69))
    )
    private var tab = 0
    private var detail: Book? = null
    private var toast: String? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val type = Typeface.create("sans", Typeface.NORMAL)
    private val bold = Typeface.create("sans", Typeface.BOLD)

    init { setBackgroundColor(paper); isClickable = true }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, isBold: Boolean = false) {
        paint.typeface = if (isBold) bold else type; paint.textSize = dp(size); paint.color = color; paint.style = Paint.Style.FILL
        canvas.drawText(value, dp(x), dp(y), paint)
    }
    private fun round(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        canvas.drawRoundRect(dp(l), dp(t), dp(r), dp(b), dp(radius), dp(radius), paint)
    }
    private fun line(canvas: Canvas, x1: Float, y: Float, x2: Float) {
        paint.color = line; paint.strokeWidth = dp(1f); canvas.drawLine(dp(x1), dp(y), dp(x2), dp(y), paint)
    }
    private fun bookCover(canvas: Canvas, book: Book, x: Float, y: Float, w: Float = 70f, h: Float = 98f) {
        round(canvas, x, y, x + w, y + h, 4f, book.color)
        paint.color = Color.argb(45, 255, 255, 255); canvas.drawRect(dp(x + 7), dp(y + 7), dp(x + w - 7), dp(y + 10), paint)
        text(canvas, book.title.uppercase(), x + 8, y + 33, 7f, Color.WHITE, true)
        text(canvas, book.author.split(" ").last().uppercase(), x + 8, y + h - 12, 6f, Color.WHITE)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width / resources.displayMetrics.density
        if (detail != null) drawDetail(c, detail!!, w) else drawHome(c, w)
        toast?.let { msg -> round(c, 22f, 610f, w - 22f, 660f, 14f, ink); text(c, msg, 38f, 641f, 13f, Color.WHITE, true) }
    }

    private fun drawHome(c: Canvas, w: Float) {
        text(c, "LIBRARY", 24f, 40f, 12f, teal, true)
        text(c, "Your reading space", 24f, 76f, 29f, ink, true)
        text(c, "Good morning, Maya", 24f, 101f, 14f, muted)
        round(c, w - 66f, 30f, w - 24f, 72f, 21f, Color.rgb(221, 237, 232))
        text(c, "M", w - 52f, 58f, 15f, teal, true)
        text(c, "▾", w - 43f, 61f, 10f, teal)
        text(c, "MY LOANS", 24f, 142f, 12f, teal, true)
        text(c, "${books.size} books", w - 88f, 142f, 12f, muted)
        var y = 158f
        books.forEachIndexed { index, book ->
            loanRow(c, book, y, w, index)
            y += 126f
        }
        line(c, 24f, 540f, w - 24f)
        text(c, "READING HISTORY", 24f, 570f, 12f, teal, true)
        text(c, "See all", w - 72f, 570f, 12f, teal, true)
        text(c, "Your finished books will appear here", 24f, 603f, 14f, muted)
        nav(c, w)
    }

    private fun loanRow(c: Canvas, book: Book, y: Float, w: Float, index: Int) {
        bookCover(c, book, 24f, y, 56f, 82f)
        text(c, book.title, 96f, y + 22f, 16f, ink, true)
        text(c, book.author, 96f, y + 44f, 12f, muted)
        text(c, book.due, 96f, y + 66f, 12f, if (index == 0) Color.rgb(168, 105, 38) else muted)
        round(c, w - 92f, y + 24f, w - 24f, y + 58f, 17f, if (book.renewed) Color.rgb(224, 237, 231) else teal)
        text(c, if (book.renewed) "Renewed" else "Renew", w - (if (book.renewed) 78f else 73f), y + 46f, 11f, if (book.renewed) teal else Color.WHITE, true)
        line(c, 96f, y + 98f, w - 24f)
    }

    private fun nav(c: Canvas, w: Float) {
        round(c, 0f, 674f, w, 740f, 0f, Color.WHITE)
        line(c, 0f, 674f, w)
        text(c, "⌂", 58f, 705f, 24f, if (tab == 0) teal else muted, true)
        text(c, "Home", 47f, 727f, 10f, if (tab == 0) teal else muted, true)
        text(c, "◷", w / 2 - 10f, 705f, 23f, if (tab == 1) teal else muted, true)
        text(c, "History", w / 2 - 23f, 727f, 10f, if (tab == 1) teal else muted, true)
        text(c, "♡", w - 69f, 705f, 24f, muted, true)
        text(c, "Saved", w - 80f, 727f, 10f, muted, true)
    }

    private fun drawDetail(c: Canvas, b: Book, w: Float) {
        text(c, "‹", 24f, 51f, 34f, ink)
        text(c, "BOOK DETAILS", 64f, 44f, 12f, teal, true)
        bookCover(c, b, (w - 112f) / 2, 88f, 112f, 164f)
        text(c, b.title, 24f, 291f, 26f, ink, true)
        text(c, b.author, 24f, 318f, 15f, muted)
        round(c, 24f, 346f, w - 24f, 403f, 13f, Color.rgb(220, 237, 232))
        text(c, "ON LOAN", 42f, 370f, 10f, teal, true)
        text(c, b.due, 42f, 390f, 13f, ink, true)
        text(c, "ABOUT THIS BOOK", 24f, 447f, 12f, teal, true)
        text(c, "A captivating story to lose yourself in, with", 24f, 475f, 14f, muted)
        text(c, "characters and ideas that stay with you long", 24f, 496f, 14f, muted)
        text(c, "after the final page.", 24f, 517f, 14f, muted)
        round(c, 24f, 550f, w - 24f, 600f, 14f, teal)
        text(c, "Renew this loan", 42f, 581f, 14f, Color.WHITE, true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x / resources.displayMetrics.density; val y = event.y / resources.displayMetrics.density
        val w = width / resources.displayMetrics.density
        if (detail != null) {
            if (y < 75f) { detail = null; invalidate(); return true }
            if (y in 545f..610f) { toast = "Loan renewed for 3 more weeks"; invalidate(); postDelayed({ toast = null; invalidate() }, 2200); return true }
            return true
        }
        if (y > 675f) { tab = if (x > w / 3 && x < w * .68f) 1 else 0; invalidate(); return true }
        if (tab == 1) { toast = "Reading history is up to date"; invalidate(); postDelayed({ toast = null; invalidate() }, 1800); return true }
        books.forEachIndexed { i, b ->
            val top = 158f + i * 126f
            if (y in top..(top + 106f)) {
                if (x > w - 105f) { books[i] = b.copy(renewed = true, due = "Due in 3 weeks"); toast = "Loan renewed for 3 more weeks"; invalidate(); postDelayed({ toast = null; invalidate() }, 2200) }
                else { detail = b; invalidate() }
                return true
            }
        }
        return true
    }
}
