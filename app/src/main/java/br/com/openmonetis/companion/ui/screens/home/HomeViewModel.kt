package br.com.openmonetis.companion.ui.screens.home

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.openmonetis.companion.data.local.dao.AppConfigDao
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.service.CaptureNotificationListenerService
import br.com.openmonetis.companion.service.SyncWorker
import br.com.openmonetis.companion.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class NotificationUiItem(
    val id: String,
    val appName: String,
    val appIcon: Drawable?,
    val title: String?,
    val text: String,
    val parsedAmount: String?,
    val parsedName: String?,
    val syncStatus: SyncStatus,
    val timestamp: String,
    val timestampFull: String
)

enum class SyncStatusFilter {
    ALL, PENDING, SYNCED, FAILED
}

data class MonitoredAppIcon(
    val packageName: String,
    val displayName: String,
    val icon: Drawable?
)

data class HomeUiState(
    val pendingCount: Int = 0,
    val syncedToday: Int = 0,
    val lastSyncTime: String? = null,
    val hasNotificationPermission: Boolean = false,
    val enabledAppsCount: Int = 0,
    val monitoredApps: List<MonitoredAppIcon> = emptyList(),
    val isRefreshing: Boolean = false,
    // History
    val notifications: List<NotificationUiItem> = emptyList(),
    val selectedFilter: SyncStatusFilter = SyncStatusFilter.ALL,
    val isLoadingNotifications: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val appConfigDao: AppConfigDao,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    private val dateFormatFull = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    // Cache for app icons to avoid loading during scroll
    private val iconCache = mutableMapOf<String, Drawable?>()

    init {
        loadStats()
        loadNotifications()
        checkNotificationPermission()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Count pending notifications
            val pendingCount = notificationDao.countPending()

            // Count synced today
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val syncedToday = notificationDao.countSyncedSince(todayStart)

            // Get enabled apps with icons
            val enabledApps = appConfigDao.getEnabled()
            val pm = context.packageManager
            val appsWithIcons = enabledApps.map { app ->
                val icon = try {
                    pm.getApplicationIcon(app.packageName)
                } catch (e: Exception) {
                    null
                }
                MonitoredAppIcon(
                    packageName = app.packageName,
                    displayName = app.displayName,
                    icon = icon
                )
            }

            // Get last sync time
            val lastSyncTime = secureStorage.lastSyncTime
            val lastSyncTimeFormatted = if (lastSyncTime > 0) {
                dateFormatFull.format(Date(lastSyncTime))
            } else {
                null
            }

            _uiState.value = _uiState.value.copy(
                pendingCount = pendingCount,
                syncedToday = syncedToday,
                enabledAppsCount = enabledApps.size,
                monitoredApps = appsWithIcons,
                lastSyncTime = lastSyncTimeFormatted
            )
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingNotifications = true)

            val notifications = notificationDao.getRecent(100)
            val filteredNotifications = filterNotifications(notifications, _uiState.value.selectedFilter)

            // Pre-load all icons on IO thread
            val uiItems = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                filteredNotifications.map { entity ->
                    val icon = iconCache.getOrPut(entity.sourceApp) {
                        try {
                            pm.getApplicationIcon(entity.sourceApp)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    entity.toUiItem(icon)
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoadingNotifications = false,
                notifications = uiItems
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
            loadStats()
        }
    }

    private fun filterNotifications(
        notifications: List<NotificationEntity>,
        filter: SyncStatusFilter
    ): List<NotificationEntity> {
        return when (filter) {
            SyncStatusFilter.ALL -> notifications
            SyncStatusFilter.PENDING -> notifications.filter {
                it.syncStatus == SyncStatus.PENDING_SYNC || it.syncStatus == SyncStatus.SYNCING
            }
            SyncStatusFilter.SYNCED -> notifications.filter {
                it.syncStatus == SyncStatus.SYNCED || it.syncStatus == SyncStatus.PROCESSED
            }
            SyncStatusFilter.FAILED -> notifications.filter {
                it.syncStatus == SyncStatus.SYNC_FAILED
            }
        }
    }

    private fun NotificationEntity.toUiItem(icon: Drawable?): NotificationUiItem {
        return NotificationUiItem(
            id = id,
            appName = sourceAppName ?: sourceApp,
            appIcon = icon,
            title = originalTitle,
            text = originalText,
            parsedAmount = parsedAmount?.let { "R$ %.2f".format(it) },
            parsedName = parsedName,
            syncStatus = syncStatus,
            timestamp = dateFormat.format(Date(createdAt)),
            timestampFull = dateFormatFull.format(Date(createdAt))
        )
    }

    private fun checkNotificationPermission() {
        val hasPermission = isNotificationListenerEnabled()
        _uiState.value = _uiState.value.copy(hasNotificationPermission = hasPermission)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(context, CaptureNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    fun requestNotificationPermission() {
        // This should open the notification listener settings
        // The actual navigation should be handled by the UI
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            loadStats()
            loadNotifications()
            checkNotificationPermission()

            // Trigger a sync
            SyncWorker.enqueue(context)

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
}
