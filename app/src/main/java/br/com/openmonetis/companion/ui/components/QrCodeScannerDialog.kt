package br.com.openmonetis.companion.ui.components

import android.annotation.SuppressLint
import androidx.annotation.OptIn as AndroidxOptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.openmonetis.companion.R
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
@AndroidxOptIn(markerClass = [ExperimentalGetImage::class])
fun QrCodeScannerDialog(
    onQrCodeDetected: (String) -> Boolean,
    onDismiss: () -> Unit,
    onCameraError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val detected = remember { AtomicBoolean(false) }
    val disposed = remember { AtomicBoolean(false) }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var invalidCode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            disposed.set(true)
            cameraProvider?.unbindAll()
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).also { previewView ->
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                        providerFuture.addListener(
                            listener@{
                                if (disposed.get()) return@listener

                                try {
                                    val provider = providerFuture.get()
                                    cameraProvider = provider
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { useCase ->
                                            useCase.setAnalyzer(cameraExecutor) { imageProxy ->
                                                analyzeQrCode(
                                                    imageProxy = imageProxy,
                                                    scanner = scanner,
                                                    detected = detected,
                                                    onDetected = { rawValue ->
                                                        ContextCompat.getMainExecutor(context).execute {
                                                            if (!disposed.get()) {
                                                                if (onQrCodeDetected(rawValue)) {
                                                                    detected.set(true)
                                                                } else {
                                                                    detected.set(false)
                                                                    invalidCode = true
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (_: Exception) {
                                    if (!disposed.get()) onCameraError()
                                }
                            },
                            ContextCompat.getMainExecutor(viewContext)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(248.dp)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(999.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.qr_scanner_close)
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.qr_scanner_instruction),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (invalidCode) {
                        Text(
                            text = stringResource(R.string.qr_scanner_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalGetImage
@SuppressLint("UnsafeOptInUsageError")
private fun analyzeQrCode(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    detected: AtomicBoolean,
    onDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || detected.get()) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            val rawValue = barcodes.firstNotNullOfOrNull { it.rawValue }
            if (rawValue != null && detected.compareAndSet(false, true)) onDetected(rawValue)
        }
        .addOnCompleteListener { imageProxy.close() }
}
