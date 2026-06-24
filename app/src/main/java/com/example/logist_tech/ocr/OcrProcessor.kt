package com.example.logist_tech.ocr

object OcrProcessor {

    fun parsearTextoOcr(textoOcr: String): OcrData {
        val lineas = textoOcr.lines().map { it.trim() }.filter { it.isNotBlank() }

        val nombre         = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad       = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val pesoKg         = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoria      = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val destino        = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val tipoMovimiento = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val fecha          = extraerCampoTexto(lineas, listOf("fecha", "date"))

        val camposFaltantes = mutableListOf<String>()
        if (nombre.isBlank())  camposFaltantes.add("producto")
        if (cantidad == 0)     camposFaltantes.add("cantidad")
        if (destino.isBlank()) camposFaltantes.add("destino")

        return OcrData(
            nombre          = nombre,
            cantidad        = cantidad,
            pesoKg          = pesoKg,
            destino         = destino,
            categoria       = categoria,
            tipoMovimiento  = tipoMovimiento,
            fecha           = fecha,
            textoOriginal   = textoOcr,
            camposFaltantes = camposFaltantes
        )
    }

    fun parsearQr(qrTexto: String): QrData? {
        if (qrTexto.isBlank() || qrTexto == "Esperando código QR...") return null

        val lineas = qrTexto.lines().map { it.trim() }.filter { it.isNotBlank() }

        val idCaja         = extraerCampoTexto(lineas, listOf("idcaja", "id", "caja"))
        val nombre         = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad       = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val destino        = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val pesoKg         = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoria      = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val tipoMovimiento = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val fecha          = extraerCampoTexto(lineas, listOf("fecha", "date"))

        if (idCaja.isBlank() && nombre.isBlank() && cantidad == 0 && destino.isBlank()) return null

        return QrData(
            idCaja         = idCaja,
            nombre         = nombre,
            cantidad       = cantidad,
            destino        = destino,
            pesoKg         = pesoKg,
            categoria      = categoria,
            tipoMovimiento = tipoMovimiento,
            fecha          = fecha
        )
    }

    private fun extraerCampoTexto(lineas: List<String>, claves: List<String>): String {
        for (linea in lineas) {
            val lineaLower = linea.lowercase()
            for (clave in claves) {
                val regex = Regex("\\b${Regex.escape(clave)}\\s*:\\s*(.+)", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(lineaLower)) {
                    val colonIndex = linea.indexOf(':', linea.lowercase().indexOf(clave))
                    val valor = if (colonIndex >= 0) linea.substring(colonIndex + 1).trim() else ""
                    if (valor.isNotBlank()) return valor
                }
            }
        }
        return ""
    }

    private fun extraerCampoNumeroEntero(lineas: List<String>, claves: List<String>): Int {
        val texto = extraerCampoTexto(lineas, claves)
        return Regex("\\d+").find(texto)?.value?.toIntOrNull() ?: 0
    }

    private fun extraerCampoNumeroDecimal(lineas: List<String>, claves: List<String>): Double {
        val texto = extraerCampoTexto(lineas, claves)
        return Regex("\\d+([.,]\\d+)?").find(texto)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }
}