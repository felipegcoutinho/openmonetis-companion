package br.com.opensheets.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.opensheets.companion.data.local.entities.NotificationEntity
import br.com.opensheets.companion.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM notifications ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE sync_status = :status ORDER BY created_at ASC")
    suspend fun getByStatus(status: SyncStatus): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE sync_status IN (:statuses) ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingSync(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED),
        limit: Int = 50
    ): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE sync_status IN (:statuses)")
    suspend fun countPending(
        statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED)
    ): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE created_at >= :since")
    suspend fun countSince(since: Long): Int

    @Query("UPDATE notifications SET sync_status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus)

    @Query("UPDATE notifications SET sync_status = :status, server_item_id = :serverId WHERE id = :id")
    suspend fun markSynced(id: String, serverId: String, status: SyncStatus = SyncStatus.SYNCED)

    @Query("UPDATE notifications SET sync_status = :status, sync_error = :error WHERE id = :id")
    suspend fun markSyncFailed(id: String, error: String?, status: SyncStatus = SyncStatus.SYNC_FAILED)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notifications WHERE created_at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}
