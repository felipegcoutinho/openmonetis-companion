package br.com.openmonetis.companion.service

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
import br.com.openmonetis.companion.BuildConfig
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.dao.SyncLogDao
import br.com.openmonetis.companion.data.local.entities.SyncLogEntity
import br.com.openmonetis.companion.data.local.entities.SyncLogType
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.data.remote.OpenMonetisApi
import br.com.openmonetis.companion.data.remote.dto.InboxBatchRequest
import br.com.openmonetis.companion.data.remote.dto.InboxRequest
import br.com.openmonetis.companion.util.SecureStorage
import br.com.openmonetis.companion.util.SyncResultNotifier
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
    private val syncLogDao: SyncLogDao,
    private val api: OpenMonetisApi,
    private val secureStorage: SecureStorage
) : CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    private val syncResultNotifier = SyncResultNotifier(applicationContext, secureStorage)

    override suspend fun doWork(): Result {
        cleanOldData()
        if (!secureStorage.isConfigured()) return Result.failure(androidx.work.workDataOf("error" to "CONFIGURATION"))
        // Unique work serializes claims. Recover rows left by a process interruption.
        notificationDao.recoverInterruptedSync()
        var sent = 0
        var failed = 0
        var afterTime = Long.MIN_VALUE
        var afterId = ""
        val cutoff = System.currentTimeMillis()
        val started = cutoff
        while (true) {
            val candidates = notificationDao.getSyncBatch(cutoff, afterTime, afterId, BATCH_SIZE)
            if (candidates.isEmpty()) break
            afterTime = candidates.last().createdAt
            afterId = candidates.last().id
            val pending = candidates.filter { notificationDao.claimForSync(it.id) == 1 }
            if (pending.isEmpty()) continue
            setProgress(androidx.work.workDataOf("sent" to sent, "failed" to failed))
            try {
                val response = api.submitBatch(InboxBatchRequest(pending.map { notification ->
                    InboxRequest(sourceApp = notification.sourceApp, sourceAppName = notification.sourceAppName,
                        originalTitle = notification.originalTitle, originalText = notification.originalText,
                        notificationTimestamp = dateFormat.format(Date(notification.notificationTimestamp)),
                        parsedName = notification.parsedName, parsedAmount = notification.parsedAmount,
                        clientId = notification.id)
                }))
                if (response.isSuccessful) {
                    val results = response.body()?.results.orEmpty().associateBy { it.clientId }
                    // Every claimed item reaches a terminal state, even with an incomplete response.
                    pending.forEach { notification ->
                        val result = results[notification.id]
                        if (result?.success == true && result.serverId != null) {
                            notificationDao.markSynced(notification.id, result.serverId)
                            syncResultNotifier.notifySuccess(notification)
                            sent++
                        } else {
                            notificationDao.markSyncFailed(notification.id, ITEM_SYNC_ERROR)
                            syncResultNotifier.notifyError(notification, ITEM_SYNC_ERROR)
                            failed++
                        }
                    }
                    if (sent > 0) secureStorage.lastSyncTime = System.currentTimeMillis()
                } else {
                    val unauthorized = response.code() == 401 || response.code() == 403
                    val error = if (unauthorized) TOKEN_SYNC_ERROR else TEMPORARY_SYNC_ERROR
                    pending.forEach { notificationDao.markSyncFailed(it.id, error) }
                    log(SyncLogType.ERROR, error, details = "HTTP ${response.code()}")
                    return if (unauthorized) Result.failure(androidx.work.workDataOf("error" to "AUTH", "sent" to sent))
                        else Result.retry()
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    pending.forEach { notificationDao.changeStatusIf(it.id, SyncStatus.SYNCING, SyncStatus.PENDING_SYNC) }
                }
                throw cancelled
            } catch (_: Exception) {
                pending.forEach { notificationDao.markSyncFailed(it.id, TEMPORARY_SYNC_ERROR) }
                log(SyncLogType.ERROR, TEMPORARY_SYNC_ERROR)
                return Result.retry()
            }
            // WorkManager limits workers to ten minutes; release remaining work for another attempt.
            if (System.currentTimeMillis() - started > 8 * 60_000L) return Result.retry()
        }
        log(if (failed == 0) SyncLogType.SUCCESS else SyncLogType.WARNING,
            "Sincronização concluída: $sent enviadas, $failed falhas")
        val output = androidx.work.workDataOf("sent" to sent, "failed" to failed, "error" to if (failed > 0) "ITEM" else "")
        return if (failed > 0) Result.failure(output) else Result.success(output)
    }

    private suspend fun log(
        type: SyncLogType,
        message: String,
        notificationId: String? = null,
        details: String? = null
    ) {
        syncLogDao.insert(
            SyncLogEntity(
                type = type,
                message = message,
                notificationId = notificationId,
                details = details
            )
        )
    }

    private suspend fun cleanOldData() {
        val currentTime = System.currentTimeMillis()
        syncLogDao.deleteOlderThan(currentTime - LOG_RETENTION_DAYS * DAY_IN_MILLISECONDS)
        notificationDao.deleteTerminalOlderThan(
            currentTime - NOTIFICATION_RETENTION_DAYS * DAY_IN_MILLISECONDS
        )
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_notifications"
        private const val BATCH_SIZE = 50
        private const val LOG_RETENTION_DAYS = 7L
        private const val NOTIFICATION_RETENTION_DAYS = 30L
        private const val DAY_IN_MILLISECONDS = 24L * 60L * 60L * 1_000L
        private const val ITEM_SYNC_ERROR = "Falha ao enviar lançamento"
        private const val TOKEN_SYNC_ERROR = "Token inválido ou expirado"
        private const val TEMPORARY_SYNC_ERROR = "Falha temporária de comunicação"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag("requested_at:${System.currentTimeMillis()}")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
