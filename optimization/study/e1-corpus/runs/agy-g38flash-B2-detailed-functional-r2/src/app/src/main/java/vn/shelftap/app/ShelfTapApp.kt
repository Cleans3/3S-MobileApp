package vn.shelftap.app

import android.app.Application
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.remote.AuthInterceptor
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.MemberRepository
import java.util.concurrent.TimeUnit

/**
 * Dependency container for providing repository and network singletons.
 */
class AppContainer(application: Application) {

    val sessionManager = SessionManager(application)

    // Raw client used for unauthenticated or refresh calls to avoid recursive interception
    val rawOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val authInterceptor = AuthInterceptor(
        sessionManager = sessionManager,
        rawClientProvider = { rawOkHttpClient },
        baseUrl = BASE_URL
    )

    // Authenticated client with interceptor for Bearer token & automatic 401 refresh
    val authenticatedOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(authenticatedOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ShelfTapApiService = retrofit.create(ShelfTapApiService::class.java)

    val authRepository = AuthRepository(apiService, sessionManager)
    val memberRepository = MemberRepository(apiService, sessionManager)

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
    }
}

class ShelfTapApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
