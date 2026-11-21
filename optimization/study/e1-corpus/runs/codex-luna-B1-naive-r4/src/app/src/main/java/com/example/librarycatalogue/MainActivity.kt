package com.example.librarycatalogue

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

private data class Book(val title: String, val author: String, val year: Int, val isbn: String, val color: Int)

class MainActivity : Activity() {
    private lateinit var adapter: BookAdapter
    private lateinit var emptyLabel: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var queryVersion = 0
    private val allBooks = Catalogue.books

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.statusBarColor = Color.rgb(247, 248, 246)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 246))
            setPadding(dp(20), dp(12), dp(20), 0)
        }
        root.addView(header())
        val search = searchBox()
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18); bottomMargin = dp(18) })

        val list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            clipToPadding = false
            setPadding(0, 0, 0, dp(8))
        }
        adapter = BookAdapter(this, allBooks.take(30))
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        emptyLabel = TextView(this).apply {
            text = "No books found"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        root.addView(emptyLabel, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val version = ++queryVersion
                val text = s?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
                handler.postDelayed({
                    Thread {
                        val results = if (text.isBlank()) allBooks.take(30) else allBooks.asSequence()
                            .filter { it.title.lowercase(Locale.ROOT).contains(text) || it.author.lowercase(Locale.ROOT).contains(text) || it.isbn.contains(text) }
                            .take(100)
                            .toList()
                        runOnUiThread {
                            if (version != queryVersion) return@runOnUiThread
                            adapter.replace(results)
                            val hasResults = results.isNotEmpty()
                            list.visibility = if (hasResults) View.VISIBLE else View.GONE
                            emptyLabel.visibility = if (hasResults) View.GONE else View.VISIBLE
                            emptyLabel.text = "No books found for \"${s?.toString()}\""
                        }
                    }.start()
                }, 120)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@MainActivity).apply {
            text = "Library catalogue"
            textSize = 28f
            setTextColor(Color.rgb(25, 33, 43))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        addView(TextView(this@MainActivity).apply {
            text = getString(com.example.librarycatalogue.R.string.catalogue_count)
            textSize = 14f
            setTextColor(Color.rgb(104, 114, 125))
            setPadding(0, dp(4), 0, 0)
        })
    }

    private fun searchBox(): EditText = EditText(this).apply {
        hint = getString(R.string.search_hint)
        textSize = 15f
        singleLine = true
        setTextColor(Color.rgb(25, 33, 43))
        setHintTextColor(Color.rgb(104, 114, 125))
        setPadding(dp(16), 0, dp(16), 0)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE)
            setStroke(dp(1), Color.rgb(226, 230, 232))
            cornerRadius = dp(12).toFloat()
        }
        contentDescription = getString(R.string.search_hint)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

private class BookAdapter(private val context: Context, private var books: List<Book>) : BaseAdapter() {
    override fun getCount() = books.size
    override fun getItem(position: Int) = books[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
        val row = (recycled as? LinearLayout) ?: makeRow()
        val book = getItem(position)
        val cover = row.getChildAt(0) as CoverView
        val info = row.getChildAt(1) as LinearLayout
        (info.getChildAt(0) as TextView).text = book.title
        (info.getChildAt(1) as TextView).text = book.author
        (info.getChildAt(2) as TextView).text = "${book.year}  ·  ISBN ${book.isbn}"
        cover.book = book
        return row
    }
    fun replace(newBooks: List<Book>) { books = newBooks; notifyDataSetChanged() }
    private fun makeRow() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(0), dp(8), dp(0), dp(8))
        addView(CoverView(context), LinearLayout.LayoutParams(dp(64), dp(92)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
            addView(TextView(context).apply { textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(25,33,43)); maxLines = 2 })
            addView(TextView(context).apply { textSize = 14f; setTextColor(Color.rgb(76,88,99)); setPadding(0, dp(5), 0, 0) })
            addView(TextView(context).apply { textSize = 12f; setTextColor(Color.rgb(104,114,125)); setPadding(0, dp(6), 0, 0) })
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}

private class CoverView(context: Context) : View(context) {
    var book: Book? = null
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val b = book ?: return
        paint.color = b.color
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 8f, 8f, paint)
        paint.color = Color.argb(45, 255, 255, 255)
        canvas.drawRect(0f, height * .68f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.WHITE
        paint.textSize = width * .19f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(b.title.take(1).uppercase(), width * .18f, height * .42f, paint)
        paint.textSize = width * .10f
        canvas.drawText("LIBRARY", width * .18f, height * .88f, paint)
    }
}

private object Catalogue {
    private val titles = arrayOf("The Quiet Atlas", "A Field Guide to Elsewhere", "The Shape of Rain", "Small Hours", "The Orchard Keeper", "Maps for the Lost", "A Brief History of Light", "The Long Way Home", "Paper Boats", "The Last Garden")
    private val authors = arrayOf("Mara Ellison", "Jon Bell", "Nadia Hart", "Elliot Ward", "Priya Shah", "Theo Martin", "Ruth Lane", "Amelia North")
    private val colors = intArrayOf(Color.rgb(41, 91, 91), Color.rgb(105, 71, 102), Color.rgb(165, 91, 57), Color.rgb(60, 91, 126), Color.rgb(92, 113, 65))
    val books: List<Book> = List(100000) { i ->
        Book("${titles[i % titles.size]} ${i / titles.size + 1}", authors[i % authors.size], 1980 + (i * 7 % 45), "978-${1000 + i / 1000}-${String.format(Locale.US, "%04d", i % 10000)}", colors[i % colors.size])
    }
}
