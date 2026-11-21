# WorkManager creates the sync worker reflectively by class name.
-keep class vn.shelftap.app.sync.CatalogueSyncWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room creates the generated database implementation reflectively by name.
-keep class vn.shelftap.app.data.local.ShelfTapDatabase_Impl {
    <init>();
}
