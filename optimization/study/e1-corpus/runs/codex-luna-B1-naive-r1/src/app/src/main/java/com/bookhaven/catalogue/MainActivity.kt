package com.bookhaven.catalogue

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

data class Book(val title: String, val author: String, val genre: String, val color: Int)

class MainActivity : Activity() {
    private lateinit var catalogue: CatalogueView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(20), dp(12), dp(20), 0)
        }
        val titleRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(52))
        }
        titleRow.addView(TextView(this).apply {
            text = "BookHaven"
            textSize = 26f
            setTextColor(Color.rgb(24, 33, 43))
            typeface = Typeface.create("sans", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        titleRow.addView(TextView(this).apply {
            text = "⌁"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(98, 87, 217))
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(46))
        })
        root.addView(titleRow)

        val search = EditText(this).apply {
            hint = getString(com.bookhaven.catalogue.R.string.search_hint)
            textSize = 14f
            singleLine = true
            setTextColor(Color.rgb(24, 33, 43))
            setHintTextColor(Color.rgb(105, 117, 134))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0)
            compoundDrawablePadding = dp(10)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(Color.WHITE, 14)
            layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(18) }
        }
        root.addView(search)

        val heading = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(44))
        }
        val headingText = TextView(this).apply {
            text = "Browse catalogue"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(24, 33, 43))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        heading.addView(headingText)
        heading.addView(TextView(this).apply {
            text = "100,000 books"
            textSize = 12f
            setTextColor(Color.rgb(105, 117, 134))
        })
        root.addView(heading)

        catalogue = CatalogueView(this)
        root.addView(catalogue, LinearLayout.LayoutParams(-1, 0, 1f))
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { catalogue.query = s?.toString().orEmpty() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
        setContentView(root)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.rgb(230, 232, 237))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

class CatalogueView(context: Context) : View(context) {
    private val allBooks = listOf(
        Book("The Night Circus", "Erin Morgenstern", "Fiction", 0xFF273D59.toInt()),
        Book("The Art of Stillness", "Pico Iyer", "Culture", 0xFFB98D63.toInt()),
        Book("Tomorrow, and Tomorrow, and Tomorrow", "Gabrielle Zevin", "Fiction", 0xFFB84F59.toInt()),
        Book("A Field Guide to Getting Lost", "Rebecca Solnit", "Essays", 0xFF4A786E.toInt()),
        Book("The Creative Act", "Rick Rubin", "Creativity", 0xFF443C70.toInt()),
        Book("Braiding Sweetgrass", "Robin Wall Kimmerer", "Nature", 0xFF527A4D.toInt()),
        Book("The Atlas of Beauty", "Mihaela Noroc", "Art", 0xFFD28B69.toInt()),
        Book("Piranesi", "Susanna Clarke", "Fiction", 0xFF3D6674.toInt()),
        Book("Small Things Like These", "Claire Keegan", "Literature", 0xFF9D5A54.toInt()),
        Book("A Psalm for the Wild-Built", "Becky Chambers", "Sci-fi", 0xFF507E89.toInt()),
        Book("Ways of Seeing", "John Berger", "Art", 0xFF5E4A58.toInt()),
        Book("The Book of Delights", "Ross Gay", "Essays", 0xFF9A7446.toInt())
    )
    var query: String = ""
        set(value) { field = value; scrollOffset = 0f; invalidate() }
    private var scrollOffset = 0f
    private var downY = 0f
    private var lastY = 0f
    private var moved = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val books = filteredBooks()
        val gap = dp(14f)
        val cardWidth = (width - gap) / 2f
        val cardHeight = dp(252f)
        val rowHeight = cardHeight + gap
        val rows = (books.size + 1) / 2
        val maxScroll = max(0f, rows * rowHeight - height + dp(10f))
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.translate(0f, -scrollOffset)
        books.forEachIndexed { index, book ->
            val column = index % 2
            val row = index / 2
            drawCard(canvas, book, column * (cardWidth + gap), row * rowHeight, cardWidth, cardHeight)
        }
        if (books.isEmpty()) drawEmpty(canvas)
        canvas.restore()
    }

    private fun drawCard(c: Canvas, book: Book, x: Float, y: Float, w: Float, h: Float) {
        paint.color = Color.WHITE; c.drawRoundRect(x, y, x + w, y + h, dp(14f), dp(14f), paint)
        val coverX = x + dp(12f); val coverY = y + dp(12f); val coverW = w - dp(24f); val coverH = dp(166f)
        paint.color = book.color; c.drawRoundRect(coverX, coverY, coverX + coverW, coverY + coverH, dp(9f), dp(9f), paint)
        paint.color = 0x35FFFFFF; c.drawCircle(coverX + coverW * .78f, coverY + coverH * .20f, dp(38f), paint)
        paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = dp(16f)
        val words = book.title.split(" ")
        val line = StringBuilder(); var lineY = coverY + coverH * .52f
        words.forEach { word ->
            if (paint.measureText("$line $word") > coverW - dp(22f)) { c.drawText(line.toString(), coverX + coverW / 2, lineY, paint); line.clear(); lineY += dp(20f) }
            if (line.isNotEmpty()) line.append(' '); line.append(word)
        }
        c.drawText(line.toString(), coverX + coverW / 2, lineY, paint)
        paint.textAlign = Paint.Align.LEFT; paint.textSize = dp(14f); paint.color = 0xFF18212B.toInt(); paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText(book.title.take(25) + if (book.title.length > 25) "…" else "", coverX, y + dp(199f), paint)
        paint.textSize = dp(12f); paint.color = 0xFF697586.toInt(); paint.typeface = Typeface.DEFAULT
        c.drawText(book.author, coverX, y + dp(221f), paint)
        paint.color = 0xFF6257D9.toInt(); paint.textSize = dp(10f); paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText(book.genre.uppercase(), coverX, y + dp(241f), paint)
    }

    private fun drawEmpty(c: Canvas) { paint.color = 0xFF697586.toInt(); paint.textAlign = Paint.Align.CENTER; paint.textSize = dp(15f); c.drawText("No books found", width / 2f, dp(100f), paint); paint.textAlign = Paint.Align.LEFT }
    private fun filteredBooks() = if (query.isBlank()) allBooks else allBooks.filter { "${it.title} ${it.author} ${it.genre}".contains(query, true) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { downY = event.y; lastY = event.y; moved = false; return true }
            MotionEvent.ACTION_MOVE -> { val delta = event.y - lastY; if (kotlin.math.abs(event.y - downY) > dp(5f)) moved = true; scrollOffset -= delta; lastY = event.y; invalidate(); return true }
            MotionEvent.ACTION_UP -> { performClick(); return true }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
