package vn.shelftap.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import vn.shelftap.app.data.local.RemoteKeyEntity
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.ShelfTapApiService
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class CatalogueRemoteMediator(
    private val query: String?,
    private val database: ShelfTapDatabase,
    private val apiService: ShelfTapApiService
) : RemoteMediator<Int, TitleEntity>() {

    private val titleDao = database.titleDao()
    private val remoteKeyDao = database.remoteKeyDao()

    override suspend fun initialize(): InitializeAction {
        // If titles already exist locally, skip initial remote refresh so offline catalogue works instantly!
        val localCount = titleDao.getTitleCount()
        return if (localCount > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TitleEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: 0
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        return try {
            val response = apiService.getTitles(
                query = if (query.isNullOrBlank()) null else query,
                page = page,
                size = state.config.pageSize.coerceAtMost(50)
            )

            if (!response.isSuccessful) {
                return MediatorResult.Error(HttpException(response))
            }

            val body = response.body() ?: return MediatorResult.Success(endOfPaginationReached = true)
            val titles = body.items.map { TitleEntity.fromDomain(it.toDomainModel()) }
            val endOfPaginationReached = titles.isEmpty() || (body.page + 1) * body.size >= body.total

            database.withTransaction {
                if (loadType == LoadType.REFRESH && query.isNullOrBlank()) {
                    // Only clear keys if fresh full reload
                    remoteKeyDao.clearRemoteKeys()
                }

                val prevKey = if (page == 0) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = titles.map {
                    RemoteKeyEntity(titleId = it.id, prevKey = prevKey, nextKey = nextKey)
                }

                remoteKeyDao.insertAll(keys)
                titleDao.insertAll(titles)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            // Offline / poor network condition: return error gracefully without stopping cached paging
            MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            MediatorResult.Error(exception)
        } catch (exception: Exception) {
            MediatorResult.Error(exception)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, TitleEntity>): RemoteKeyEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { title ->
            remoteKeyDao.getRemoteKeyByTitleId(title.id)
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, TitleEntity>): RemoteKeyEntity? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { title ->
            remoteKeyDao.getRemoteKeyByTitleId(title.id)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, TitleEntity>): RemoteKeyEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                remoteKeyDao.getRemoteKeyByTitleId(id)
            }
        }
    }
}
