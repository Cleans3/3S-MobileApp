package com.example.libraryloans

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import android.app.Activity

private data class Book(
    val title: String,
    val author: String,
    val category: String,
    val due: String? = null,
    val returned: String? = null,
    val summary: String,
    val accent: Int
)

class MainActivity : Activity() {
    private val forest = Color.rgb(29, 91, 72)
    private val ink = Color.rgb(27, 42, 37)
    private val muted = Color.rgb(101, 115, 109)
    private val cream = Color.rgb(248, 247, 242)
    private val line = Color.rgb(221, 229, 224)
    private lateinit var content: FrameLayout
    private var selectedTab = 0
    private val currentLoans = mutableListOf(
        Book("The Overstory", "Richard Powers", "Fiction", due = "Due in 3 days", summary = summary("A sweeping novel about nine strangers whose lives are shaped by the natural world."), accent = Color.rgb(53, 87, 73)),
        Book("Braiding Sweetgrass", "Robin Wall Kimmerer", "Nature", due = "Due in 11 days", summary = summary("A botanist's beautiful reflection on plants, science, and our reciprocal relationship with the earth."), accent = Color.rgb(104, 92, 50)),
        Book("Tomorrow, and Tomorrow, and Tomorrow", "Gabrielle Zevin", "Fiction", due = "Due in 18 days", summary = summary("Two friends, a shared love of games, and a creative partnership that lasts for decades."), accent = Color.rgb(126, 69, 62))
    )
    private val readingHistory = listOf(
        Book("The Midnight Library", "Matt Haig", "Fiction", returned = "Returned 12 May 2024", summary("Between life and death there is a library, and within that library, infinite lives."), accent = Color.rgb(68, 70, 109)),
        Book("A Field Guide to Getting Lost", "Rebecca Solnit", "Essays", returned = "Returned 28 Apr 2024", summary("An evocative collection about uncertainty, discovery, and the wisdom of not knowing."), accent = Color.rgb(116, 78, 47)),
        Book("Pachinko", "Min Jin Lee", "Fiction", returned = "Returned 04 Mar 2024", summary("A multigenerational story of family and identity spanning Korea and Japan."), accent = Color.rgb(113, 65, 76))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cream
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        showShell()
    }

