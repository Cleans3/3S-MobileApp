package vn.shelftap.lab

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import vn.shelftap.lab.model.Book

/** Parses the catalog JSON into Book objects. */
class CatalogRepository(private val client: CatalogClient = CatalogClient()) {
    private val gson = Gson()

    /** Blocking: run it off the main thread. */
    fun loadBooks(): List<Book> {
        val json = client.fetchCatalog()
        return gson.fromJson(json, object : TypeToken<List<Book>>() {}.type)
    }
}
