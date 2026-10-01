package br.com.openmonetis.companion.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.openmonetis.companion.data.local.dao.AppConfigDao
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.entities.AppConfigEntity
import br.com.openmonetis.companion.data.remote.DeviceConnectionVerifier
import br.com.openmonetis.companion.util.CompanionQrCode
import br.com.openmonetis.companion.util.NotificationsExporter
import br.com.openmonetis.companion.util.SecureStorage
import br.com.openmonetis.companion.util.ServerUrlPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MonitoredAppUi(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean,
    val icon: Drawable? = null
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
    val disconnected: Boolean = false,
    val lastVerifiedTime: Long = 0,
    val deleteCount: Int = 0,
    val pendingDeleteCount: Int = 0,
    val clearDataError: String? = null,
    val exportedUri: String? = null,
    val monitoredApps: List<MonitoredAppUi> = emptyList(),
    val appVersion: String = "",
    val showDisconnectDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showAddAppDialog: Boolean = false,
    val showEditServerDialog: Boolean = false,
    val editServerUrl: String = "",
    val editToken: String = "",
    val editServerError: String? = null,
    val isSavingServer: Boolean = false,
    val installedApps: List<InstalledAppUi> = emptyList(),
    val appSearchQuery: String = "",
    val isLoadingApps: Boolean = false,
    val isExportingNotifications: Boolean = false,
    val exportMessage: String? = null,
    val notifySyncSuccess: Boolean = true,
    val notifySyncError: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val connectionVerifier: DeviceConnectionVerifier,
    private val appConfigDao: AppConfigDao,
    private val notificationDao: NotificationDao,
    private val notificationsExporter: NotificationsExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var allInstalledApps: List<InstalledAppUi> = emptyList()

    init {
        loadSettings()
        viewModelScope.launch {
            appConfigDao.getAllFlow().collectLatest { loadMonitoredApps() }
        }
        viewModelScope.launch {
            secureStorage.observeConnection().collect { state ->
                _uiState.value = _uiState.value.copy(lastVerifiedTime = state.lastVerifiedTime)
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val serverUrl = secureStorage.serverUrl ?: ""
            val tokenName = secureStorage.tokenName ?: ""
            val appVersion = getAppVersion()

            _uiState.value = _uiState.value.copy(
                serverUrl = serverUrl,
                tokenName = tokenName,
                isConnected = secureStorage.isConfigured(),
                appVersion = appVersion,
                notifySyncSuccess = secureStorage.notifySyncSuccess,
                notifySyncError = secureStorage.notifySyncError
            )

            loadMonitoredApps()
        }
    }

    private suspend fun loadMonitoredApps() {
        val apps = appConfigDao.getAll()
        val pm = context.packageManager
        val uiApps = withContext(Dispatchers.IO) { apps.map { app ->
            val icon = try {
                pm.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
            MonitoredAppUi(
                packageName = app.packageName,
                displayName = app.displayName,
                isEnabled = app.isEnabled,
                icon = icon
            )
        }
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
                
                // Query apps that have a launcher activity (user-visible apps)
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                    .mapNotNull { resolveInfo ->
                        val packageName = resolveInfo.activityInfo.packageName
                        // Exclude already monitored and our own app
                        if (packageName in monitoredPackages || packageName == context.packageName) {
                            null
                        } else {
                            try {
                                val appInfo = pm.getApplicationInfo(packageName, 0)
                                InstalledAppUi(
                                    packageName = packageName,
                                    displayName = pm.getApplicationLabel(appInfo).toString(),
                                    icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.displayName.lowercase() }
            }
            
            allInstalledApps = apps
            _uiState.value = _uiState.value.copy(
                installedApps = apps.filter { it.displayName.contains(_uiState.value.appSearchQuery, true) || it.packageName.contains(_uiState.value.appSearchQuery, true) },
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

    fun addApps(packages: Set<String>) {
        viewModelScope.launch {
            allInstalledApps.filter { it.packageName in packages }.forEach { app ->
                appConfigDao.insert(AppConfigEntity(packageName = app.packageName, displayName = app.displayName))
            }
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showClearDataDialog = true,
                deleteCount = notificationDao.countAll(), pendingDeleteCount = notificationDao.countPending(), clearDataError = null)
        }
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
                disconnected = true,
                showDisconnectDialog = false
            )
        }
    }

    fun showEditServerDialog() {
        _uiState.value = _uiState.value.copy(
            showEditServerDialog = true,
            editServerUrl = _uiState.value.serverUrl,
            editToken = "",
            editServerError = null
        )
    }

    fun hideEditServerDialog() {
        _uiState.value = _uiState.value.copy(
            showEditServerDialog = false,
            editServerUrl = "",
            editToken = "",
            editServerError = null,
            isSavingServer = false
        )
    }

    fun updateEditServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(editServerUrl = url, editServerError = null)
    }

    fun updateEditToken(token: String) {
        _uiState.value = _uiState.value.copy(editToken = token, editServerError = null)
    }

    fun saveServerSettings() {
        if (_uiState.value.isSavingServer) return
        viewModelScope.launch {
            val serverOrigin = ServerUrlPolicy.parse(_uiState.value.editServerUrl)
            if (serverOrigin == null) {
                _uiState.value = _uiState.value.copy(editServerError = "Use uma URL HTTPS válida")
                return@launch
            }

            val normalizedUrl = serverOrigin.toString().removeSuffix("/")
            val previousUrl = secureStorage.serverUrl.orEmpty()
            val previousToken = secureStorage.accessToken.orEmpty()
            val newTokenInput = _uiState.value.editToken.trim()
            val newToken = newTokenInput
                .takeIf(String::isNotEmpty)
                ?.let(CompanionQrCode::extractToken)
            if (newTokenInput.isNotEmpty() && newToken == null) {
                _uiState.value = _uiState.value.copy(editServerError = "Informe um token válido")
                return@launch
            }
            val serverChanged = ServerUrlPolicy.normalize(previousUrl) != normalizedUrl
            if (serverChanged && newToken == null) {
                _uiState.value = _uiState.value.copy(
                    editServerError = "Informe um novo token ao trocar de servidor"
                )
                return@launch
            }

            val token = newToken ?: previousToken
            if (token.isEmpty()) {
                _uiState.value = _uiState.value.copy(editServerError = "Informe o token de acesso")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSavingServer = true, editServerError = null)
            val verified = try { connectionVerifier.verify(serverOrigin, token) } catch (_: java.io.IOException) {
                _uiState.value = _uiState.value.copy(isSavingServer = false,
                    editServerError = "Não foi possível acessar o servidor. Verifique a conexão e tente novamente.")
                return@launch
            }
            if (verified == null) {
                _uiState.value = _uiState.value.copy(
                    isSavingServer = false,
                    editServerError = "Servidor ou token inválido"
                )
                return@launch
            }

            secureStorage.saveCredentials(
                serverUrl = normalizedUrl,
                accessToken = token,
                tokenId = verified.tokenId,
                tokenName = verified.tokenName
            )
            _uiState.value = _uiState.value.copy(
                serverUrl = normalizedUrl,
                tokenName = verified.tokenName.orEmpty(),
                isConnected = true,
                showEditServerDialog = false,
                editServerUrl = "",
                editToken = "",
                editServerError = null,
                isSavingServer = false
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            if (notificationDao.countByStatus(br.com.openmonetis.companion.data.local.entities.SyncStatus.SYNCING) > 0) {
                _uiState.value = _uiState.value.copy(clearDataError = "Há um envio em andamento. Aguarde e tente novamente.")
                return@launch
            }
            val removed = notificationDao.deleteUnlessAnySyncing()
            if (removed == 0 && notificationDao.countAll() > 0) {
                _uiState.value = _uiState.value.copy(clearDataError = "O envio começou. Aguarde e tente novamente.")
                return@launch
            }
            hideClearDataDialog()
        }
    }

    fun exportNotifications() {
        if (_uiState.value.isExportingNotifications) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingNotifications = true)

            try {
                val result = withContext(Dispatchers.IO) {
                    notificationsExporter.exportToDownloads()
                }
                val message = if (result.notificationCount > 0) {
                    "${result.notificationCount} notificações exportadas para Downloads/${result.fileName}"
                } else {
                    "Arquivo criado em Downloads/${result.fileName}, mas não havia notificações salvas"
                }
                _uiState.value = _uiState.value.copy(
                    isExportingNotifications = false,
                    exportMessage = message,
                    exportedUri = result.uri.toString()
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExportingNotifications = false,
                    exportMessage = "Não foi possível exportar as notificações", exportedUri = null
                )
            }
        }
    }

    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }

    fun setNotifySyncSuccess(enabled: Boolean) {
        secureStorage.notifySyncSuccess = enabled
        _uiState.value = _uiState.value.copy(notifySyncSuccess = enabled)
    }

    fun setNotifySyncError(enabled: Boolean) {
        secureStorage.notifySyncError = enabled
        _uiState.value = _uiState.value.copy(notifySyncError = enabled)
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
