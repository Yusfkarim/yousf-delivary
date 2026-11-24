package com.yousif.delivery.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yousif.delivery.AppGraph

/**
 * Retries pending upserts/deletes and refreshes the local mirror whenever the
 * phone regains internet. Never deletes local rows on failure.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val session = AppGraph.session(applicationContext)
        if (!session.loggedIn) return Result.success()

        val repository = AppGraph.repository(applicationContext)
        return try {
            if (repository.syncAll()) {
                Result.success()
            } else if (runAttemptCount < 10) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (error: Exception) {
            if (runAttemptCount < 10) Result.retry() else Result.failure()
        }
    }
}
