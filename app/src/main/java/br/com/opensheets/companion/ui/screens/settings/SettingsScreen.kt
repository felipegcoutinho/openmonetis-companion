package br.com.opensheets.companion.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import br.com.opensheets.companion.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onDisconnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permission status when returning from settings
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissionStatus()
        }
    }

    // Handle disconnect navigation
    LaunchedEffect(uiState.isConnected) {
        if (!uiState.isConnected && uiState.serverUrl.isEmpty()) {
            onDisconnected()
        }
    }

    // Disconnect confirmation dialog
    if (uiState.showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDisconnectDialog,
            title = { Text(stringResource(R.string.settings_disconnect)) },
            text = { Text("Deseja realmente desconectar este dispositivo? Você precisará configurar novamente.") },
            confirmButton = {
                TextButton(onClick = viewModel::disconnect) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDisconnectDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear data confirmation dialog
    if (uiState.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideClearDataDialog,
            title = { Text("Limpar Dados") },
            text = { Text("Isso irá excluir todas as notificações capturadas localmente. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearAllData) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideClearDataDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Section
            item {
                SectionHeader(title = stringResource(R.string.settings_server))
            }

            item {
                ServerCard(
                    serverUrl = uiState.serverUrl,
                    tokenName = uiState.tokenName,
                    isConnected = uiState.isConnected,
                    onDisconnect = viewModel::showDisconnectDialog
                )
            }

            // Notification Permission Section
            item {
                SectionHeader(title = stringResource(R.string.settings_notification_permission))
            }

            item {
                PermissionCard(
                    hasPermission = uiState.hasNotificationPermission,
                    onRequestPermission = {
                        context.startActivity(viewModel.openNotificationSettings())
                    }
                )
            }

            // Monitored Apps Section
            item {
                SectionHeader(title = stringResource(R.string.settings_monitored_apps))
            }

            items(uiState.monitoredApps) { app ->
                AppToggleItem(
                    app = app,
                    onToggle = { enabled -> viewModel.toggleApp(app.packageName, enabled) }
                )
            }

            // Data Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Dados")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    onClick = viewModel::showClearDataDialog
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Limpar Notificações Locais",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Excluir todas as notificações capturadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = stringResource(R.string.settings_about))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_version),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = uiState.appVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ServerCard(
    serverUrl: String,
    tokenName: String,
    isConnected: Boolean,
    onDisconnect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = if (isConnected) {
                            stringResource(R.string.settings_server_connected)
                        } else {
                            stringResource(R.string.settings_server_disconnected)
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (isConnected) {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Desconectar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.settings_server_url),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = serverUrl.ifEmpty { "-" },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_token_name),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = tokenName.ifEmpty { "-" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PermissionCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            }
        ),
        onClick = if (!hasPermission) onRequestPermission else ({})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (hasPermission) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPermission) {
                        stringResource(R.string.settings_permission_granted)
                    } else {
                        stringResource(R.string.settings_grant_permission)
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (hasPermission) {
                        "O app pode capturar notificações financeiras"
                    } else {
                        "Toque para abrir as configurações"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppToggleItem(
    app: MonitoredAppUi,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = app.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
