package br.com.openmonetis.companion

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import br.com.openmonetis.companion.data.local.AppDatabase
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.data.remote.OpenMonetisApi
import br.com.openmonetis.companion.data.remote.dto.*
import br.com.openmonetis.companion.service.SyncWorker
import br.com.openmonetis.companion.util.SecureStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private class Api(private val code: Int = 200, private val omitLast: Boolean = false) : OpenMonetisApi {
        val requests = mutableListOf<InboxBatchRequest>()
        override suspend fun submitBatch(request: InboxBatchRequest): Response<InboxBatchResponse> {
            requests += request
            if (code != 200) return Response.error(code, "{}".toResponseBody())
            val results = (if (omitLast) request.items.dropLast(1) else request.items).map {
                BatchResult(it.clientId, "server-${it.clientId}", true, null)
            }
            return Response.success(InboxBatchResponse(null, request.items.size, results.size, 0, results, null))
        }
        override suspend fun healthCheck(): Response<HealthResponse> = error("Unused")
        override suspend fun verifyToken(): Response<VerifyTokenResponse> = error("Unused")
        override suspend fun submitNotification(request: InboxRequest): Response<InboxResponse> = error("Unused")
    }
    private suspend fun withWorker(count: Int, api: Api, assertResult: suspend (AppDatabase, ListenableWorker.Result) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val storage = SecureStorage(context)
        storage.saveCredentials("https://example.invalid", "test-token", null, null)
        storage.notifySyncSuccess = false
        storage.notifySyncError = false
        val now = System.currentTimeMillis()
        db.notificationDao().insertAll((1..count).map { NotificationEntity(
            "test-$it", "test.app", "Banco de teste", null, "Compra de teste", now, null, 1.0, null, null, createdAt = now)
        })
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters) =
                SyncWorker(appContext, params, db.notificationDao(), db.syncLogDao(), api, storage)
        }
        try {
            val worker = TestListenableWorkerBuilder<SyncWorker>(context).setWorkerFactory(factory).build()
            assertResult(db, worker.doWork())
        } finally { storage.clear(); db.close() }
    }
    @Test fun drainsMultipleBatchesAndRecordsActualSendTime() = runBlocking {
        val api = Api()
        withWorker(120, api) { db, result ->
            assertTrue(result is ListenableWorker.Result.Success)
            assertEquals(listOf(50, 50, 20), api.requests.map { it.items.size })
            assertEquals(120, db.notificationDao().countByStatus(SyncStatus.SYNCED))
            assertTrue(db.notificationDao().getAll().all { it.syncedAt != null })
        }
    }
    @Test fun omittedResultsDoNotStaySendingOrLoopForever() = runBlocking {
        val api = Api(omitLast = true)
        withWorker(3, api) { db, result ->
            assertTrue(result is ListenableWorker.Result.Failure)
            assertEquals(1, api.requests.size)
            assertEquals(2, db.notificationDao().countByStatus(SyncStatus.SYNCED))
            assertEquals(1, db.notificationDao().countByStatus(SyncStatus.SYNC_FAILED))
            assertEquals(0, db.notificationDao().countByStatus(SyncStatus.SYNCING))
        }
    }
    @Test fun authorizationFailureReturnsActionableCause() = runBlocking {
        withWorker(2, Api(401)) { db, result ->
            assertTrue(result is ListenableWorker.Result.Failure)
            assertEquals("AUTH", (result as ListenableWorker.Result.Failure).outputData.getString("error"))
            assertTrue(db.notificationDao().getAll().all { it.syncError == "Token inválido ou expirado" })
        }
    }
    @Test fun networkFailureRetriesWithoutStrandingRows() = runBlocking {
        withWorker(2, Api(503)) { db, result ->
            assertTrue(result is ListenableWorker.Result.Retry)
            assertEquals(2, db.notificationDao().countByStatus(SyncStatus.SYNC_FAILED))
            assertEquals(0, db.notificationDao().countByStatus(SyncStatus.SYNCING))
        }
    }
}
