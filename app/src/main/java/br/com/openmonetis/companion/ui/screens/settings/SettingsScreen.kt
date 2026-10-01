package br.com.openmonetis.companion.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import br.com.openmonetis.companion.ui.components.OpenMonetisOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import br.com.openmonetis.companion.ui.components.OpenMonetisTextButton as TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import br.com.openmonetis.companion.R
import br.com.openmonetis.companion.ui.components.OpenMonetisDefaults
import br.com.openmonetis.companion.ui.components.OpenMonetisLogo
import br.com.openmonetis.companion.ui.theme.success
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToKeywords: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onDisconnected: () -> Unit,
    openConnection: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var openedConnection by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openConnection, uiState.isConnected) {
        if (openConnection && uiState.isConnected && !openedConnection) {
            openedConnection = true
            viewModel.showEditServerDialog()
        }
    }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var pendingAlertPreference by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingAlertPreference?.invoke(granted)
        pendingAlertPreference = null
        if (!granted) {
            Toast.makeText(
                context,
                "Permissão necessária para exibir alertas do Companion",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun updateAlertPreference(
        enabled: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onCheckedChange(enabled)
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onCheckedChange(true)
        } else {
            pendingAlertPreference = onCheckedChange
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle disconnect navigation
    LaunchedEffect(uiState.disconnected) {
        if (uiState.disconnected) {
            onDisconnected()
        }
    }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let { message ->
            val uri = uiState.exportedUri
            val result = snackbar.showSnackbar(message, if (uri != null) "Compartilhar" else null,
                withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed && uri != null) {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(uri))
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar notificações"))
            }
            viewModel.clearExportMessage()
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
            text = { Text(uiState.clearDataError ?: "Remover ${uiState.deleteCount} registros deste aparelho, incluindo ${uiState.pendingDeleteCount} pendências? Os lançamentos no servidor permanecem salvos. Esta limpeza não pode ser desfeita. Exporte uma cópia antes de continuar.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearAllData) {
                    Text("Excluir notificações", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideClearDataDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add app dialog
    if (uiState.showAddAppDialog) {
        AddAppDialog(
            installedApps = uiState.installedApps,
            searchQuery = uiState.appSearchQuery,
            isLoading = uiState.isLoadingApps,
            onSearchQueryChange = viewModel::updateAppSearchQuery,
            onAppsSelected = viewModel::addApps,
            onDismiss = viewModel::hideAddAppDialog
        )
    }

    // Edit server dialog
    if (uiState.showEditServerDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSavingServer) viewModel.hideEditServerDialog()
            },
            title = { Text("Editar Servidor") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        colors = OpenMonetisDefaults.textFieldColors(),
                        enabled = !uiState.isSavingServer,
                        value = uiState.editServerUrl,
                        onValueChange = viewModel::updateEditServerUrl,
                        label = { Text("URL do Servidor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.editServerError != null
                    )
                    OutlinedTextField(
                        colors = OpenMonetisDefaults.textFieldColors(),
                        enabled = !uiState.isSavingServer,
                        value = uiState.editToken,
                        onValueChange = viewModel::updateEditToken,
                        label = { Text("Novo token (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = uiState.editServerError != null
                    )
                    uiState.editServerError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::saveServerSettings,
                    enabled = !uiState.isSavingServer
                ) {
                    if (uiState.isSavingServer) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Salvar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::hideEditServerDialog,
                    enabled = !uiState.isSavingServer
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
            // Monitored Apps Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_monitored_apps), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = viewModel::showAddAppDialog
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar")
                    }
                }
            }

            if (uiState.monitoredApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "Nenhum app configurado. Toque em \"Adicionar\" para selecionar apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(uiState.monitoredApps) { app ->
                AppToggleItem(
                    app = app,
                    onToggle = { enabled -> viewModel.toggleApp(app.packageName, enabled) },
                    onRemove = { viewModel.removeApp(app.packageName) }
                )
            }

            // Server Section
            item {
                SectionHeader(title = stringResource(R.string.settings_server))
            }

            item {
                ServerCard(
                    serverUrl = uiState.serverUrl,
                    tokenName = uiState.tokenName,
                    isConnected = uiState.isConnected,
                    lastVerifiedTime = uiState.lastVerifiedTime,
                    onEdit = viewModel::showEditServerDialog,
                    onDisconnect = viewModel::showDisconnectDialog
                )
            }

            item {
                SectionHeader(title = "Alertas do Companion")
            }

            item {
                NotificationPreferenceItem(
                    title = "Confirmar envio com notificação",
                    subtitle = "Avisa no telefone quando um lançamento for enviado com sucesso",
                    checked = uiState.notifySyncSuccess,
                    onCheckedChange = { enabled ->
                        updateAlertPreference(enabled, viewModel::setNotifySyncSuccess)
                    }
                )
            }

            item {
                NotificationPreferenceItem(
                    title = "Avisar erro de envio",
                    subtitle = "Mostra uma notificação quando houver falha ao enviar um lançamento",
                    checked = uiState.notifySyncError,
                    onCheckedChange = { enabled ->
                        updateAlertPreference(enabled, viewModel::setNotifySyncError)
                    }
                )
            }

            item {
                SectionHeader("Captura e diagnóstico")
                OutlinedButton(onClick = onNavigateToKeywords, modifier = Modifier.fillMaxWidth()) { Text("Gatilhos de captura") }
                OutlinedButton(onClick = onNavigateToLogs, modifier = Modifier.fillMaxWidth()) { Text("Logs de diagnóstico") }
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    onClick = viewModel::exportNotifications,
                    enabled = !uiState.isExportingNotifications
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isExportingNotifications) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exportar Notificações",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (uiState.isExportingNotifications) {
                                    "Gerando arquivo JSON em Downloads..."
                                } else {
                                    "Salvar notificações capturadas em um arquivo JSON"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                AboutCard(
                    appVersion = uiState.appVersion,
                    onOpenCompanion = {
                        uriHandler.openUri("https://github.com/felipegcoutinho/openmonetis-companion")
                    },
                    onOpenOpenMonetis = {
                        uriHandler.openUri("https://github.com/felipegcoutinho/openmonetis")
                    },
                    onOpenAuthor = {
                        uriHandler.openUri("https://github.com/felipegcoutinho")
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AboutCard(
    appVersion: String,
    onOpenCompanion: () -> Unit,
    onOpenOpenMonetis: () -> Unit,
    onOpenAuthor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            OpenMonetisLogo(modifier = Modifier.padding(16.dp))
            HorizontalDivider()
            AboutRow(
                title = stringResource(R.string.settings_version),
                value = appVersion
            )
            HorizontalDivider()
            AboutRow(
                title = "Código-fonte do Companion",
                value = "felipegcoutinho/openmonetis-companion",
                onClick = onOpenCompanion
            )
            HorizontalDivider()
            AboutRow(
                title = "Projeto principal",
                value = "felipegcoutinho/openmonetis",
                onClick = onOpenOpenMonetis
            )
            HorizontalDivider()
            AboutRow(
                title = "Desenvolvido por",
                value = "felipegcoutinho",
                onClick = onOpenAuthor
            )
        }
    }
}

@Composable
private fun AboutRow(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Abrir $title",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun NotificationPreferenceItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                modifier = Modifier.semantics { contentDescription = title },
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun AddAppDialog(
    installedApps: List<InstalledAppUi>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onAppsSelected: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    fun toggle(packageName: String) { selected = if (packageName in selected) selected - packageName else selected + packageName }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Selecionar apps")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.55f).dp)
            ) {
                OutlinedTextField(
                    colors = OpenMonetisDefaults.textFieldColors(),
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar app") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    installedApps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Nenhum app encontrado" else "Nenhum app disponível",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(installedApps) { app ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    onClick = { toggle(app.packageName) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Checkbox(checked = app.packageName in selected, onCheckedChange = { toggle(app.packageName) })
                                        app.icon?.let { icon ->
                                            Image(
                                                painter = rememberDrawablePainter(drawable = icon),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.displayName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (installedApps.count { it.displayName == app.displayName } > 1) Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAppsSelected(selected.toSet()) }, enabled = selected.isNotEmpty()) { Text("Monitorar ${selected.size} " + if (selected.size == 1) "app" else "apps") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ServerCard(
    serverUrl: String,
    tokenName: String,
    isConnected: Boolean,
    lastVerifiedTime: Long,
    onEdit: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
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
                            MaterialTheme.colorScheme.success
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = if (isConnected) {
                            "Configurado"
                        } else {
                            stringResource(R.string.settings_server_disconnected)
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar servidor e token",
                            tint = MaterialTheme.colorScheme.secondary
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
            }

            Text("Última verificação: " + if (lastVerifiedTime > 0) br.com.openmonetis.companion.ui.notifications.formatDate(lastVerifiedTime) else "não registrada", style = MaterialTheme.typography.bodySmall)
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
private fun AppToggleItem(
    app: MonitoredAppUi,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            app.icon?.let { icon ->
                Image(
                    painter = rememberDrawablePainter(drawable = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    modifier = Modifier.semantics { contentDescription = "Monitorar ${app.displayName}" },
                    checked = app.isEnabled,
                    onCheckedChange = onToggle
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remover monitoramento de ${app.displayName}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
