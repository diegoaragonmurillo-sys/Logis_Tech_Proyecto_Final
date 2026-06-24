package com.example.logist_tech.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.logist_tech.auth.SessionManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class ScanMode { QR, OCR_FOTO }

private const val QR_TIMEOUT_SECONDS = 4

@Composable
fun ScannerScreen(onNavigarResultado: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rol = SessionManager.rol
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var scanMode by remember { mutableStateOf(ScanMode.QR) }
    var qrTimeoutAlcanzado by remember { mutableStateOf(false) }
    var secondsWaiting by remember { mutableStateOf(0) }

    // Para captura de foto
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    var procesandoFoto by remember { mutableStateOf(false) }
    var mensajeFoto by remember { mutableStateOf<String?>(null) }

    // Entrada manual (testing)
    var showManualDialog by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        ScannerResultHolder.textoQr = ""
        ScannerResultHolder.textoOcr = ""
        ScannerResultHolder.imagenBitmap = null
    }

    // Timeout QR → muestra botón de foto
    LaunchedEffect(scanMode) {
        if (scanMode == ScanMode.QR) {
            secondsWaiting = 0
            qrTimeoutAlcanzado = false
            repeat(QR_TIMEOUT_SECONDS) {
                delay(1000)
                secondsWaiting++
            }
            if (scanMode == ScanMode.QR) {
                qrTimeoutAlcanzado = true
            }
        }
    }

    // Diálogo entrada manual
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text(if (!qrTimeoutAlcanzado) "Ingresar código QR" else "Ingresar texto OCR") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (!qrTimeoutAlcanzado)
                            "Escribe el código de la caja (ej: CJ-001)"
                        else
                            "Escribe el texto del formulario:\nID: CJ-001\nProducto: Laptop\nCantidad: 5\nPeso: 1.5",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        label = { Text("Contenido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !qrTimeoutAlcanzado,
                        minLines = if (qrTimeoutAlcanzado) 4 else 1
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (manualText.isNotBlank()) {
                        showManualDialog = false
                        if (!qrTimeoutAlcanzado) {
                            ScannerResultHolder.textoQr = manualText
                        } else {
                            ScannerResultHolder.textoOcr = manualText
                        }
                        manualText = ""
                        onNavigarResultado()
                    }
                }) { Text("CONFIRMAR") }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false; manualText = "" }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                cameraExecutor = cameraExecutor,
                scanMode = scanMode,
                onImageCaptureReady = { imageCaptureUseCase = it },
                onQrDetected = { qrText ->
                    ScannerResultHolder.textoQr = qrText
                    ScannerResultHolder.textoOcr = ""
                    onNavigarResultado()
                }
            )

            // ── Overlay según estado ──────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {

                // Toggle BANDA arriba izquierda
                if (rol == SessionManager.Rol.BANDA) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp, start = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(ScanMode.QR to "QR", ScanMode.OCR_FOTO to "OCR").forEach { (mode, label) ->
                            val selected = scanMode == mode
                            Button(
                                onClick = {
                                    if (!selected) {
                                        scanMode = mode
                                        qrTimeoutAlcanzado = mode == ScanMode.OCR_FOTO
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFF2980B9) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text(
                                    label,
                                    color = Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Botón manual discreto arriba derecha
                IconButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Default.Keyboard,
                        contentDescription = "Manual",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }

                // Marco de enfoque
                val modoFoto = scanMode == ScanMode.OCR_FOTO || qrTimeoutAlcanzado
                Box(
                    modifier = Modifier
                        .size(if (modoFoto) 300.dp else 260.dp)
                        .align(Alignment.Center)
                        .border(
                            2.dp,
                            if (modoFoto) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                )

                // Aviso timeout
                AnimatedVisibility(
                    visible = qrTimeoutAlcanzado && scanMode == ScanMode.QR,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-160).dp)
                ) {
                    Text(
                        "No se detectó QR",
                        color = Color(0xFF60A5FA),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Countdown QR
                AnimatedVisibility(
                    visible = !qrTimeoutAlcanzado && scanMode == ScanMode.QR && secondsWaiting > 0,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 150.dp)
                ) {
                    Text(
                        "Buscando QR (${QR_TIMEOUT_SECONDS - secondsWaiting}s)...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                // ── BOTÓN DE FOTO — aparece cuando timeout o modo OCR ──────
                if (qrTimeoutAlcanzado || scanMode == ScanMode.OCR_FOTO) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (mensajeFoto != null) {
                            Text(
                                mensajeFoto!!,
                                color = if (mensajeFoto!!.startsWith("✓")) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            "Encuadra el formulario y toma la foto",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Botón reintentar QR
                            if (qrTimeoutAlcanzado && scanMode == ScanMode.QR) {
                                OutlinedButton(
                                    onClick = {
                                        qrTimeoutAlcanzado = false
                                        secondsWaiting = 0
                                        scanMode = ScanMode.QR
                                    },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reintentar QR")
                                }
                            }

                            // Botón tomar foto
                            if (procesandoFoto) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(52.dp))
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        val capture = imageCaptureUseCase ?: return@FloatingActionButton
                                        procesandoFoto = true
                                        mensajeFoto = null
                                        tomarFotoYProcesarOCR(
                                            imageCapture = capture,
                                            context = context,
                                            onTextoExtraido = { texto ->
                                                procesandoFoto = false
                                                if (texto.isNotBlank()) {
                                                    ScannerResultHolder.textoOcr = texto
                                                    ScannerResultHolder.textoQr = ""
                                                    onNavigarResultado()
                                                } else {
                                                    mensajeFoto = "No se pudo leer el texto. Intenta de nuevo."
                                                }
                                            },
                                            onError = {
                                                procesandoFoto = false
                                                mensajeFoto = "Error al capturar. Intenta de nuevo."
                                            }
                                        )
                                    },
                                    containerColor = Color(0xFF2980B9),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Tomar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Texto inferior modo QR
                    Text(
                        "Apunta al código QR del formulario o la caja",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Se requiere permiso de cámara", color = Color.White)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Dar permiso")
                    }
                    TextButton(onClick = { showManualDialog = true }) {
                        Text("Entrada manual (pruebas)", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    scanMode: ScanMode,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var yaProcesado by remember { mutableStateOf(false) }

    LaunchedEffect(scanMode) { yaProcesado = false }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // ImageCapture para la foto OCR
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Análisis QR en tiempo real
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!yaProcesado && scanMode == ScanMode.QR) {
                        procesarFrameQR(imageProxy, barcodeScanner) { result ->
                            yaProcesado = true
                            ContextCompat.getMainExecutor(context).execute {
                                onQrDetected(result)
                            }
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                    ContextCompat.getMainExecutor(ctx).execute {
                        onImageCaptureReady(imageCapture)
                    }
                } catch (e: Exception) {
                    Log.e("Scanner", "Error CameraX", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { _ -> yaProcesado = false },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose { barcodeScanner.close() }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun procesarFrameQR(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    onFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val qr = barcodes.firstOrNull()?.rawValue
                if (qr != null) onFound(qr)
            }
            .addOnFailureListener { Log.e("MLKit_QR", "Error", it) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

private fun tomarFotoYProcesarOCR(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onTextoExtraido: (String) -> Unit,
    onError: () -> Unit
) {
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(image: ImageProxy) {
                val mediaImage = image.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        image.imageInfo.rotationDegrees
                    )
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            image.close()
                            textRecognizer.close()
                            onTextoExtraido(visionText.text)
                        }
                        .addOnFailureListener {
                            image.close()
                            textRecognizer.close()
                            Log.e("OCR_FOTO", "Error OCR", it)
                            onError()
                        }
                } else {
                    image.close()
                    onError()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("OCR_FOTO", "Error captura", exception)
                onError()
            }
        }
    )
}