package com.example.logist_tech.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logist_tech.ui.theme.Logist_TechTheme

/**
 * T-03 — OcrResultScreen
 * Pantalla que muestra los datos parseados del OCR y el resultado
 * de la comparación con el QR. Se llama desde ScannerScreen de Diego
 * cuando hay texto OCR y/o QR detectados.
 */
@Composable
fun OcrResultScreen(
    textoOcr: String,
    textoQr: String,
    onRegistrarEnInventario: (com.example.logist_tech.models.Producto) -> Unit = {}
) {
    // Procesar con OcrProcessor
    val ocrData  = remember(textoOcr) { OcrProcessor.parsearTextoOcr(textoOcr) }
    val qrData   = remember(textoQr)  { OcrProcessor.parsearQr(textoQr) }
    val resultado = remember(ocrData, qrData) { OcrProcessor.compararOcrConQr(ocrData, qrData) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "📋 Resultado del Escaneo",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        // ── Campos extraídos por OCR ──
        CampoCard(titulo = "📦 Producto",  valor = ocrData.nombre.ifBlank  { "No detectado" })
        CampoCard(titulo = "🔢 Cantidad",  valor = if (ocrData.cantidad > 0) "${ocrData.cantidad}" else "No detectado")
        CampoCard(titulo = "⚖️ Peso (kg)", valor = if (ocrData.pesoKg > 0.0) "${ocrData.pesoKg} kg" else "No detectado")
        CampoCard(titulo = "📍 Destino",   valor = ocrData.destino.ifBlank { "No detectado" })

        // ── Resultado comparación QR vs OCR ──
        AnomaliaCard(resultado = resultado)

        // ── Botón para registrar en inventario ──
        if (!resultado.hayAnomalia || resultado.tipo == "SIN_ANOMALIA") {
            Button(
                onClick = {
                    val producto = OcrProcessor.ocrDataToProducto(ocrData)
                    onRegistrarEnInventario(producto)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("✅ Registrar en Inventario", color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Componentes internos
// ─────────────────────────────────────────────

@Composable
private fun CampoCard(titulo: String, valor: String) {
    val esVacio = valor == "No detectado"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esVacio) Color(0xFFFFF3F3) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, fontSize = 13.sp, color = Color.Gray)
            Text(
                text = valor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (esVacio) Color(0xFFE53935) else Color.Unspecified
            )
        }
    }
}

@Composable
private fun AnomaliaCard(resultado: ResultadoComparacion) {
    val bgColor = when {
        !resultado.hayAnomalia        -> Color(0xFFE8F5E9)  // verde claro
        resultado.prioridad == "ALTA" -> Color(0xFFFFEBEE)  // rojo claro
        else                          -> Color(0xFFFFF8E1)  // amarillo claro
    }
    val textColor = when {
        !resultado.hayAnomalia        -> Color(0xFF2E7D32)
        resultado.prioridad == "ALTA" -> Color(0xFFB71C1C)
        else                          -> Color(0xFFF57F17)
    }
    val icono = when {
        !resultado.hayAnomalia        -> "✅"
        resultado.prioridad == "ALTA" -> "🔴"
        else                          -> "🟡"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$icono ${resultado.tipo.replace("_", " ")}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = resultado.descripcion,
                fontSize = 13.sp,
                color = textColor
            )
            if (resultado.hayAnomalia) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prioridad: ${resultado.prioridad}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun OcrResultScreenPreview() {
    Logist_TechTheme {
        OcrResultScreen(
            textoOcr = "Producto: Coca Cola\nCantidad: 24\nPeso: 1.5\nDestino: Lima",
            textoQr  = "{\"producto\":\"Coca Cola\",\"cantidad\":24}"
        )
    }
}