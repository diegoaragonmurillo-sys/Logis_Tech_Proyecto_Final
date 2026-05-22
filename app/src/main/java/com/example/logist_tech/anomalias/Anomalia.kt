package com.example.logist_tech.anomalias

/**
 * T-04 — Anomalia
 * Representa una anomalía detectada y registrada en el sistema.
 * Se genera a partir del ResultadoComparacion del módulo OCR (T-03).
 */
data class Anomalia(
    val id: String,                  // ID único (timestamp)
    val fecha: String,               // Fecha de detección (yyyy-MM-dd HH:mm)
    val tipo: String,                // "QR_OCR_DIFERENTE", "CANTIDAD_VACIA", etc.
    val descripcion: String,         // Mensaje legible del error
    val prioridad: String,           // "BAJA", "MEDIA" o "ALTA"
    val productoNombre: String,      // Nombre del producto involucrado
    val textoOcrOriginal: String,    // Texto crudo del OCR (evidencia del documento)
    val evidenciaUri: String = ""    // URI de la foto capturada (opcional)
)