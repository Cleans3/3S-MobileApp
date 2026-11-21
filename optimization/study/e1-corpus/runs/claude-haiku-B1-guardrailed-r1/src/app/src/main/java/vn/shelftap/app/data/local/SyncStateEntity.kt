package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row record of how far the catalogue download has got, so it can resume. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int,
    val nextPage: Int,
    val total: Int,
    val complete: Boolean,
)
