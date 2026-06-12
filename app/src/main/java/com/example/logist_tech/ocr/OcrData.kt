package com.example.logist_tech.ocr

/**
 * T-03 — OcrData
 * Representa los datos extraídos del texto OCR de un documento logístico.
 * Los campos faltantes se registran para poder generar anomalías.
 */
data class OcrData(
    val nombre: String,
    val cantidad: Int,
    val pesoKg: Double,
    val destino: String,
    val categoria: String,
    val tipoMovimiento: String,
    val fecha: String,
    val textoOriginal: String,
    val camposFaltantes: List<String>
)