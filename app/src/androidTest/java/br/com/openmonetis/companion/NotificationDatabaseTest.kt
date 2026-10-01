package br.com.openmonetis.companion

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.openmonetis.companion.data.local.AppDatabase
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun notification(id: String, status: SyncStatus, created: Long) = NotificationEntity(
        id, "test.app", "Banco de teste", null, "Compra de teste", created, null, 1234.56, null, null,
        syncStatus = status, createdAt = created)

    @Test fun migrationPreservesLegacyDataWithoutInventingSendTime() = runBlocking {
        val name = "migration-ux-test.db"
        context.deleteDatabase(name)
        val source = InstrumentationRegistry.getInstrumentation().context.assets
            .open("br.com.openmonetis.companion.data.local.AppDatabase/1.json").bufferedReader().use { it.readText() }
        val schema = JSONObject(source).getJSONObject("database")
        val old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            val tables = schema.getJSONArray("entities")
            for (index in 0 until tables.length()) {
                val table = tables.getJSONObject(index)
                old.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", table.getString("tableName")))
            }
            old.execSQL("INSERT INTO notifications (id,source_app,original_text,notification_timestamp,sync_status,created_at) VALUES ('legacy','test.app','preservar',1,'SYNCED',1)")
            old.version = 1
        } finally { old.close() }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val entity = db.notificationDao().getById("legacy")!!
            assertEquals("preservar", entity.originalText)
            assertEquals(SyncStatus.SYNCED, entity.syncStatus)
            assertNull(entity.syncedAt)
            assertEquals(0, db.notificationDao().countSyncedSince(0))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun filtersBeforeLimitAndSupportsOldPendingRecords() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.insert(notification("old-pending", SyncStatus.PENDING_SYNC, 1))
            dao.insertAll((2..151).map { notification("sent-$it", SyncStatus.SYNCED, it.toLong()) })
            assertEquals("old-pending", dao.observeFiltered(listOf(SyncStatus.PENDING_SYNC), 50).first().single().id)
            assertEquals(51, dao.observeFiltered(listOf(SyncStatus.SYNCED), 51).first().size)
            assertEquals(100, dao.observeFiltered(listOf(SyncStatus.SYNCED), 100).first().size)
            dao.markSynced("old-pending", "server-id", syncedAt = 2_000)
            assertEquals(1, dao.countSyncedSince(1_000))
        } finally { db.close() }
    }

    @Test fun retentionStartsAtSendTimeAndCleanupWaitsForActiveSending() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.insert(notification("old-capture", SyncStatus.PENDING_SYNC, 1))
            dao.markSynced("old-capture", "server-id", syncedAt = 2_000)
            dao.deleteTerminalOlderThan(1_000)
            assertNotNull(dao.getById("old-capture"))
            dao.insert(notification("sending", SyncStatus.SYNCING, 2))
            assertEquals(0, dao.deleteUnlessAnySyncing())
            assertEquals(2, dao.countAll())
            dao.markSynced("sending", "server-id-2")
            assertEquals(2, dao.deleteUnlessAnySyncing())
            assertEquals(0, dao.countAll())
        } finally { db.close() }
    }

    @Test fun sendingCannotBeDeletedOrDiscardedAndUndoNeverOverwrites() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            val original = notification("guarded", SyncStatus.PENDING_SYNC, 1)
            dao.insert(original)
            assertEquals(1, dao.claimForSync(original.id))
            assertEquals(0, dao.deleteUnlessSyncing(original.id))
            assertEquals(0, dao.changeStatusIf(original.id, SyncStatus.PENDING_SYNC, SyncStatus.DISCARDED))
            dao.markSynced(original.id, "server-id")
            dao.restoreDeleted(original)
            assertEquals(SyncStatus.SYNCED, dao.getById(original.id)!!.syncStatus)
            val actualSnapshot = dao.removeWithSnapshot(original.id)!!
            assertEquals(SyncStatus.SYNCED, actualSnapshot.syncStatus)
            dao.restoreDeleted(actualSnapshot)
            assertEquals(SyncStatus.SYNCED, dao.getById(original.id)!!.syncStatus)
            dao.updateStatus(original.id, SyncStatus.PENDING_SYNC)
            assertEquals(1, dao.changeStatusIf(original.id, SyncStatus.PENDING_SYNC, SyncStatus.DISCARDED))
            assertEquals(1, dao.changeStatusIf(original.id, SyncStatus.DISCARDED, SyncStatus.PENDING_SYNC))
        } finally { db.close() }
    }
}
