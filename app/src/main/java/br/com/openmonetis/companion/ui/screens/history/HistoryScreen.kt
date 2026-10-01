package br.com.openmonetis.companion.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.openmonetis.companion.ui.notifications.*
import br.com.openmonetis.companion.ui.components.OpenMonetisOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigateBack: () -> Unit, onEditConnection: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()) {
    val list by viewModel.listState.collectAsStateWithLifecycle()
    val counts by viewModel.filterCounts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    NotificationDialogsAndFeedback(viewModel, list.items, selectedId, { selectedId = null }, snackbar, onEditConnection)
    ConfirmNotificationDeletion(list.items.firstOrNull { it.id == deleteId }, { deleteId = null }, viewModel::deleteNotification)
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text("Histórico de notificações") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Registros deste aparelho. Enviados e descartados são mantidos por 30 dias; pendências permanecem até serem resolvidas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { NotificationFilters(list.filter, viewModel::setFilter, counts) }
            if (list.loading) item { CircularProgressIndicator() }
            else if (list.items.isEmpty()) item { NotificationEmpty(list.filter) }
            items(list.items, key = { it.id }) { item -> NotificationCard(item, { selectedId = item.id }, { deleteId = item.id }) }
            if (list.hasMore) item { OpenMonetisOutlinedButton(onClick = viewModel::loadMore, modifier = Modifier.fillMaxWidth()) { Text("Carregar mais registros") } }
        }
    }
}
