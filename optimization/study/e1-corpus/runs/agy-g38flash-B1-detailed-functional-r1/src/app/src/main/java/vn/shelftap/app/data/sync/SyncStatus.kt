package vn.shelftap.app.data.sync

/**
 * State representing catalogue synchronization progress.
 */
sealed interface SyncStatus {
    object Idle : SyncStatus

    data class Syncing(
        val downloaded: Int,
        val total: Int
    ) : SyncStatus {
        val progress: Float
            get() = if (total > 0) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    }

    data class Completed(
        val totalTitles: Int
    ) : SyncStatus

    data class Error(
        val message: String,
        val downloadedSoFar: Int = 0
    ) : SyncStatus
}
