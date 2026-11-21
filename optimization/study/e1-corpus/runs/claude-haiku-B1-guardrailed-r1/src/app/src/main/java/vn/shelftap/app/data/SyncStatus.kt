package vn.shelftap.app.data

/** Progress of the one-time download of the whole catalogue. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Running(val loaded: Int, val total: Int) : SyncStatus
    data object Complete : SyncStatus
    data object Failed : SyncStatus
}
