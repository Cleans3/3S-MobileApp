package vn.shelftap.app.data

/** State of the background download of the whole catalogue. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Running(val loaded: Int, val total: Int) : SyncStatus
    data object Done : SyncStatus
    data object Failed : SyncStatus
}
