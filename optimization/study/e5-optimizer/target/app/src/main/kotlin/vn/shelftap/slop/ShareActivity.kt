package vn.shelftap.slop

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Shows a book that another app shared with ShelfTap. */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = intent.getStringExtra("book") ?: "nothing shared" })
    }
}
