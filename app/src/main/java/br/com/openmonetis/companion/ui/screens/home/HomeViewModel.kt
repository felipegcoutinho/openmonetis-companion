package br.com.openmonetis.companion.ui.screens.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.openmonetis.companion.data.local.dao.AppConfigDao
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.service.CaptureNotificationListenerService
import br.com.openmonetis.companion.service.SyncWorker
import br.com.openmonetis.companion.ui.notifications.NotificationsViewModel
import br.com.openmonetis.companion.ui.notifications.formatDate
import br.com.openmonetis.companion.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class MonitoredAppIcon(val packageName: String, val displayName: String, val icon: Drawable?)
data class HomeUiState(
    val pendingCount: Int = 0, val failedCount: Int = 0, val syncedToday: Int = 0,
    val lastSyncTime: String? = null, val lastVerifiedTime: String? = null,
    val hasNotificationPermission: Boolean = false, val configured: Boolean = false,
    val monitoredApps: List<MonitoredAppIcon> = emptyList(),
    val syncState: WorkInfo.State? = null, val syncError: String? = null, val sentInRun: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    notificationDao: NotificationDao,
    private val appConfigDao: AppConfigDao,
    private val secureStorage: SecureStorage,
    savedState: SavedStateHandle
) : NotificationsViewModel(context, notificationDao, 8, savedState) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            notificationDao.observeCount(listOf(SyncStatus.PENDING_SYNC, SyncStatus.SYNCING)).collect { count ->
                _uiState.update { it.copy(pendingCount = count) }
            }
        }
        viewModelScope.launch {
            notificationDao.observeCount(listOf(SyncStatus.SYNC_FAILED)).collect { count ->
                _uiState.update { it.copy(failedCount = count) }
            }
        }
        viewModelScope.launch {
            flow { while (true) {
                emit(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                delay(60_000)
            } }.distinctUntilChanged().collectLatest { start ->
                notificationDao.observeSyncedSince(start).collect { count -> _uiState.update { it.copy(syncedToday = count) } }
            }
        }
        viewModelScope.launch {
            appConfigDao.getAllFlow().collectLatest { apps ->
                val mapped = withContext(Dispatchers.IO) { apps.filter { it.isEnabled }.map { app ->
                    MonitoredAppIcon(app.packageName, app.displayName,
                        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull())
                } }
                _uiState.update { it.copy(monitoredApps = mapped) }
            }
        }
        viewModelScope.launch {
            secureStorage.observeConnection().collect { state -> _uiState.update { it.copy(
                configured = state.configured,
                lastSyncTime = state.lastSyncTime.takeIf { time -> time > 0 }?.let(::formatDate),
                lastVerifiedTime = state.lastVerifiedTime.takeIf { time -> time > 0 }?.let(::formatDate)) } }
        }
        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME).collect { works ->
                val work = works.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    ?: works.lastOrNull { !it.state.isFinished } ?: works.maxByOrNull { it.tags.firstOrNull { tag -> tag.startsWith("requested_at:") }?.substringAfter(":")?.toLongOrNull() ?: 0L }
                _uiState.update { it.copy(syncState = work?.state,
                    syncError = work?.outputData?.getString("error"),
                    sentInRun = work?.progress?.getInt("sent", 0) ?: 0) }
            }
        }
        refreshPermissionStatus()
    }
    fun refreshPermissionStatus() {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val component = ComponentName(context, CaptureNotificationListenerService::class.java)
        _uiState.update { it.copy(hasNotificationPermission = enabled?.split(':')?.any { name -> ComponentName.unflattenFromString(name) == component } == true) }
    }
    fun openNotificationSettings() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    fun refreshData() { refreshPermissionStatus(); SyncWorker.enqueue(context) }
}
