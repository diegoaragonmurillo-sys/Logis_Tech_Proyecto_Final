package com.example.logist_tech.ocr

data class OcrData(
    val nombre: String,
    val cantidad: Int,
    val pesoKg: Double,
    val destino: String,       // no va al backend, solo informativo
    val categoria: String,
    val tipoMovimiento: String, // no va al backend, solo informativo
    val fecha: String,
    val textoOriginal: String,
    val camposFaltantes: List<String>,
    // campos nuevos:
    val prioridad: String = "",
    val esFragil: Int = 0,
    val idProveedor: String = "",
    val idTipoCaja: String = ""
)