package br.com.opensheets.companion.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.entities.NotificationEntity
import br.com.opensheets.companion.data.local.entities.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class NotificationUiItem(
    val id: String,
    val appName: String,
    val title: String?,
    val text: String,
    val parsedAmount: String?,
    val parsedName: String?,
    val syncStatus: SyncStatus,
    val timestamp: String,
    val timestampFull: String
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationUiItem> = emptyList(),
    val isEmpty: Boolean = false,
    val selectedFilter: SyncStatusFilter = SyncStatusFilter.ALL
)

enum class SyncStatusFilter {
    ALL, PENDING, SYNCED, FAILED
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    private val dateFormatFull = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val notifications = notificationDao.getRecent(100)
            val filteredNotifications = filterNotifications(notifications, _uiState.value.selectedFilter)
            val uiItems = filteredNotifications.map { it.toUiItem() }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                notifications = uiItems,
                isEmpty = uiItems.isEmpty()
            )
        }
    }

    fun setFilter(filter: SyncStatusFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadNotifications()
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            notificationDao.delete(id)
            loadNotifications()
        }
    }

    private fun filterNotifications(
        notifications: List<NotificationEntity>,
        filter: SyncStatusFilter
    ): List<NotificationEntity> {
        return when (filter) {
            SyncStatusFilter.ALL -> notifications
            SyncStatusFilter.PENDING -> notifications.filter {
                it.syncStatus == SyncStatus.PENDING_SYNC
            }
            SyncStatusFilter.SYNCED -> notifications.filter {
                it.syncStatus == SyncStatus.SYNCED
            }
            SyncStatusFilter.FAILED -> notifications.filter {
                it.syncStatus == SyncStatus.SYNC_FAILED
            }
        }
    }

    private fun NotificationEntity.toUiItem(): NotificationUiItem {
        return NotificationUiItem(
            id = id,
            appName = sourceAppName ?: sourceApp,
            title = originalTitle,
            text = originalText,
            parsedAmount = parsedAmount?.let { "R$ %.2f".format(it) },
            parsedName = parsedName,
            syncStatus = syncStatus,
            timestamp = dateFormat.format(Date(createdAt)),
            timestampFull = dateFormatFull.format(Date(createdAt))
        )
    }
}
