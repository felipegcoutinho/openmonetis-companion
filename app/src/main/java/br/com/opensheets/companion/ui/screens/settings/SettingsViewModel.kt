package br.com.opensheets.companion.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonitoredAppUi(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean
)

data class SettingsUiState(
    val serverUrl: String = "",
    val tokenName: String = "",
    val isConnected: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val monitoredApps: List<MonitoredAppUi> = emptyList(),
    val appVersion: String = "",
    val showDisconnectDialog: Boolean = false,
    val showClearDataDialog: Boolean = false
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

    // Default banking apps to monitor
    private val defaultApps = listOf(
        AppConfigEntity("com.nu.production", "Nubank", true),
        AppConfigEntity("br.com.intermedium", "Inter", true),
        AppConfigEntity("com.itau", "Itaú", true),
        AppConfigEntity("com.bradesco", "Bradesco", true),
        AppConfigEntity("br.com.bb.android", "Banco do Brasil", true),
        AppConfigEntity("com.santander.app", "Santander", true),
        AppConfigEntity("br.com.gabba.Caixa", "Caixa", true),
        AppConfigEntity("com.picpay", "PicPay", true),
        AppConfigEntity("com.mercadopago.wallet", "Mercado Pago", true),
        AppConfigEntity("com.c6bank.app", "C6 Bank", true),
        AppConfigEntity("br.com.original.bank", "Banco Original", true),
        AppConfigEntity("com.neon", "Neon", true),
        AppConfigEntity("br.com.xpi.investor", "XP Investimentos", true),
        AppConfigEntity("com.btgpactual.app", "BTG Pactual", true),
        AppConfigEntity("br.com.safra.SafraWallet", "Safra", true)
    )

    init {
        loadSettings()
        initializeDefaultApps()
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

    private fun initializeDefaultApps() {
        viewModelScope.launch {
            val existingApps = appConfigDao.getAll()
            if (existingApps.isEmpty()) {
                appConfigDao.insertAll(defaultApps)
            }
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
