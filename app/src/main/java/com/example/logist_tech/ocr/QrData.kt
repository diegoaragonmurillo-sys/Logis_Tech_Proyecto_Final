package com.example.logist_tech.ocr

/**
 * T-03 — QrData
 * Representa los datos extraídos del código QR (formato JSON).
 * Formato completo: {"idCaja":"CJ-001","producto":"Laptop","cantidad":10,"destino":"Lima","peso":1.5}
 */
data class QrData(
    val idCaja: String,
    val nombre: String,
    val cantidad: Int,
    val destino: String        = "",
    val pesoKg: Double         = 0.0,
    val categoria: String      = "",
    val tipoMovimiento: String = "",
    val fecha: String          = ""
)