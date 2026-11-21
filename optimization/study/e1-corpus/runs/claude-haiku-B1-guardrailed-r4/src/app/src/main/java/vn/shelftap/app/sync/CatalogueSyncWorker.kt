package vn.shelftap.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONException
import java.io.IOException
import vn.shelftap.app.ShelfTapApp

class CatalogueSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sync = (applicationContext as ShelfTapApp).container.sync
        return try {
            sync.run()
            Result.success()
        } catch (e: IOException) {
            // Network dropped or server error: WorkManager retries with backoff and the download resumes.
            Result.retry()
        } catch (e: JSONException) {
            Result.failure()
        }
    }
}
