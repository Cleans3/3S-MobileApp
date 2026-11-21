package vn.shelftap.app.data.sync

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Syncing(val current: Int, val total: Int) : SyncStatus
    data class Success(val count: Int) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
