package br.com.opensheets.companion.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.entities.SyncStatus
import br.com.opensheets.companion.data.remote.OpenSheetsApi
import br.com.opensheets.companion.data.remote.dto.InboxBatchRequest
import br.com.opensheets.companion.data.remote.dto.InboxRequest
import br.com.opensheets.companion.util.SecureStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationDao: NotificationDao,
    private val api: OpenSheetsApi,
    private val secureStorage: SecureStorage
) : CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")

        // Check if configured
        if (!secureStorage.isConfigured()) {
            Log.w(TAG, "Not configured, skipping sync")
            return Result.failure()
        }

        // Get pending notifications
        val pending = notificationDao.getPendingSync(limit = BATCH_SIZE)

        if (pending.isEmpty()) {
            Log.d(TAG, "No pending notifications to sync")
            return Result.success()
        }

        Log.d(TAG, "Syncing ${pending.size} notifications")

        return try {
            // Mark as syncing
            pending.forEach { notification ->
                notificationDao.updateStatus(notification.id, SyncStatus.SYNCING)
            }

            // Build batch request
            val items = pending.map { notification ->
                InboxRequest(
                    sourceApp = notification.sourceApp,
                    sourceAppName = notification.sourceAppName,
                    deviceId = secureStorage.deviceId,
                    originalTitle = notification.originalTitle,
                    originalText = notification.originalText,
                    notificationTimestamp = dateFormat.format(Date(notification.notificationTimestamp)),
                    parsedName = notification.parsedName,
                    parsedAmount = notification.parsedAmount,
                    parsedDate = notification.parsedDate?.let { dateFormat.format(Date(it)) },
                    parsedCardLastDigits = notification.parsedCardLastDigits,
                    parsedTransactionType = notification.parsedTransactionType,
                    clientId = notification.id
                )
            }

            val response = api.submitBatch(InboxBatchRequest(items))

            if (response.isSuccessful) {
                val body = response.body()

                body?.results?.forEach { result ->
                    val clientId = result.clientId ?: return@forEach

                    if (result.success && result.serverId != null) {
                        notificationDao.markSynced(clientId, result.serverId)
                    } else {
                        notificationDao.markSyncFailed(clientId, result.error)
                    }
                }

                Log.d(TAG, "Sync completed: ${body?.success}/${body?.total} successful")

                // If there are more pending, schedule another sync
                val remainingCount = notificationDao.countPending()
                if (remainingCount > 0) {
                    enqueue(applicationContext)
                }

                Result.success()
            } else {
                val errorCode = response.code()

                if (errorCode == 401) {
                    // Token expired, try to refresh
                    Log.w(TAG, "Token expired, attempting refresh")
                    // TODO: Implement token refresh
                    pending.forEach { notification ->
                        notificationDao.markSyncFailed(notification.id, "Token expirado")
                    }
                    Result.failure()
                } else if (errorCode == 429) {
                    // Rate limited, retry later
                    Log.w(TAG, "Rate limited, will retry")
                    pending.forEach { notification ->
                        notificationDao.updateStatus(notification.id, SyncStatus.PENDING_SYNC)
                    }
                    Result.retry()
                } else {
                    Log.e(TAG, "Sync failed with code $errorCode")
                    pending.forEach { notification ->
                        notificationDao.markSyncFailed(notification.id, "HTTP $errorCode")
                    }
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception", e)
            pending.forEach { notification ->
                notificationDao.markSyncFailed(notification.id, e.message)
            }
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "sync_notifications"
        private const val BATCH_SIZE = 50

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
