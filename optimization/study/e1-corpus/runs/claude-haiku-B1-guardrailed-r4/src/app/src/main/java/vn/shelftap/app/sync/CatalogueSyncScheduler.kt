package vn.shelftap.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CatalogueSyncScheduler {

    private const val WORK_NAME = "catalogue-sync"

    /**
     * Ensures the catalogue download is running. KEEP means a pending or running download is not
     * replaced. Once the download is complete the worker returns at once, so this is cheap on every launch.
     */
    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<CatalogueSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
