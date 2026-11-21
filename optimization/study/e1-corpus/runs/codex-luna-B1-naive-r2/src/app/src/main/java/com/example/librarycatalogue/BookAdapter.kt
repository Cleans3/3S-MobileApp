package com.example.librarycatalogue

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookAdapter : RecyclerView.Adapter<BookAdapter.Holder>() {
    private val items = mutableListOf<Book>()
    private val colors = intArrayOf(Color.rgb(63, 91, 109), Color.rgb(183, 107, 72), Color.rgb(100, 119, 80), Color.rgb(112, 88, 131), Color.rgb(42, 112, 119))
    fun replaceAll(books: List<Book>) { items.clear(); items.addAll(books); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.bookTitle)
        private val author = view.findViewById<TextView>(R.id.bookAuthor)
        private val meta = view.findViewById<TextView>(R.id.bookMeta)
        private val cover = view.findViewById<View>(R.id.coverColor)
        private val coverText = view.findViewById<TextView>(R.id.coverText)
        fun bind(book: Book) {
            title.text = book.title; author.text = book.author; meta.text = "${book.year}  ·  ISBN ${book.isbn}"
            cover.setBackgroundColor(intArrayOf(Color.rgb(63,91,109), Color.rgb(183,107,72), Color.rgb(100,119,80), Color.rgb(112,88,131), Color.rgb(42,112,119))[book.tone])
            coverText.text = book.title.split(" ").take(3).joinToString("\n") { it.take(1) }
        }
    }
}
