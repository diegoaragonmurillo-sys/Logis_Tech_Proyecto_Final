package com.example.logist_tech.ocr

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OcrProcessor {

    private fun fechaActual() =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun extraerIdDeTexto(texto: String): String {
        val lineas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
        val raw = extraerCampoTexto(lineas, listOf("id", "codigo", "serie", "idcaja"))
        return if (Regex("^CJ-\\d{4}$").matches(raw)) raw else ""
    }

    fun parsearTextoOcr(textoOcr: String): OcrData {
        val lineas = textoOcr.lines().map { it.trim() }.filter { it.isNotBlank() }

        val nombre         = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad       = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val pesoKg         = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoria      = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val destino        = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val tipoMovimiento = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val prioridad      = extraerCampoTexto(lineas, listOf("prioridad", "priority"))
        val esFragilStr    = extraerCampoTexto(lineas, listOf("fragil", "fragile"))
        val esFragil       = if (esFragilStr == "1" || esFragilStr.lowercase() == "si") 1 else 0
        val idProveedor    = extraerCampoTexto(lineas, listOf("proveedor", "provider", "id_proveedor"))
        val idTipoCaja     = extraerCampoTexto(lineas, listOf("tipocaja", "tipo_caja", "id_tipo_caja"))
        val fecha          = fechaActual()

        val camposFaltantes = mutableListOf<String>()
        if (nombre.isBlank())     camposFaltantes.add("producto")
        if (cantidad == 0)        camposFaltantes.add("cantidad")
        if (prioridad.isBlank())  camposFaltantes.add("prioridad")
        if (idTipoCaja.isBlank()) camposFaltantes.add("tipo_caja")

        return OcrData(
            nombre          = nombre,
            cantidad        = cantidad,
            pesoKg          = pesoKg,
            destino         = destino,
            categoria       = categoria,
            tipoMovimiento  = tipoMovimiento,
            fecha           = fecha,
            textoOriginal   = textoOcr,
            camposFaltantes = camposFaltantes,
            prioridad       = prioridad,
            esFragil        = esFragil,
            idProveedor     = idProveedor,
            idTipoCaja      = idTipoCaja
        )
    }

    fun parsearQr(qrTexto: String): QrData? {
        if (qrTexto.isBlank() || qrTexto == "Esperando código QR...") return null

        val lineas = qrTexto.lines().map { it.trim() }.filter { it.isNotBlank() }

        val idCaja         = extraerCampoTexto(lineas, listOf("id", "idcaja", "caja", "codigo"))
        val nombre         = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad       = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val destino        = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val pesoKg         = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoria      = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val tipoMovimiento = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val prioridad      = extraerCampoTexto(lineas, listOf("prioridad", "priority"))
        val esFragilStr    = extraerCampoTexto(lineas, listOf("fragil", "fragile"))
        val esFragil       = if (esFragilStr == "1" || esFragilStr.lowercase() == "si") 1 else 0
        val idProveedor    = extraerCampoTexto(lineas, listOf("proveedor", "provider", "id_proveedor"))
        val idTipoCaja     = extraerCampoTexto(lineas, listOf("tipocaja", "tipo_caja", "id_tipo_caja"))
        val fecha          = fechaActual()

        if (idCaja.isBlank() && nombre.isBlank() && cantidad == 0) return null

        return QrData(
            idCaja         = idCaja,
            nombre         = nombre,
            cantidad       = cantidad,
            destino        = destino,
            pesoKg         = pesoKg,
            categoria      = categoria,
            tipoMovimiento = tipoMovimiento,
            fecha          = fecha,
            prioridad      = prioridad,
            esFragil       = esFragil,
            idProveedor    = idProveedor,
            idTipoCaja     = idTipoCaja
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