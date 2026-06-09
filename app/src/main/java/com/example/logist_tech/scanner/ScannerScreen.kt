package com.example.logist_tech.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

enum class ScanMode { QR, OCR }

@Composable
fun ScannerScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf(ScanMode.QR) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    var procesandoFoto by remember { mutableStateOf(false) }

    // Estados del HUD original
    var detectedQrCode by remember { mutableStateOf("Esperando código QR...") }
    var detectedOcrText by remember { mutableStateOf("Esperando texto logístico...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageCapture = ImageCapture.Builder().build()
                        imageCaptureUseCase = imageCapture

                        val ocrRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val barcodeScanner = BarcodeScanning.getClient()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (scanMode == ScanMode.QR) {
                                // Solo QR activo — evita el conflicto
                                processImageProxy(
                                    imageProxy = imageProxy,
                                    ocrRecognizer = ocrRecognizer,
                                    barcodeScanner = barcodeScanner,
                                    onQrDetected = { qrText -> detectedQrCode = qrText },
                                    onTextDetected = { ocrText -> detectedOcrText = ocrText }
                                )
                            } else {
                                // En modo OCR no procesamos frames en tiempo real
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("CameraX", "Error al iniciar cámara con escáner", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Selector QR / OCR arriba ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { scanMode = ScanMode.QR },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanMode == ScanMode.QR) Color(0xFF2980B9) else Color.Gray
                    ),
                    shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp),
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("📱 QR", color = Color.White)
                }
                Button(
                    onClick = { scanMode = ScanMode.OCR },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanMode == ScanMode.OCR) Color(0xFF2980B9) else Color.Gray
                    ),
                    shape = RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp),
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("🔍 OCR", color = Color.White)
                }
            }

            // ── HUD inferior ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scanMode == ScanMode.QR) {
                    Text(text = "📱 [QR Detectado]: $detectedQrCode", color = Color.Green)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "🔍 [OCR Texto]: $detectedOcrText", color = Color.White)
                } else {
                    Text(
                        text = "Apunta la cámara al documento y toma la foto",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            procesandoFoto = true
                            tomarFotoYProcesar(
                                imageCapture = imageCaptureUseCase,
                                context = context,
                                onResultado = { textoOcr ->
                                    procesandoFoto = false
                                    Log.d("OCR_FOTO", "Texto extraído: $textoOcr")
                                },
                                onError = {
                                    procesandoFoto = false
                                    Log.e("OCR_FOTO", "Error al procesar foto", it)
                                }
                            )
                        },
                        enabled = !procesandoFoto,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2980B9)
                        )
                    ) {
                        if (procesandoFoto) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("📷 Tomar Foto", color = Color.White)
                        }
                    }
                }
            }

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Se requiere permiso de la cámara para escanear.")
            }
        }
    }
}

// ── Función original conservada ──
@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    ocrRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onQrDetected: (String) -> Unit,
    onTextDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { qrValue -> onQrDetected(qrValue) }
                }
            }
            .addOnFailureListener { Log.e("MLKit_QR", "Error al leer QR", it) }

        ocrRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotBlank()) {
                    val shortText = visionText.text.lines().take(3).joinToString(" | ")
                    onTextDetected(shortText)
                }
            }
            .addOnFailureListener { Log.e("MLKit_OCR", "Error en OCR", it) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

// ── Nueva función — toma foto estática y procesa OCR ──
private fun tomarFotoYProcesar(
    imageCapture: ImageCapture?,
    context: android.content.Context,
    onResultado: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(Exception("ImageCapture no disponible"))
        return
    }

    val ocrRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
                    ocrRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            onResultado(visionText.text)
                        }
                        .addOnFailureListener { onError(it) }
                        .addOnCompleteListener { image.close() }
                } else {
                    image.close()
                    onError(Exception("Imagen vacía"))
                }
            }
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}