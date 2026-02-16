package br.com.openmonetis.companion.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.openmonetis.companion.data.local.dao.SyncLogDao
import br.com.openmonetis.companion.data.local.entities.SyncLogEntity
import br.com.openmonetis.companion.data.local.entities.SyncLogType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LogUiItem(
    val id: String,
    val type: SyncLogType,
    val message: String,
    val details: String?,
    val timestamp: String
)

data class LogsUiState(
    val logs: List<LogUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val syncLogDao: SyncLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val logs = syncLogDao.getRecent(200)
            val uiItems = logs.map { it.toUiItem() }

            _uiState.value = _uiState.value.copy(
                logs = uiItems,
                isLoading = false,
                isEmpty = uiItems.isEmpty()
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            syncLogDao.deleteAll()
            loadLogs()
        }
    }

    private fun SyncLogEntity.toUiItem(): LogUiItem {
        return LogUiItem(
            id = id,
            type = type,
            message = message,
            details = details,
            timestamp = dateFormat.format(Date(timestamp))
        )
    }
}
