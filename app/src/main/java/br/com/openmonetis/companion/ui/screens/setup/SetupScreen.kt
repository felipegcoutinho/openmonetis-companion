package br.com.openmonetis.companion.ui.screens.setup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import br.com.openmonetis.companion.ui.components.OpenMonetisOutlinedButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import br.com.openmonetis.companion.R
import br.com.openmonetis.companion.ui.components.OpenMonetisDefaults
import br.com.openmonetis.companion.ui.components.QrCodeScannerDialog
import br.com.openmonetis.companion.ui.components.OpenMonetisLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var cameraBlocked by remember { mutableStateOf(false) }
    val cameraPermissionErrorMessage = stringResource(R.string.setup_camera_permission_required)
    val cameraErrorMessage = stringResource(R.string.setup_camera_error)
    var showQrScanner by remember { mutableStateOf(false) }
    var qrScannerError by remember { mutableStateOf<String?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showQrScanner = true
            qrScannerError = null
        } else {
            qrScannerError = cameraPermissionErrorMessage
            cameraBlocked = (context as? Activity)?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true
        }
    }

    fun openQrScanner() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showQrScanner = true
            qrScannerError = null
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showQrScanner) {
        QrCodeScannerDialog(
            onQrCodeDetected = { payload ->
                viewModel.updateTokenFromQrCode(payload).also { accepted ->
                    if (accepted) {
                        showQrScanner = false
                        scope.launch { snackbar.showSnackbar("Token lido. Confirme para conectar.") }
                    }
                }
            },
            onDismiss = { showQrScanner = false },
            onCameraError = {
                showQrScanner = false
                qrScannerError = cameraErrorMessage
            }
        )
    }

    LaunchedEffect(isConfigured) {
        if (isConfigured) {
            onSetupComplete()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.step) {
                            SetupStep.SERVER_URL -> "1 de 2 · Servidor"
                            SetupStep.TOKEN -> "2 de 2 · Token"
                        }
                    )
                },
                navigationIcon = {
                    if (uiState.step == SetupStep.TOKEN) {
                        IconButton(onClick = { viewModel.goBackToServerStep() }, enabled = !uiState.isLoading) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState.step,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            label = "setup_step"
        ) { step ->
            when (step) {
                SetupStep.SERVER_URL -> ServerUrlStep(
                    serverUrl = uiState.serverUrl,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onServerUrlChange = viewModel::updateServerUrl,
                    onVerifyConnection = viewModel::verifyServerConnection
                )
                SetupStep.TOKEN -> TokenStep(
                    token = uiState.token,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    serverName = uiState.serverUrl,
                    cameraBlocked = cameraBlocked,
                    onCameraSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) },
                    onTokenChange = viewModel::updateToken,
                    onScanQrCode = ::openQrScanner,
                    qrScannerError = qrScannerError,
                    onVerifyToken = viewModel::verifyToken
                )
            }
        }
    }
}

@Composable
private fun ServerUrlStep(
    serverUrl: String,
    isLoading: Boolean,
    error: String?,
    onServerUrlChange: (String) -> Unit,
    onVerifyConnection: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OpenMonetisLogo(
            markHeight = 56.dp,
            wordmarkWidth = 164.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.setup_server_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            colors = OpenMonetisDefaults.textFieldColors(),
            enabled = !isLoading,
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text(stringResource(R.string.setup_server_url_label)) },
            placeholder = { Text(stringResource(R.string.setup_server_url_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (!isLoading && serverUrl.isNotBlank()) { keyboard?.hide(); onVerifyConnection() } }),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerifyConnection,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            enabled = serverUrl.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.setup_verify_connection))
            }
        }
    }
}

@Composable
private fun TokenStep(
    token: String,
    isLoading: Boolean,
    error: String?,
    serverName: String?,
    cameraBlocked: Boolean,
    onCameraSettings: () -> Unit,
    onTokenChange: (String) -> Unit,
    onScanQrCode: () -> Unit,
    qrScannerError: String?,
    onVerifyToken: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (serverName != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Servidor verificado: $serverName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.setup_token_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            colors = OpenMonetisDefaults.textFieldColors(),
            enabled = !isLoading,
            value = token,
            onValueChange = onTokenChange,
            label = { Text(stringResource(R.string.setup_token_label)) },
            placeholder = { Text(stringResource(R.string.setup_token_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardActions = KeyboardActions(onDone = { if (!isLoading && token.isNotBlank()) { keyboard?.hide(); onVerifyToken() } }),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onScanQrCode,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.setup_scan_qr))
        }

        qrScannerError?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        if (cameraBlocked) OutlinedButton(onClick = onCameraSettings, modifier = Modifier.fillMaxWidth()) { Text("Permitir câmera nos ajustes") }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerifyToken,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            enabled = token.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.setup_connect))
            }
        }
    }
}
