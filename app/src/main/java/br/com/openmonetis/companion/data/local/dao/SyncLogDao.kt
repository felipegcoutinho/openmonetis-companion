package br.com.openmonetis.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.openmonetis.companion.data.local.entities.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {

    @Insert
    suspend fun insert(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<SyncLogEntity>

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 100): Flow<List<SyncLogEntity>>

    @Query("SELECT COUNT(*) FROM sync_logs")
    suspend fun count(): Int

    @Query("DELETE FROM sync_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM sync_logs")
    suspend fun deleteAll()
}
