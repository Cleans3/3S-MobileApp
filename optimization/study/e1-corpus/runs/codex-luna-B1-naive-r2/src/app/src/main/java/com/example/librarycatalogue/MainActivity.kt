package com.example.librarycatalogue

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val repository = BookRepository()
    private val adapter = BookAdapter()
    private var searchJob: Job? = null
    private var query = ""
    private var loading = false
    private var offset = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val list = findViewById<RecyclerView>(R.id.bookList)
        list.layoutManager = LinearLayoutManager(this); list.adapter = adapter
        findViewById<EditText>(R.id.searchInput).doAfterTextChanged { editable ->
            query = editable?.toString().orEmpty(); searchJob?.cancel()
            searchJob = CoroutineScope(Dispatchers.Main).launch { delay(250); load(true) }
        }
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                if (!loading && dy > 0 && lm.findLastVisibleItemPosition() >= adapter.itemCount - 5) load(false)
            }
        })
        load(true)
    }
    private fun load(reset: Boolean) {
        if (loading) return
        if (reset) offset = 0
        loading = true
        val page = repository.page(if (reset) 0 else offset, 30, query)
        if (reset) adapter.replaceAll(page) else { val current = repository.page(0, offset, query); adapter.replaceAll(current + page) }
        offset = if (reset) page.size else offset + page.size
        findViewById<TextView>(R.id.resultsLabel).text = if (query.isBlank()) getString(R.string.all_books) else getString(R.string.results, offset)
        loading = false
    }
}
