package br.com.openmonetis.companion.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkInfo
import br.com.openmonetis.companion.ui.components.OpenMonetisLogo
import br.com.openmonetis.companion.ui.components.OpenMonetisOutlinedButton
import br.com.openmonetis.companion.ui.notifications.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit, onNavigateToHistory: (NotificationFilter) -> Unit, onEditConnection: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val list by viewModel.listState.collectAsStateWithLifecycle()
    val counts by viewModel.filterCounts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refreshPermissionStatus() } }
    NotificationDialogsAndFeedback(viewModel, list.items, selectedId, { selectedId = null }, snackbar, onEditConnection)
    ConfirmNotificationDeletion(list.items.firstOrNull { it.id == deleteId }, { deleteId = null }, viewModel::deleteNotification)
    val busy = state.syncState in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED)
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { OpenMonetisLogo(markHeight = 28.dp, wordmarkWidth = 120.dp) }, actions = {
            IconButton(onClick = viewModel::refreshData, enabled = !busy && state.configured) { Icon(Icons.Default.Refresh, "Sincronizar pendências") }
            IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Configurações") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seu OpenMonetis", style = MaterialTheme.typography.headlineMedium)
                    Text("Acompanhe a captura e o envio de notificações.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val status = when {
                            state.syncState == WorkInfo.State.RUNNING -> "Enviando · ${state.sentInRun} concluídos"
                            busy -> "Envio agendado · aguardando conexão ou nova tentativa"
                            state.syncError == "AUTH" -> "Atualize o token para retomar os envios"
                            state.failedCount > 0 -> "Há notificações com erro de envio"
                            !state.hasNotificationPermission -> "A captura precisa de permissão"
                            state.monitoredApps.isEmpty() -> "Selecione apps para começar a capturar"
                            else -> "Captura habilitada neste aparelho"
                        }
                        Text(status, style = MaterialTheme.typography.titleMedium)
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Text("Conexão configurada", style = MaterialTheme.typography.bodySmall)
                        state.lastVerifiedTime?.let { verifiedTime ->
                            Text("Última verificação: $verifiedTime", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Último envio: ${state.lastSyncTime ?: "nenhum envio registrado"}", style = MaterialTheme.typography.bodySmall)
                        if (state.syncError == "AUTH") TextButton(onClick = onEditConnection,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Text("Atualizar conexão") }
                    }
                }
            }
            if (!state.hasNotificationPermission || state.monitoredApps.isEmpty()) item {
                OutlinedCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Concluir configuração", style = MaterialTheme.typography.titleMedium)
                        Text("1. Autorize o acesso às notificações.\n2. Escolha os apps que deseja monitorar.\n3. As próximas notificações serão capturadas automaticamente.")
                        if (!state.hasNotificationPermission) Button(onClick = { context.startActivity(viewModel.openNotificationSettings()) }, modifier = Modifier.fillMaxWidth()) { Text("Autorizar captura") }
                        if (state.monitoredApps.isEmpty()) OpenMonetisOutlinedButton(onClick = onNavigateToSettings, modifier = Modifier.fillMaxWidth()) { Text("Selecionar apps") }
                    }
                }
            }
            if (state.monitoredApps.isNotEmpty()) item {
                MonitoredAppsCard(state.monitoredApps, onNavigateToSettings)
            }
            item {
                // Wrap naturally at larger font scales instead of squeezing three columns.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CounterCard("Aguardando envio", state.pendingCount) { onNavigateToHistory(NotificationFilter.PENDING) }
                    CounterCard("Com erro", state.failedCount) { onNavigateToHistory(NotificationFilter.FAILED) }
                    CounterCard("Enviados hoje", state.syncedToday) { onNavigateToHistory(NotificationFilter.SENT) }
                }
            }
            item {
                Column {
                    Text("Notificações recentes", style = MaterialTheme.typography.titleLarge)
                    NotificationFilters(list.filter, viewModel::setFilter, counts)
                }
            }
            if (list.loading) item { CircularProgressIndicator() }
            else if (list.items.isEmpty()) item { NotificationEmpty(list.filter) }
            items(list.items, key = { it.id }) { item -> NotificationCard(item, { selectedId = item.id }, { deleteId = item.id }) }
            item { OpenMonetisOutlinedButton(onClick = { onNavigateToHistory(list.filter) }, modifier = Modifier.fillMaxWidth()) { Text("Ver histórico completo") } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonitoredAppsCard(apps: List<MonitoredAppIcon>, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("monitored-apps")) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${apps.size} " + if (apps.size == 1) "app monitorado" else "apps monitorados",
                style = MaterialTheme.typography.titleSmall)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val density = LocalDensity.current
                val gap = 12.dp
                val minCellWidth = 88.dp * density.fontScale
                val columns = ((maxWidth + gap) / (minCellWidth + gap)).toInt().coerceIn(1, 3)
                val cellWidth = with(density) {
                    ((constraints.maxWidth - gap.roundToPx() * (columns - 1)) / columns).toDp()
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = columns
                ) {
                    apps.forEach { app ->
                        Column(Modifier.width(cellWidth), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val iconModifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small)
                                .testTag("monitored-app-icon:${app.packageName}")
                            if (app.icon != null) Image(rememberDrawablePainter(app.icon), null, iconModifier)
                            else Icon(Icons.Default.Apps, null, iconModifier, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(app.displayName, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterCard(label: String, count: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(12.dp))
            Text(count.toString(), style = MaterialTheme.typography.titleLarge)
        }
    }
}
