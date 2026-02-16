package br.com.openmonetis.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "type")
    val type: SyncLogType,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "notification_id")
    val notificationId: String? = null,

    @ColumnInfo(name = "details")
    val details: String? = null
)

enum class SyncLogType {
    INFO,
    SUCCESS,
    ERROR,
    WARNING
}
