package com.example.logist_tech.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@Composable
fun ScannerScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }

    // Estados para mostrar en pantalla lo que la IA va detectando en tiempo real
    var detectedQrCode by remember { mutableStateOf("Esperando código QR...") }
    var detectedOcrText by remember { mutableStateOf("Esperando texto logístico...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

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

                        // 1. Configuración de la Vista Previa (CameraX)
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        // 2. Configuración del Analizador de Imagen en Tiempo Real (CameraX)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        // Inicializar los reconocedores de Google ML Kit
                        val ocrRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val barcodeScanner = BarcodeScanning.getClient()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(
                                imageProxy = imageProxy,
                                ocrRecognizer = ocrRecognizer,
                                barcodeScanner = barcodeScanner,
                                onQrDetected = { qrText -> detectedQrCode = qrText },
                                onTextDetected = { ocrText -> detectedOcrText = ocrText }
                            )
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis // Vinculamos el analizador inteligente junto a la cámara
                            )
                        } catch (e: Exception) {
                            Log.e("CameraX", "Error al iniciar cámara con escáner", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Capa superior visual (HUD) para ver las detecciones durante la Feria Tecnológica
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Text(text = "📱 [QR Detectado]: $detectedQrCode", color = Color.Green)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "🔍 [OCR Texto]: $detectedOcrText", color = Color.White)
            }

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Se requiere permiso de la cámara para escanear.")
            }
        }
    }
}

// Función interna encargada de procesar cada fotograma de la cámara con las librerías de Inteligencia Artificial
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

        // Ejecutar lectura de QR de manera paralela
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { qrValue ->
                        onQrDetected(qrValue) // Código QR encontrado con éxito
                    }
                }
            }
            .addOnFailureListener { Log.e("MLKit_QR", "Error al leer QR", it) }

        // Ejecutar lectura de Texto (OCR)
        ocrRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotBlank()) {
                    // Tomamos las primeras líneas detectadas para no saturar la pantalla de pruebas
                    val shortText = visionText.text.lines().take(3).joinToString(" | ")
                    onTextDetected(shortText) // Texto OCR encontrado con éxito
                }
            }
            .addOnFailureListener { Log.e("MLKit_OCR", "Error en OCR", it) }
            .addOnCompleteListener {
                // Obligatorio para que CameraX libere el fotograma y procese el siguiente
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}


@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun ScannerScreenPreview() {
    // Esto te permitirá ver cómo se renderiza tu pantalla en el "Design" split de Android Studio
    ScannerScreen()
}