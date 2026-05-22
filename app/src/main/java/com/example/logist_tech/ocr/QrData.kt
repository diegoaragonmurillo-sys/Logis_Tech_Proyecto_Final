package com.example.logist_tech.ocr

/**
 * T-03 — QrData
 * Representa los datos extraídos del código QR (formato JSON).
 * Ejemplo de QR esperado: {"producto":"Laptop","cantidad":10}
 */
data class QrData(
    val nombre: String,
    val cantidad: Int
)