package com.example.logist_tech.ocr

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