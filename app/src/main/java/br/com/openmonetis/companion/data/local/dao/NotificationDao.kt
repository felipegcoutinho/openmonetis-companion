package br.com.openmonetis.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

data class NotificationStatusCount(val status: SyncStatus, val count: Int)

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE sync_status = :status ORDER BY created_at ASC")
    suspend fun getByStatus(status: SyncStatus): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE sync_status IN (:statuses) ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingSync(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED),
        limit: Int = 50
    ): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE sync_status IN ('PENDING_SYNC', 'SYNC_FAILED') AND created_at <= :cutoff AND (created_at > :afterTime OR (created_at = :afterTime AND id > :afterId)) ORDER BY created_at ASC, id ASC LIMIT :limit")
    suspend fun getSyncBatch(cutoff: Long, afterTime: Long, afterId: String, limit: Int): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status IN (:statuses)")
    suspend fun countPending(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED)
    ): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE created_at >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status = :status AND synced_at >= :since")
    suspend fun countSyncedSince(since: Long, status: SyncStatus = SyncStatus.SYNCED): Int

    @Query("UPDATE notifications SET sync_status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus)

    @Query("UPDATE notifications SET sync_status = :status, sync_error = NULL WHERE id = :id")
    suspend fun retrySync(id: String, status: SyncStatus = SyncStatus.PENDING_SYNC)

    @Query("UPDATE notifications SET sync_status = :status, server_item_id = :serverId, synced_at = :syncedAt, sync_error = NULL WHERE id = :id")
    suspend fun markSynced(id: String, serverId: String, status: SyncStatus = SyncStatus.SYNCED, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET sync_status = :status, sync_error = :error WHERE id = :id")
    suspend fun markSyncFailed(id: String, error: String?, status: SyncStatus = SyncStatus.SYNC_FAILED)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notifications WHERE COALESCE(synced_at, created_at) < :before AND sync_status IN (:statuses)")
    suspend fun deleteTerminalOlderThan(
        before: Long,
        statuses: List<SyncStatus> = listOf(
            SyncStatus.SYNCED,
            SyncStatus.PROCESSED,
            SyncStatus.DISCARDED
        )
    )

    @Query("SELECT * FROM notifications WHERE sync_status IN (:statuses) ORDER BY created_at DESC, id DESC LIMIT :limit")
    fun observeFiltered(statuses: List<SyncStatus>, limit: Int): Flow<List<NotificationEntity>>

    @Query("SELECT sync_status AS status, COUNT(*) AS count FROM notifications GROUP BY sync_status")
    fun observeStatusCounts(): Flow<List<NotificationStatusCount>>

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status IN (:statuses)")
    fun observeCount(statuses: List<SyncStatus>): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status = 'SYNCED' AND synced_at >= :since")
    fun observeSyncedSince(since: Long): Flow<Int>

    @Query("UPDATE notifications SET sync_status = 'SYNCING' WHERE id = :id AND sync_status IN ('PENDING_SYNC', 'SYNC_FAILED')")
    suspend fun claimForSync(id: String): Int

    @Query("UPDATE notifications SET sync_status = 'PENDING_SYNC' WHERE sync_status = 'SYNCING'")
    suspend fun recoverInterruptedSync()

    @Transaction
    suspend fun removeWithSnapshot(id: String): NotificationEntity? {
        val current = getById(id) ?: return null
        if (current.syncStatus == SyncStatus.SYNCING) return null
        return if (deleteUnlessSyncing(id) == 1) current else null
    }

    @Query("DELETE FROM notifications WHERE id = :id AND sync_status != 'SYNCING'")
    suspend fun deleteUnlessSyncing(id: String): Int

    @Query("UPDATE notifications SET sync_status = :status WHERE id = :id AND sync_status = :expected")
    suspend fun changeStatusIf(id: String, expected: SyncStatus, status: SyncStatus): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreDeleted(notification: NotificationEntity): Long

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun countAll(): Int

    @Query("DELETE FROM notifications WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE sync_status = 'SYNCING')")
    suspend fun deleteUnlessAnySyncing(): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}
