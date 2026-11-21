package vn.shelftap.app.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.TitleEntity
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class CatalogueRemoteMediator(
    private val api: ShelfTapApi,
    private val database: AppDatabase
) : RemoteMediator<Int, TitleEntity>() {

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TitleEntity>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val remoteKey = getRemoteKeyForLastItem(state)
                    val nextPage = remoteKey?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                    nextPage
                }
            }

            val response = api.getTitles(
                page = page,
                size = state.config.pageSize
            )

            val endOfPaginationReached = response.items.isEmpty() ||
                    ((response.page + 1) * response.size) >= response.total

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    database.remoteKeyDao().clearRemoteKeys()
                }

                val prevPage = if (page == 0) null else page - 1
                val nextPage = if (endOfPaginationReached) null else page + 1

                val titles = response.items.map { TitleEntity.fromDto(it) }
                val remoteKeys = titles.map {
                    RemoteKeyEntity(
                        titleId = it.id,
                        prevPage = prevPage,
                        nextPage = nextPage
                    )
                }

                database.titleDao().insertTitlesWithFts(titles)
                database.remoteKeyDao().insertAll(remoteKeys)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, TitleEntity>): RemoteKeyEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { title ->
            database.remoteKeyDao().getRemoteKeyByTitleId(title.id)
        }
    }
}
