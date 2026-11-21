package vn.shelftap.app.di

import android.content.Context
import vn.shelftap.app.data.api.NetworkClient
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.auth.AuthRepository
import vn.shelftap.app.data.auth.DefaultAuthRepository
import vn.shelftap.app.data.local.ShelfTapDatabaseHelper
import vn.shelftap.app.data.repository.HistoryRepository
import vn.shelftap.app.data.repository.LoansRepository
import vn.shelftap.app.data.repository.MemberRepository
import vn.shelftap.app.data.repository.TitleRepository

interface AppContainer {
    val authRepository: AuthRepository
    val apiService: ShelfTapApiService
    val databaseHelper: ShelfTapDatabaseHelper
    val loansRepository: LoansRepository
    val historyRepository: HistoryRepository
    val memberRepository: MemberRepository
    val titleRepository: TitleRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val authRepository: AuthRepository by lazy {
        DefaultAuthRepository()
    }

    override val apiService: ShelfTapApiService by lazy {
        NetworkClient.createService(authRepository)
    }

    override val databaseHelper: ShelfTapDatabaseHelper by lazy {
        ShelfTapDatabaseHelper(context)
    }

    override val loansRepository: LoansRepository by lazy {
        LoansRepository(apiService, databaseHelper)
    }

    override val historyRepository: HistoryRepository by lazy {
        HistoryRepository(apiService, databaseHelper)
    }

    override val memberRepository: MemberRepository by lazy {
        MemberRepository(apiService, databaseHelper)
    }

    override val titleRepository: TitleRepository by lazy {
        TitleRepository(apiService, databaseHelper)
    }
}
