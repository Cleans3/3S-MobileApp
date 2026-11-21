package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.model.BookDetail
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.security.EncryptedStorage

class RenewalNotAllowedException(message: String = "This loan cannot be renewed") : Exception(message)

data class CachedResource<T>(
    val data: T,
    val isOffline: Boolean
)

class LibraryRepository(
    private val apiService: ShelfTapApiService,
    private val encryptedStorage: EncryptedStorage,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val LOANS_CACHE = "loans_cache.enc"
        private const val HISTORY_CACHE = "history_cache.enc"
        private const val PROFILE_CACHE = "profile_cache.enc"
        private fun bookCache(titleId: String) = "book_${titleId}.enc"
    }

    suspend fun getLoans(): CachedResource<List<Loan>> = withContext(ioDispatcher) {
        try {
            val remoteLoans = apiService.getLoans()
            val serialized = json.encodeToString(remoteLoans)
            encryptedStorage.saveEncrypted(LOANS_CACHE, serialized)
            CachedResource(data = remoteLoans, isOffline = false)
        } catch (e: Exception) {
            val cachedJson = encryptedStorage.readEncrypted(LOANS_CACHE)
            if (cachedJson != null) {
                try {
                    val cachedLoans = json.decodeFromString<List<Loan>>(cachedJson)
                    return@withContext CachedResource(data = cachedLoans, isOffline = true)
                } catch (jsonErr: Exception) {
                    throw e
                }
            }
            throw e
        }
    }

    suspend fun renewLoan(loanId: String): Loan = withContext(ioDispatcher) {
        try {
            val updated = apiService.renewLoan(loanId)
            val cachedJson = encryptedStorage.readEncrypted(LOANS_CACHE)
            if (cachedJson != null) {
                try {
                    val cachedLoans = json.decodeFromString<List<Loan>>(cachedJson).toMutableList()
                    val index = cachedLoans.indexOfFirst { it.loanId == loanId }
                    if (index >= 0) {
                        cachedLoans[index] = updated
                        encryptedStorage.saveEncrypted(LOANS_CACHE, json.encodeToString(cachedLoans))
                    }
                } catch (_: Exception) {}
            }
            updated
        } catch (e: HttpException) {
            if (e.code() == 409) {
                throw RenewalNotAllowedException()
            }
            throw e
        }
    }

    suspend fun getHistory(): CachedResource<List<HistoryItem>> = withContext(ioDispatcher) {
        try {
            val remoteHistory = apiService.getHistory()
            val serialized = json.encodeToString(remoteHistory)
            encryptedStorage.saveEncrypted(HISTORY_CACHE, serialized)
            CachedResource(data = remoteHistory, isOffline = false)
        } catch (e: Exception) {
            val cachedJson = encryptedStorage.readEncrypted(HISTORY_CACHE)
            if (cachedJson != null) {
                try {
                    val cachedHistory = json.decodeFromString<List<HistoryItem>>(cachedJson)
                    return@withContext CachedResource(data = cachedHistory, isOffline = true)
                } catch (jsonErr: Exception) {
                    throw e
                }
            }
            throw e
        }
    }

    suspend fun getProfile(): CachedResource<MemberProfile> = withContext(ioDispatcher) {
        try {
            val remoteProfile = apiService.getProfile()
            val serialized = json.encodeToString(remoteProfile)
            encryptedStorage.saveEncrypted(PROFILE_CACHE, serialized)
            CachedResource(data = remoteProfile, isOffline = false)
        } catch (e: Exception) {
            val cachedJson = encryptedStorage.readEncrypted(PROFILE_CACHE)
            if (cachedJson != null) {
                try {
                    val cachedProfile = json.decodeFromString<MemberProfile>(cachedJson)
                    return@withContext CachedResource(data = cachedProfile, isOffline = true)
                } catch (jsonErr: Exception) {
                    throw e
                }
            }
            throw e
        }
    }

    suspend fun setHistoryEnabled(enabled: Boolean): MemberProfile = withContext(ioDispatcher) {
        val updatedProfile = apiService.updatePrivacy(PrivacyRequest(historyEnabled = enabled))
        encryptedStorage.saveEncrypted(PROFILE_CACHE, json.encodeToString(updatedProfile))
        if (!enabled) {
            encryptedStorage.saveEncrypted(HISTORY_CACHE, json.encodeToString(emptyList<HistoryItem>()))
        }
        updatedProfile
    }

    suspend fun getTitleDetail(titleId: String): CachedResource<BookDetail> = withContext(ioDispatcher) {
        val cacheKey = bookCache(titleId)
        try {
            val remoteBook = apiService.getTitle(titleId)
            encryptedStorage.saveEncrypted(cacheKey, json.encodeToString(remoteBook))
            CachedResource(data = remoteBook, isOffline = false)
        } catch (e: Exception) {
            val cachedJson = encryptedStorage.readEncrypted(cacheKey)
            if (cachedJson != null) {
                try {
                    val cachedBook = json.decodeFromString<BookDetail>(cachedJson)
                    return@withContext CachedResource(data = cachedBook, isOffline = true)
                } catch (jsonErr: Exception) {
                    throw e
                }
            }
            throw e
        }
    }
}
