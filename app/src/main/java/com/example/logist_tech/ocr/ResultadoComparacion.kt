package com.example.logist_tech.ocr

/**
 * T-03 — ResultadoComparacion
 * Resultado de comparar los datos del OCR contra los del QR.
 * Este objeto se pasa al módulo de Anomalías (T-04).
 *
 * Tipos de anomalía posibles:
 *  - "QR_OCR_DIFERENTE"    → El nombre o cantidad no coincide entre QR y OCR
 *  - "CANTIDAD_VACIA"      → No se detectó cantidad en el OCR
 *  - "PRODUCTO_INEXISTENTE"→ No se detectó nombre de producto en el OCR
 *  - "TEXTO_BORROSO"       → El OCR no pudo leer el documento correctamente
 *  - "SIN_ANOMALIA"        → Todo coincide correctamente
 */
data class ResultadoComparacion(
    val hayAnomalia: Boolean,
    val tipo: String,           // Código del tipo de anomalía
    val descripcion: String,    // Mensaje legible para mostrar en pantalla
    val prioridad: String,      // "BAJA", "MEDIA" o "ALTA"
    val ocrData: OcrData,       // Datos leídos por OCR
    val qrData: QrData?         // Datos leídos por QR (puede ser null si no hay QR)
)