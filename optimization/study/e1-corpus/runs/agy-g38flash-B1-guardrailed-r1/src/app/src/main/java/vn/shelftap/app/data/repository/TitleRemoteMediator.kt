package vn.shelftap.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.local.TitleRemoteKey
import vn.shelftap.app.data.model.toEntity
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.di.DispatcherProvider
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class TitleRemoteMediator(
    private val query: String?,
    private val apiService: ShelfTapApiService,
    private val database: AppDatabase,
    private val dispatcherProvider: DispatcherProvider
) : RemoteMediator<Int, TitleEntity>() {

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TitleEntity>
    ): MediatorResult = withContext(dispatcherProvider.io) {
        try {
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextPage?.minus(1) ?: 0
                }
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    val prevKey = remoteKeys?.prevPage
                        ?: return@withContext MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    prevKey
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextKey = remoteKeys?.nextPage
                        ?: return@withContext MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    nextKey
                }
            }

            val pageSize = state.config.pageSize.coerceAtMost(50)
            val response = apiService.getTitles(
                query = query?.ifBlank { null },
                page = page,
                size = pageSize
            )

            val titles = response.items.map { it.toEntity() }
            val endOfPagination = titles.isEmpty() || (page + 1) * pageSize >= response.total

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    if (query.isNullOrBlank()) {
                        database.remoteKeyDao().clearRemoteKeys()
                    }
                }

                val prevPage = if (page == 0) null else page - 1
                val nextPage = if (endOfPagination) null else page + 1
                val keys = titles.map {
                    TitleRemoteKey(id = it.id, prevPage = prevPage, nextPage = nextPage)
                }

                database.remoteKeyDao().insertAll(keys)
                database.titleDao().upsertTitlesWithFts(titles)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPagination)
        } catch (e: IOException) {
            // When offline or poor network, return Error so cached Room items keep working
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, TitleEntity>): TitleRemoteKey? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { title ->
            database.remoteKeyDao().getRemoteKeyById(title.id)
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, TitleEntity>): TitleRemoteKey? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { title ->
            database.remoteKeyDao().getRemoteKeyById(title.id)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, TitleEntity>): TitleRemoteKey? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                database.remoteKeyDao().getRemoteKeyById(id)
            }
        }
    }
}
