package br.com.openmonetis.companion.ui.notifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.ui.components.CapturedNotificationDetailsDialog
import br.com.openmonetis.companion.ui.components.OpenMonetisTextButton
import br.com.openmonetis.companion.ui.theme.success
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.foundation.Image

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationFilters(selected: NotificationFilter, onSelect: (NotificationFilter) -> Unit, counts: Map<NotificationFilter, Int> = emptyMap()) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 160.dp * LocalDensity.current.fontScale * 2 + 8.dp) 2 else 1
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = columns
        ) {
            NotificationFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelect(filter) },
                    label = {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(filter.label, Modifier.weight(1f))
                            counts[filter]?.let { Text(it.toString(), style = MaterialTheme.typography.labelMedium) }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationUiItem, onClick: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.appIcon?.let { Image(rememberDrawablePainter(it), null, Modifier.size(32.dp)); Spacer(Modifier.width(12.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(item.appName, style = MaterialTheme.typography.labelLarge)
                    Text(item.timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, enabled = item.syncStatus != SyncStatus.SYNCING) {
                    Icon(Icons.Default.DeleteOutline, "Remover registro de ${item.appName}")
                }
            }
            Text(item.parsedName ?: item.title ?: "Notificação capturada", style = MaterialTheme.typography.titleMedium)
            item.parsedAmount?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
            Text(item.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            val color = when (item.syncStatus) {
                SyncStatus.SYNCED, SyncStatus.PROCESSED -> MaterialTheme.colorScheme.success
                SyncStatus.SYNC_FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(item.syncStatus.userLabel(), style = MaterialTheme.typography.labelLarge, color = color)
            item.syncError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun NotificationEmpty(filter: NotificationFilter) {
    val message = when (filter) {
        NotificationFilter.PENDING -> "Nenhuma pendência. Novas notificações dos apps monitorados aparecerão aqui."
        NotificationFilter.FAILED -> "Nenhum erro de envio. Se ocorrer uma falha, você poderá consultar e reenviar aqui."
        NotificationFilter.SENT -> "Ainda não há envios. Ative o acesso às notificações e selecione os apps para começar."
        NotificationFilter.DISCARDED -> "Nenhum registro descartado. Descartar interrompe o envio e mantém uma cópia local."
    }
    OutlinedCard(Modifier.fillMaxWidth()) { Text(message, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun NotificationDialogsAndFeedback(viewModel: NotificationsViewModel, items: List<NotificationUiItem>,
    selectedId: String?, onDismiss: () -> Unit, snackbar: SnackbarHostState, onEditConnection: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            val result = snackbar.showSnackbar(feedback.message,
                actionLabel = if (feedback.undo != null) "Desfazer" else null,
                withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) feedback.undo?.invoke()
        }
    }
    val selected = items.firstOrNull { it.id == selectedId }
    selected?.let { item ->
        CapturedNotificationDetailsDialog(item.details(), onDismiss,
            onCopyOriginalText = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Notificação", item.text))
            },
            onRetry = if (item.syncStatus == SyncStatus.SYNC_FAILED) ({
                if (item.syncError?.contains("Token", true) == true) onEditConnection() else viewModel.retryNotification(item.id)
                onDismiss()
            }) else null,
            onDiscard = if (item.syncStatus in listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED)) ({ viewModel.discardNotification(item); onDismiss() }) else null,
            retryLabel = if (item.syncError?.contains("Token", true) == true) "Atualizar token" else "Reenviar")
    }
}

@Composable
fun ConfirmNotificationDeletion(item: NotificationUiItem?, onDismiss: () -> Unit, onConfirm: (NotificationUiItem) -> Unit) {
    if (item == null) return
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Remover registro local?") },
        text = { Text(if (item.syncStatus in listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED))
            "Esta notificação ainda não foi enviada. Ao remover, ela deixa de ser sincronizada. Você poderá desfazer logo após a remoção."
            else "A cópia será removida deste aparelho. O lançamento no OpenMonetis permanece salvo. Você poderá desfazer logo após a remoção.") },
        confirmButton = { OpenMonetisTextButton(onClick = { onConfirm(item); onDismiss() }) { Text("Remover", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { OpenMonetisTextButton(onClick = onDismiss) { Text("Cancelar") } })
}
