package br.com.openmonetis.companion.ui.screens.settings.keywords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.openmonetis.companion.data.local.dao.KeywordsSettingsDao
import br.com.openmonetis.companion.data.local.entities.KeywordsSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeywordsSettingsUiState(
    val triggerKeywords: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val showAddTriggerDialog: Boolean = false,
    val newKeyword: String = "",
    val showResetDialog: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class KeywordsSettingsViewModel @Inject constructor(
    private val keywordsSettingsDao: KeywordsSettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeywordsSettingsUiState())
    val uiState: StateFlow<KeywordsSettingsUiState> = _uiState.asStateFlow()

    init {
        loadKeywords()
    }

    private fun loadKeywords() {
        viewModelScope.launch {
            val settings = keywordsSettingsDao.get() ?: KeywordsSettingsEntity()
            _uiState.value = _uiState.value.copy(
                triggerKeywords = settings.getTriggerKeywordsList(),
                isLoading = false
            )
        }
    }

    // Trigger Keywords
    fun showAddTriggerDialog() {
        _uiState.value = _uiState.value.copy(showAddTriggerDialog = true, newKeyword = "")
    }

    fun hideAddTriggerDialog() {
        _uiState.value = _uiState.value.copy(showAddTriggerDialog = false, newKeyword = "")
    }

    fun addTriggerKeyword() {
        val keyword = _uiState.value.newKeyword.trim().lowercase()
        if (keyword.isNotEmpty() && keyword !in _uiState.value.triggerKeywords) {
            val newList = _uiState.value.triggerKeywords + keyword
            _uiState.value = _uiState.value.copy(
                triggerKeywords = newList,
                showAddTriggerDialog = false,
                newKeyword = ""
            )
            saveKeywords()
        }
    }

    fun removeTriggerKeyword(keyword: String) {
        val newList = _uiState.value.triggerKeywords - keyword
        _uiState.value = _uiState.value.copy(triggerKeywords = newList)
        saveKeywords()
    }

    fun updateNewKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(newKeyword = keyword)
    }

    fun showResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = true)
    }

    fun hideResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = false)
    }

    fun resetToDefaults() {
        val defaults = KeywordsSettingsEntity()
        _uiState.value = _uiState.value.copy(
            triggerKeywords = defaults.getTriggerKeywordsList(),
            showResetDialog = false
        )
        saveKeywords()
    }

    private fun saveKeywords() {
        viewModelScope.launch {
            val entity = KeywordsSettingsEntity.fromKeywordList(
                _uiState.value.triggerKeywords
            )
            keywordsSettingsDao.save(entity)
            _uiState.value = _uiState.value.copy(saveSuccess = true)
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
