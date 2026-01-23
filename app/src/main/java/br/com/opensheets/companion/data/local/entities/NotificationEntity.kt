package br.com.opensheets.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "source_app")
    val sourceApp: String,

    @ColumnInfo(name = "source_app_name")
    val sourceAppName: String?,

    @ColumnInfo(name = "original_title")
    val originalTitle: String?,

    @ColumnInfo(name = "original_text")
    val originalText: String,

    @ColumnInfo(name = "notification_timestamp")
    val notificationTimestamp: Long,

    @ColumnInfo(name = "parsed_name")
    val parsedName: String?,

    @ColumnInfo(name = "parsed_amount")
    val parsedAmount: Double?,

    @ColumnInfo(name = "parsed_date")
    val parsedDate: Long?,

    @ColumnInfo(name = "parsed_card_last_digits")
    val parsedCardLastDigits: String?,

    @ColumnInfo(name = "parsed_transaction_type")
    val parsedTransactionType: String?,

    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,

    @ColumnInfo(name = "server_item_id")
    val serverItemId: String? = null,

    @ColumnInfo(name = "sync_error")
    val syncError: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    PENDING_SYNC,
    SYNCING,
    SYNCED,
    SYNC_FAILED,
    PROCESSED,
    DISCARDED
}
