package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.HistoryItem

@Entity(tableName = "history_items")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleId: String,
    val title: String,
    val outAt: String,
    val returnedAt: String?
) {
    fun toDomain(): HistoryItem = HistoryItem(
        titleId = titleId,
        title = title,
        outAt = outAt,
        returnedAt = returnedAt
    )

    companion object {
        fun fromDomain(item: HistoryItem): HistoryEntity = HistoryEntity(
            titleId = item.titleId,
            title = item.title,
            outAt = item.outAt,
            returnedAt = item.returnedAt
        )
    }
}
