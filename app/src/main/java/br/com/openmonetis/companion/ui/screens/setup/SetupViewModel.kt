package br.com.openmonetis.companion.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.openmonetis.companion.data.remote.DeviceConnectionVerifier
import br.com.openmonetis.companion.data.remote.OpenMonetisApi
import br.com.openmonetis.companion.util.CompanionQrCode
import br.com.openmonetis.companion.util.SecureStorage
import br.com.openmonetis.companion.util.ServerUrlPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val step: SetupStep = SetupStep.SERVER_URL,
    val serverUrl: String = "",
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverName: String? = null,
    val serverVersion: String? = null
)

enum class SetupStep {
    SERVER_URL,
    TOKEN
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val connectionVerifier: DeviceConnectionVerifier,
    private val savedState: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState(serverUrl = savedState["serverUrl"] ?: ""))
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _isConfigured = MutableStateFlow(secureStorage.isConfigured())
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    fun updateServerUrl(url: String) {
        if (_uiState.value.isLoading) return
        savedState["serverUrl"] = url
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun updateToken(token: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(token = token, error = null)
    }

    fun verifyServerConnection() {
        if (_uiState.value.isLoading) return
        val url = ServerUrlPolicy.normalize(_uiState.value.serverUrl)
        if (url == null) {
            _uiState.value = _uiState.value.copy(error = "Use uma URL HTTPS válida")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = connectionVerifier.healthCheck(ServerUrlPolicy.parse(url)!!)

                if (response.isSuccessful && response.body()?.status == "ok") {
                    val body = response.body()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        step = SetupStep.TOKEN,
                        serverUrl = url,
                        serverName = body?.name,
                        serverVersion = body?.version
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Servidor não disponível"
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Não foi possível conectar ao servidor"
                )
            }
        }
    }

    fun verifyToken() {
        if (_uiState.value.isLoading) return
        val token = CompanionQrCode.extractToken(_uiState.value.token)
        val serverOrigin = ServerUrlPolicy.parse(_uiState.value.serverUrl)
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Informe um token válido")
            return
        }
        if (serverOrigin == null) {
            _uiState.value = _uiState.value.copy(error = "Configure novamente o servidor")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val verified = connectionVerifier.verify(serverOrigin, token)
                if (verified != null) {
                    secureStorage.saveCredentials(
                        serverUrl = _uiState.value.serverUrl,
                        accessToken = token,
                        tokenId = verified.tokenId,
                        tokenName = verified.tokenName
                    )

                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _isConfigured.value = true
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Token inválido ou expirado"
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Não foi possível acessar o servidor. Verifique a conexão e tente novamente."
                )
            }
        }
    }

    fun updateTokenFromQrCode(payload: String): Boolean {
        val token = CompanionQrCode.extractToken(payload)
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "QR Code inválido")
            return false
        }
        _uiState.value = _uiState.value.copy(token = token, error = null)
        return true
    }

    fun goBackToServerStep() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(
            step = SetupStep.SERVER_URL,
            error = null
        )
    }
}
