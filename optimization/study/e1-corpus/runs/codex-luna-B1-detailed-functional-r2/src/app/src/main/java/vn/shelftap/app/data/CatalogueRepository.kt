package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CatalogueRepository(private val dao: TitleDao) {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.shelftap.example/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(CatalogueApi::class.java)

    fun observe(query: String): Flow<List<Title>> = (if (query.isBlank()) dao.observeAll() else dao.observeSearch(query.trim()))
        .map { rows -> rows.map(TitleEntity::asTitle) }

    fun observeById(id: String): Flow<Title?> = dao.observeById(id).map { it?.asTitle() }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        var page = 0
        var total = Int.MAX_VALUE
        while (page * 50 < total) {
            val response = api.titles(page = page, size = 50)
            total = response.total
            if (response.items.isEmpty()) break
            dao.insertAll(response.items.map(TitleEntity::from))
            page++
        }
    }

    suspend fun fetchMissing(id: String): Title? = withContext(Dispatchers.IO) {
        runCatching { api.title(id).also { dao.insertAll(listOf(TitleEntity.from(it))) } }.getOrNull()
    }
}
