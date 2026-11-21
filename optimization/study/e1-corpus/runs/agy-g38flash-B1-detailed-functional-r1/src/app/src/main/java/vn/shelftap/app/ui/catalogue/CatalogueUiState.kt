package vn.shelftap.app.ui.catalogue

import vn.shelftap.app.data.sync.SyncStatus

/**
 * UI State for the Catalogue & Search screen.
 */
data class CatalogueUiState(
    val searchQuery: String = "",
    val totalCachedCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.Idle
)
