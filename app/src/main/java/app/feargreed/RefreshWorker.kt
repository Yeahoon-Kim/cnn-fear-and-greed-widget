package app.feargreed

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val store = WidgetStore(applicationContext)
        val now = Instant.now()
        val force = inputData.getBoolean(KEY_FORCE, false)
        val blocked = store.blockedUntil?.isAfter(now) == true
        val due = RefreshPolicy.isDue(now, store.lastAttempt)

        if (store.apiKey.isNotBlank() && (force || (due && !blocked))) {
            store.lastAttempt = now
            when (val result = FearGreedApi.fetch(store.apiKey)) {
                is FetchResult.Success -> {
                    store.snapshot = result.snapshot
                    store.status = WidgetStatus.OK
                    store.blockedUntil = null
                }
                FetchResult.AuthFailed -> store.status = WidgetStatus.AUTH
                FetchResult.QuotaExceeded -> {
                    store.status = WidgetStatus.QUOTA
                    store.blockedUntil = now.plus(QUOTA_BACKOFF)
                }
                FetchResult.Failed -> store.status = WidgetStatus.ERROR
            }
        }
        FearGreedWidget.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val KEY_FORCE = "force"
        private const val PERIODIC_NAME = "fear_greed_periodic"
        const val ONE_TIME_NAME = "fear_greed_now"
        private val QUOTA_BACKOFF = Duration.ofHours(6)

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun refreshNow(context: Context, force: Boolean = true) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(networkConstraint)
                .setInputData(workDataOf(KEY_FORCE to force))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
