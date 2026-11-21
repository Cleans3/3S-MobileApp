package vn.shelftap.app.data

/** State of the full catalogue download that makes offline browsing possible. */
sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val fetched: Int, val total: Int) : SyncState
    data object Complete : SyncState
    data class Failed(val message: String) : SyncState
}
