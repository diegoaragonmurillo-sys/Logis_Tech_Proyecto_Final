package com.example.logist_tech.ocr

/**
 * T-03 — OcrData
 * Representa los datos extraídos del texto OCR de un documento logístico.
 * Los campos faltantes se registran para poder generar anomalías.
 */
data class OcrData(
    val nombre: String,           // Nombre del producto detectado
    val cantidad: Int,            // Cantidad detectada (0 si no se encontró)
    val pesoKg: Double,           // Peso en kg (0.0 si no se encontró)
    val destino: String,          // Destino del producto
    val textoOriginal: String,    // Texto crudo del OCR (para debug o evidencia)
    val camposFaltantes: List<String>  // Campos que no se pudieron extraer
)