    private fun showShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
        }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        val loansNav = navItem("▣", "My loans", 0)
        val historyNav = navItem("↺", "History", 1)
        nav.addView(loansNav, LinearLayout.LayoutParams(0, dp(58), 1f))
        nav.addView(historyNav, LinearLayout.LayoutParams(0, dp(58), 1f))
        root.addView(nav)
        setContentView(root)
        renderTab()
    }

    private fun renderTab() {
        content.removeAllViews()
        content.addView(if (selectedTab == 0) loansPage() else historyPage())
    }

    private fun loansPage(): View {
        val scroll = ScrollView(this)
        val page = pageColumn()
        page.addView(topBar("Good morning, Maya", "Your library at a glance"))
        page.addView(spacer(22))
        page.addView(sectionTitle("Currently borrowed", "${currentLoans.size} books on your shelf"))
        page.addView(spacer(12))
        currentLoans.forEachIndexed { index, book ->
            page.addView(loanCard(book, index))
            page.addView(spacer(12))
        }
        val tip = TextView(this).apply {
            text = "Tip  ·  Renewals add 21 days when no one else is waiting."
            setTextColor(muted)
            textSize = 13f
            setPadding(dp(4), dp(8), dp(4), dp(24))
        }
        page.addView(tip)
        scroll.addView(page)
        return scroll
    }

    private fun historyPage(): View {
        val scroll = ScrollView(this)
        val page = pageColumn()
        page.addView(topBar("Your reading history", "A record of the stories you’ve taken home"))
        page.addView(spacer(22))
        page.addView(sectionTitle("Previously read", "${readingHistory.size} books in your history"))
        page.addView(spacer(12))
        readingHistory.forEach { book ->
            page.addView(historyCard(book))
            page.addView(spacer(12))
        }
        scroll.addView(page)
        return scroll
    }

    private fun topBar(title: String, subtitle: String): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val brand = TextView(this).apply {
            text = "NORTHSTAR LIBRARY"
            setTextColor(forest)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .12f
        }
        box.addView(brand)
        box.addView(spacer(12))
        box.addView(TextView(this).apply {
            text = title
            setTextColor(ink)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        })
        box.addView(TextView(this).apply {
            text = subtitle
            setTextColor(muted)
            textSize = 15f
            setPadding(0, dp(5), 0, 0)
        })
        return box
    }

    private fun sectionTitle(title: String, caption: String): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        row.addView(TextView(this).apply { text = title; setTextColor(ink); textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
        row.addView(TextView(this).apply { text = caption; setTextColor(muted); textSize = 13f; setPadding(0, dp(3), 0, 0) })
        return row
    }

    private fun loanCard(book: Book, index: Int): View {
        val card = cardColumn()
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(cover(book), LinearLayout.LayoutParams(dp(68), dp(92)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        info.addView(label(book.category))
        info.addView(bookTitle(book))
        info.addView(author(book.author))
        info.addView(TextView(this).apply { text = book.due ?: ""; setTextColor(forest); textSize = 13f; setPadding(0, dp(7), 0, 0) })
        top.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(top)
        card.addView(divider())
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val details = TextView(this).apply {
            text = "View details  ›"; setTextColor(forest); textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { showDetails(book) }
        }
        actions.addView(details, LinearLayout.LayoutParams(0, dp(48), 1f))
        val renew = Button(this).apply {
            text = if (book.due == "Renewed") getString(R.string.renewed) else getString(R.string.renew)
            isAllCaps = false; textSize = 13f; setTextColor(Color.WHITE); background = getDrawable(R.drawable.bg_primary_button)
            isEnabled = book.due != "Renewed"
            setOnClickListener {
                currentLoans[index] = book.copy(due = "Renewed")
                Toast.makeText(this@MainActivity, "Loan renewed for 21 days", Toast.LENGTH_SHORT).show()
                renderTab()
            }
        }
        actions.addView(renew, LinearLayout.LayoutParams(dp(112), dp(42)))
        card.addView(actions)
        return card
    }

    private fun historyCard(book: Book): View {
        val card = cardColumn()
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(cover(book), LinearLayout.LayoutParams(dp(58), dp(78)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        info.addView(bookTitle(book))
        info.addView(author(book.author))
        info.addView(TextView(this).apply { text = book.returned; setTextColor(muted); textSize = 13f; setPadding(0, dp(7), 0, 0) })
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(row)
        card.setOnClickListener { showDetails(book) }
        return card
    }

    private fun showDetails(book: Book) {
        val dialog = Dialog(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(18)); setBackgroundColor(Color.WHITE) }
        box.addView(cover(book), LinearLayout.LayoutParams(dp(92), dp(124)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        box.addView(TextView(this).apply { text = bookTitleText(book); setTextColor(ink); textSize = 23f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(4)) })
        box.addView(TextView(this).apply { text = book.author; setTextColor(muted); textSize = 15f; gravity = Gravity.CENTER })
        box.addView(TextView(this).apply { text = book.summary; setTextColor(ink); textSize = 15f; setLineSpacing(0f, 1.15f); setPadding(0, dp(22), 0, dp(18)) })
        val close = Button(this).apply { text = "Close"; isAllCaps = false; setTextColor(forest); setOnClickListener { dialog.dismiss() } }
        box.addView(close, LinearLayout.LayoutParams(-1, dp(46)))
        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun navItem(icon: String, name: String, tab: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true
        addView(TextView(this@MainActivity).apply { text = icon; textSize = 22f; gravity = Gravity.CENTER; setTextColor(if (selectedTab == tab) forest else muted) })
        addView(TextView(this@MainActivity).apply { text = name; textSize = 12f; gravity = Gravity.CENTER; setTextColor(if (selectedTab == tab) forest else muted); typeface = if (selectedTab == tab) Typeface.DEFAULT_BOLD else Typeface.DEFAULT })
        setOnClickListener { selectedTab = tab; renderTab(); (parent as? ViewGroup)?.let { } }
    }

    private fun cover(book: Book): TextView = TextView(this).apply {
        text = book.title.split(" ").take(2).joinToString("\n") { it.uppercase() }
        setTextColor(Color.WHITE); textSize = 10f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
        setBackgroundColor(book.accent)
        setPadding(dp(5), dp(5), dp(5), dp(5))
    }

    private fun bookTitle(book: Book) = TextView(this).apply { text = bookTitleText(book); setTextColor(ink); textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(4), 0, 0) }
    private fun bookTitleText(book: Book) = book.title
    private fun author(name: String) = TextView(this).apply { text = name; setTextColor(muted); textSize = 13f; setPadding(0, dp(3), 0, 0) }
    private fun label(text: String) = TextView(this).apply { this.text = text.uppercase(); setTextColor(forest); textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(1)) }
    private fun cardColumn() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(10)); background = getDrawable(R.drawable.bg_card) }
    private fun pageColumn() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(24), dp(22), 0) }
    private fun divider() = View(this).apply { setBackgroundColor(line); layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(0, dp(14), 0, 0) } }
    private fun spacer(height: Int) = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun summary(text: String) = text
}
