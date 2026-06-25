package com.example.logist_tech.ocr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OcrResultScreen(
    textoOcr: String,
    textoQr: String,
    imagenCapturada: Bitmap? = null,
    onRegistrarEnInventario: () -> Unit = {},
    onVolver: () -> Unit = {}
) {
    val esEscaneoQr = textoOcr.isBlank() && textoQr.isNotBlank()

    val ocrData = remember(textoOcr) { OcrProcessor.parsearTextoOcr(textoOcr) }
    val qrData  = remember(textoQr)  { OcrProcessor.parsearQr(textoQr) }

    val nombreMostrar = ocrData.nombre
        .orEmpty().ifBlank { "No detectado" }

    val cantidadMostrar = run {
        val n = ocrData.cantidad
        if (n > 0) "$n" else "No detectado"
    }

    val pesoMostrar = run {
        val p = ocrData.pesoKg
        if (p > 0.0) "$p kg" else "No especificado"
    }

    val categoriaMostrar = ocrData.categoria
        .orEmpty().ifBlank { "No especificado" }

    val destinoMostrar = ocrData.destino
        .orEmpty().ifBlank { "No detectado" }

    val movimientoMostrar = ocrData.tipoMovimiento
        .orEmpty().ifBlank { "No especificado" }

    val fechaMostrar = "Se genera al registrar"

    val datosLegibles = !esEscaneoQr && ocrData.nombre.isNotBlank() && ocrData.cantidad > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "Resultado del Escaneo",
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = Color(0xFF123B6D)
        )
        Text(
            text     = if (esEscaneoQr) "Modo: Escaneo QR" else "Modo: OCR / Foto",
            fontSize = 13.sp,
            color    = Color.Gray
        )

        if (!esEscaneoQr && imagenCapturada != null) {
            Image(
                bitmap             = imagenCapturada.asImageBitmap(),
                contentDescription = "Foto OCR capturada",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE7EAF0))
            )
        }

        CampoCard("Producto",   nombreMostrar)
        CampoCard("Cantidad",   cantidadMostrar)
        CampoCard("Destino",    destinoMostrar)
        CampoCard("Peso (kg)",  pesoMostrar,       esOpcional = true)
        CampoCard("Categoría",  categoriaMostrar,  esOpcional = true)
        CampoCard("Movimiento", movimientoMostrar, esOpcional = true)
        CampoCard("Fecha",      fechaMostrar,      esOpcional = true)

        Spacer(Modifier.height(8.dp))

        if (datosLegibles) {
            Button(
                onClick  = { onRegistrarEnInventario() },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Registrar en Inventario", color = Color.White)
            }
            Spacer(Modifier.height(4.dp))
        }

        Button(
            onClick  = { onVolver() },
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9))
        ) {
            Text("Volver al inicio", color = Color.White)
        }
    }
}

@Composable
private fun CampoCard(
    titulo: String,
    valor: String,
    esOpcional: Boolean = false
) {
    val noDetectado = valor == "No detectado" || valor == "No especificado"
    val containerColor = if (!esOpcional && noDetectado) Color(0xFFFFF3F3)
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = when {
        !esOpcional && noDetectado -> Color(0xFFE53935)
        esOpcional && noDetectado  -> Color(0xFF888888)
        else                       -> Color.Unspecified
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(titulo, fontSize = 13.sp, color = Color.Gray)
            Text(
                text       = valor,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = textColor
            )
        }
    }
}