package com.example.librarycatalogue

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlin.math.max
import kotlin.math.min

data class Book(val title: String, val author: String, val category: String, val color: Int)

class MainActivity : Activity() {
    private lateinit var catalogue: CatalogueView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 247, 242)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 247, 242))
            setPadding(dp(20), dp(18), dp(20), 0)
        }
        val heading = TextView(this).apply {
            text = "SHELF"
            textSize = 13f
            letterSpacing = .22f
            setTextColor(Color.rgb(53, 104, 89))
        }
        root.addView(heading, LinearLayout.LayoutParams(-1, dp(28)))
        val title = TextView(this).apply {
            text = "Browse the collection"
            textSize = 28f
            typeface = Typeface.create("sans", Typeface.BOLD)
            setTextColor(Color.rgb(32, 45, 42))
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(58)))

        val search = EditText(this).apply {
            hint = "Search by title or author"
            textSize = 16f
            singleLine = true
            setTextColor(Color.rgb(32, 45, 42))
            setHintTextColor(Color.rgb(120, 128, 123))
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(Color.WHITE, dp(12))
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(14) })
        val count = TextView(this).apply {
            text = "100,000 books"
            textSize = 13f
            setTextColor(Color.rgb(102, 112, 107))
            setPadding(dp(2), 0, 0, dp(8))
        }
        root.addView(count, LinearLayout.LayoutParams(-1, dp(30)))

        catalogue = CatalogueView(this)
        root.addView(catalogue, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, countChars: Int) {
                catalogue.query = s?.toString().orEmpty()
                count.text = if (catalogue.query.isEmpty()) "100,000 books" else "${catalogue.matchCount} matches"
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat()
        setStroke(dp(1), Color.rgb(225, 228, 222))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    inner class CatalogueView(context: Context) : View(context) {
        private val paints = Paint(Paint.ANTI_ALIAS_FLAG)
        private val books = listOf(
            Book("The Midnight Library", "Matt Haig", "Fiction", 0xFF537D73.toInt()),
            Book("Tomorrow, and Tomorrow, and Tomorrow", "Gabrielle Zevin", "Fiction", 0xFFDB8B62.toInt()),
            Book("A Field Guide to Getting Lost", "Rebecca Solnit", "Essays", 0xFF8D789B.toInt()),
            Book("The Art of Stillness", "Pico Iyer", "Culture", 0xFFB7A55E.toInt()),
            Book("Braiding Sweetgrass", "Robin Wall Kimmerer", "Nature", 0xFF6F9270.toInt()),
            Book("Project Hail Mary", "Andy Weir", "Science fiction", 0xFF6389A9.toInt()),
            Book("The Creative Act", "Rick Rubin", "Creativity", 0xFFAF6D73.toInt()),
            Book("Piranesi", "Susanna Clarke", "Fantasy", 0xFF567C91.toInt())
        )
        private var offset = 0f
        private var lastY = 0f
        private var dragging = false
        var query: String = ""
            set(value) { field = value; offset = 0f; invalidate() }
        val matchCount get() = filtered().size
        private fun filtered(): List<Book> = if (query.isBlank()) books else books.filter {
            it.title.contains(query, true) || it.author.contains(query, true)
        }
        private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val list = filtered()
            val rowHeight = dp(142).toFloat()
            val maxOffset = max(0f, list.size * rowHeight - height)
            offset = offset.coerceIn(0f, maxOffset)
            if (list.isEmpty()) {
                paints.color = 0xFF66726C.toInt(); paints.textSize = dp(16).toFloat()
                canvas.drawText("No books found", 0f, dp(42).toFloat(), paints); return
            }
            val first = (offset / rowHeight).toInt().coerceAtLeast(0)
            val last = min(list.size - 1, ((offset + height) / rowHeight).toInt() + 1)
            for (i in first..last) drawBook(canvas, list[i], i, i * rowHeight - offset)
        }

        private fun drawBook(canvas: Canvas, book: Book, index: Int, y: Float) {
            val coverW = dp(78); val coverH = dp(112)
            paints.color = book.color
            canvas.drawRoundRect(0f, y + dp(8), coverW.toFloat(), y + dp(8) + coverH, dp(7).toFloat(), dp(7).toFloat(), paints)
            paints.color = Color.WHITE; paints.textSize = dp(10).toFloat(); paints.typeface = Typeface.DEFAULT_BOLD
            val lines = book.title.split(" ").chunked(2).map { it.joinToString(" ") }.take(4)
            lines.forEachIndexed { n, line -> canvas.drawText(line, dp(8).toFloat(), y + dp(33 + n * 13).toFloat(), paints) }
            paints.color = 0xAAFFFFFF.toInt(); paints.textSize = dp(8).toFloat(); paints.typeface = Typeface.DEFAULT
            canvas.drawText("SHELF", dp(8).toFloat(), y + dp(103).toFloat(), paints)
            val textX = dp(96).toFloat()
            paints.color = 0xFF202D2A.toInt(); paints.textSize = dp(16).toFloat(); paints.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(book.title, textX, y + dp(35).toFloat(), paints)
            paints.color = 0xFF66726C.toInt(); paints.textSize = dp(13).toFloat(); paints.typeface = Typeface.DEFAULT
            canvas.drawText(book.author, textX, y + dp(59).toFloat(), paints)
            paints.color = 0xFF356859.toInt(); paints.textSize = dp(11).toFloat()
            canvas.drawText(book.category.uppercase(), textX, y + dp(90).toFloat(), paints)
            paints.color = 0xFFE0E3DD.toInt(); canvas.drawRect(0f, y + dp(133).toFloat(), width.toFloat(), y + dp(134).toFloat(), paints)
        }

        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastY = event.y; dragging = true; return true }
                MotionEvent.ACTION_MOVE -> if (dragging) {
                    offset -= event.y - lastY; lastY = event.y; invalidate(); return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { dragging = false; return true }
            }
            return true
        }
    }
}
