package br.com.opensheets.companion.ui.screens.home

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.entities.SyncStatus
import br.com.opensheets.companion.service.CaptureNotificationListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val pendingCount: Int = 0,
    val syncedToday: Int = 0,
    val lastSyncTime: String? = null,
    val hasNotificationPermission: Boolean = false,
    val enabledAppsCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val appConfigDao: AppConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStats()
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
            val syncedToday = notificationDao.countSince(todayStart)

            // Get enabled apps count
            val enabledApps = appConfigDao.getEnabled()

            _uiState.value = _uiState.value.copy(
                pendingCount = pendingCount,
                syncedToday = syncedToday,
                enabledAppsCount = enabledApps.size
            )
        }
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

    fun refreshStats() {
        loadStats()
        checkNotificationPermission()
    }
}
