package br.com.openmonetis.companion.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.service.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Home and History share the same queries, mapping and guarded actions.
data class NotificationListState(
    val filter: NotificationFilter = NotificationFilter.PENDING,
    val items: List<NotificationUiItem> = emptyList(),
    val loading: Boolean = true,
    val hasMore: Boolean = false
)
data class NotificationFeedback(val message: String, val undo: (suspend () -> Unit)? = null)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
open class NotificationsViewModel(
    protected val context: Context,
    protected val notificationDao: NotificationDao,
    private val pageSize: Int = 50,
    private val savedState: androidx.lifecycle.SavedStateHandle
) : ViewModel() {
    private val selection = savedState.getStateFlow("notificationFilter", runCatching { NotificationFilter.valueOf(savedState.get<String>("filter") ?: "PENDING") }.getOrDefault(NotificationFilter.PENDING))
    private val limit = MutableStateFlow(pageSize)
    private val mapper = NotificationMapper(context)
    val filterCounts = notificationDao.observeStatusCounts().map { counts ->
        NotificationFilter.entries.associateWith { filter -> counts.filter { it.status in filter.statuses }.sumOf { it.count } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val listState = combine(selection, limit) { filter, size -> filter to size }
        .flatMapLatest { (filter, size) -> notificationDao.observeFiltered(filter.statuses, size + 1)
            .map { entities -> withContext(Dispatchers.IO) {
                NotificationListState(filter, entities.take(size).map(mapper::map), false, entities.size > size)
            } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationListState(filter = selection.value))
    private val feedbackChannel = Channel<NotificationFeedback>(Channel.BUFFERED)
    val feedback = feedbackChannel.receiveAsFlow()
    fun setFilter(filter: NotificationFilter) { limit.value = pageSize; savedState["notificationFilter"] = filter }
    fun loadMore() { limit.value += pageSize }
    fun deleteNotification(item: NotificationUiItem) = viewModelScope.launch {
        val removed = notificationDao.removeWithSnapshot(item.id)
        if (removed != null) {
            feedbackChannel.send(NotificationFeedback("Registro removido deste aparelho. O servidor mantém o lançamento.") {
                notificationDao.restoreDeleted(removed)
            })
        } else feedbackChannel.send(NotificationFeedback("Aguarde o envio terminar para remover este registro."))
    }
    fun retryNotification(id: String) = viewModelScope.launch {
        val entity = notificationDao.getById(id) ?: return@launch
        if (entity.syncStatus != SyncStatus.SYNC_FAILED) return@launch
        if (notificationDao.changeStatusIf(id, SyncStatus.SYNC_FAILED, SyncStatus.PENDING_SYNC) == 1) {
            SyncWorker.enqueue(context)
            feedbackChannel.send(NotificationFeedback("Reenvio solicitado. Aguardando conexão."))
        }
    }
    fun discardNotification(item: NotificationUiItem) = viewModelScope.launch {
        if (item.syncStatus !in listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNC_FAILED)) return@launch
        if (notificationDao.changeStatusIf(item.id, item.syncStatus, SyncStatus.DISCARDED) == 1) {
            feedbackChannel.send(NotificationFeedback("Descartado. Este registro não será enviado.") {
                notificationDao.changeStatusIf(item.id, SyncStatus.DISCARDED, item.syncStatus)
            })
        } else feedbackChannel.send(NotificationFeedback("O envio já começou. Aguarde a conclusão."))
    }
}
