package vn.shelftap.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TitleEntity::class,
        TitleFtsEntity::class,
        TitleRemoteKey::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
