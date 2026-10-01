package br.com.openmonetis.companion.ui.notifications

import android.content.Context
import android.graphics.drawable.Drawable
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.ui.components.CapturedNotificationDetails
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val brazil = Locale.forLanguageTag("pt-BR")
fun formatDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", brazil))
fun formatAmount(amount: Double): String = NumberFormat.getCurrencyInstance(brazil).format(amount)
fun formatRecentDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val day = when (date.toLocalDate()) {
        LocalDate.now() -> "Hoje"
        LocalDate.now().minusDays(1) -> "Ontem"
        else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", brazil))
    }
    return "$day · ${date.format(DateTimeFormatter.ofPattern("HH:mm", brazil))}"
}

enum class NotificationFilter(val label: String, val statuses: List<SyncStatus>) {
    PENDING("Pendentes", listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNCING)),
    FAILED("Com erro", listOf(SyncStatus.SYNC_FAILED)),
    SENT("Enviados", listOf(SyncStatus.SYNCED, SyncStatus.PROCESSED)),
    DISCARDED("Descartados", listOf(SyncStatus.DISCARDED))
}

fun SyncStatus.userLabel() = when (this) {
    SyncStatus.PENDING_SYNC -> "Pendente"
    SyncStatus.SYNCING -> "Enviando"
    SyncStatus.SYNC_FAILED -> "Falha no envio"
    SyncStatus.SYNCED, SyncStatus.PROCESSED -> "Enviado"
    SyncStatus.DISCARDED -> "Descartado"
}

data class NotificationUiItem(val entity: NotificationEntity, val appIcon: Drawable?) {
    val id get() = entity.id
    val appName get() = entity.sourceAppName ?: entity.sourceApp
    val title get() = entity.originalTitle
    val text get() = entity.originalText
    val parsedAmount get() = entity.parsedAmount?.let(::formatAmount)
    val parsedName get() = entity.parsedName
    val syncStatus get() = entity.syncStatus
    val syncError get() = entity.syncError
    val timestamp get() = formatRecentDate(entity.notificationTimestamp)
    val timestampFull get() = formatDate(entity.notificationTimestamp)
    fun details() = CapturedNotificationDetails(appName, title, text, parsedAmount, parsedName,
        syncStatus, timestampFull, syncError)
}

class NotificationMapper(private val context: Context) {
    private val icons = mutableMapOf<String, Drawable?>()
    fun map(entity: NotificationEntity) = NotificationUiItem(entity, icons.getOrPut(entity.sourceApp) {
        runCatching { context.packageManager.getApplicationIcon(entity.sourceApp) }.getOrNull()
    })
}
