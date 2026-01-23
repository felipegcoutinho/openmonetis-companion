package br.com.opensheets.companion.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.entities.AppConfigEntity
import br.com.opensheets.companion.service.CaptureNotificationListenerService
import br.com.opensheets.companion.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MonitoredAppUi(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean
)

data class InstalledAppUi(
    val packageName: String,
    val displayName: String,
    val icon: Drawable?
)

data class SettingsUiState(
    val serverUrl: String = "",
    val tokenName: String = "",
    val isConnected: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val monitoredApps: List<MonitoredAppUi> = emptyList(),
    val appVersion: String = "",
    val showDisconnectDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showAddAppDialog: Boolean = false,
    val installedApps: List<InstalledAppUi> = emptyList(),
    val appSearchQuery: String = "",
    val isLoadingApps: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val appConfigDao: AppConfigDao,
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var allInstalledApps: List<InstalledAppUi> = emptyList()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val serverUrl = secureStorage.serverUrl ?: ""
            val tokenName = secureStorage.tokenName ?: ""
            val hasToken = secureStorage.accessToken != null
            val hasPermission = isNotificationListenerEnabled()
            val appVersion = getAppVersion()

            _uiState.value = _uiState.value.copy(
                serverUrl = serverUrl,
                tokenName = tokenName,
                isConnected = hasToken && serverUrl.isNotEmpty(),
                hasNotificationPermission = hasPermission,
                appVersion = appVersion
            )

            loadMonitoredApps()
        }
    }

    private suspend fun loadMonitoredApps() {
        val apps = appConfigDao.getAll()
        val uiApps = apps.map { app ->
            MonitoredAppUi(
                packageName = app.packageName,
                displayName = app.displayName,
                isEnabled = app.isEnabled
            )
        }
        _uiState.value = _uiState.value.copy(monitoredApps = uiApps)
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            appConfigDao.setEnabled(packageName, enabled)
            loadMonitoredApps()
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            appConfigDao.delete(packageName)
            loadMonitoredApps()
        }
    }

    fun showAddAppDialog() {
        _uiState.value = _uiState.value.copy(
            showAddAppDialog = true,
            appSearchQuery = "",
            isLoadingApps = true
        )
        loadInstalledApps()
    }

    fun hideAddAppDialog() {
        _uiState.value = _uiState.value.copy(
            showAddAppDialog = false,
            appSearchQuery = "",
            installedApps = emptyList()
        )
    }

    fun updateAppSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(appSearchQuery = query)
        filterInstalledApps(query)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val monitoredPackages = appConfigDao.getAll().map { it.packageName }.toSet()
                
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { appInfo ->
                        // Exclude already monitored apps
                        appInfo.packageName !in monitoredPackages &&
                        // Exclude our own app
                        appInfo.packageName != context.packageName &&
                        // Show user apps OR updated system apps (like banking apps)
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                         appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
                    }
                    .map { appInfo ->
                        InstalledAppUi(
                            packageName = appInfo.packageName,
                            displayName = pm.getApplicationLabel(appInfo).toString(),
                            icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                        )
                    }
                    .sortedBy { it.displayName.lowercase() }
            }
            
            allInstalledApps = apps
            _uiState.value = _uiState.value.copy(
                installedApps = apps,
                isLoadingApps = false
            )
        }
    }

    private fun filterInstalledApps(query: String) {
        val filtered = if (query.isBlank()) {
            allInstalledApps
        } else {
            allInstalledApps.filter { app ->
                app.displayName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(installedApps = filtered)
    }

    fun addApp(packageName: String, displayName: String) {
        viewModelScope.launch {
            val config = AppConfigEntity(
                packageName = packageName,
                displayName = displayName,
                isEnabled = true
            )
            appConfigDao.insert(config)
            loadMonitoredApps()
            hideAddAppDialog()
        }
    }

    fun showDisconnectDialog() {
        _uiState.value = _uiState.value.copy(showDisconnectDialog = true)
    }

    fun hideDisconnectDialog() {
        _uiState.value = _uiState.value.copy(showDisconnectDialog = false)
    }

    fun showClearDataDialog() {
        _uiState.value = _uiState.value.copy(showClearDataDialog = true)
    }

    fun hideClearDataDialog() {
        _uiState.value = _uiState.value.copy(showClearDataDialog = false)
    }

    fun disconnect() {
        viewModelScope.launch {
            secureStorage.clear()
            _uiState.value = _uiState.value.copy(
                serverUrl = "",
                tokenName = "",
                isConnected = false,
                showDisconnectDialog = false
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            notificationDao.deleteAll()
            hideClearDataDialog()
        }
    }

    fun openNotificationSettings(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun refreshPermissionStatus() {
        _uiState.value = _uiState.value.copy(
            hasNotificationPermission = isNotificationListenerEnabled()
        )
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(context, CaptureNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}
