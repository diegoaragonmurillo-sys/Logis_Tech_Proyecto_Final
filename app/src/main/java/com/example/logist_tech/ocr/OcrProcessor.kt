package com.example.logist_tech.ocr

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OcrProcessor {

    private fun fechaActual() =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun extraerIdDeTexto(texto: String): String {
        val lineas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
        val raw = extraerCampoTexto(lineas, listOf("id", "codigo", "serie", "idcaja"))
        // Normaliza: quita espacios y pasa a mayúsculas antes de validar formato
        val limpio = raw.replace(" ", "").uppercase()
        return if (Regex("^CJ-\\d{4}$").matches(limpio)) limpio else ""
    }

    fun parsearTextoOcr(textoOcr: String): OcrData {
        val lineas = textoOcr.lines().map { it.trim() }.filter { it.isNotBlank() }

        val nombre            = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad          = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val pesoKg            = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoriaVal      = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val destino           = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val tipoMovimientoVal = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val prioridad         = extraerCampoTexto(lineas, listOf("prioridad", "priority"))
        val esFragilStr       = extraerCampoTexto(lineas, listOf("fragil", "fragile"))
        val esFragil          = if (esFragilStr == "1" || esFragilStr.lowercase() == "si") 1 else 0
        val idProveedor       = extraerCampoTexto(lineas, listOf("proveedor", "provider", "id_proveedor"))
        val idTipoCaja        = extraerCampoTexto(lineas, listOf("tipocaja", "tipo_caja", "id_tipo_caja"))

        val categoria      = if (categoriaVal.isBlank()) "General" else categoriaVal
        val tipoMovimiento = if (tipoMovimientoVal.isBlank()) "ENTRADA" else tipoMovimientoVal
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

        // Intento 1: JSON
        try {
            val json = JSONObject(qrTexto)

            fun optString(json: JSONObject, keys: List<String>): String {
                for (key in keys) if (json.has(key)) return json.optString(key)
                val lowerKeys = keys.map { it.lowercase(Locale.getDefault()) }
                for (k in json.keys()) {
                    if (k.lowercase(Locale.getDefault()) in lowerKeys) return json.optString(k)
                }
                return ""
            }

            fun optInt(json: JSONObject, keys: List<String>): Int {
                for (key in keys) if (json.has(key)) return json.optInt(key)
                val lowerKeys = keys.map { it.lowercase(Locale.getDefault()) }
                for (k in json.keys()) {
                    if (k.lowercase(Locale.getDefault()) in lowerKeys) return json.optInt(k)
                }
                return 0
            }

            fun optDouble(json: JSONObject, keys: List<String>): Double {
                for (key in keys) if (json.has(key)) return json.optDouble(key)
                val lowerKeys = keys.map { it.lowercase(Locale.getDefault()) }
                for (k in json.keys()) {
                    if (k.lowercase(Locale.getDefault()) in lowerKeys) return json.optDouble(k)
                }
                return 0.0
            }

            val idCaja         = optString(json, listOf("idCaja", "idcaja", "id", "caja"))
            val nombre         = optString(json, listOf("producto", "nombre", "item"))
            val cantidad       = optInt(json, listOf("cantidad", "qty", "unidades"))
            val destino        = optString(json, listOf("destino", "destination", "para"))
            val pesoKg         = optDouble(json, listOf("peso", "pesoKg", "kg", "weight"))
            val categoria      = optString(json, listOf("categoria", "category", "tipo"))
            val tipoMovimiento = optString(json, listOf("movimiento", "movement", "tipo_mov"))
            val prioridad      = optString(json, listOf("prioridad", "priority"))
            val esFragilRaw    = optString(json, listOf("fragil", "fragile"))
            val esFragil       = if (esFragilRaw == "1" || esFragilRaw.lowercase() == "si") 1
            else optInt(json, listOf("fragil", "fragile"))
            val idProveedor    = optString(json, listOf("proveedor", "provider", "id_proveedor"))
            val idTipoCaja     = optString(json, listOf("tipocaja", "tipo_caja", "id_tipo_caja"))

            if (idCaja.isBlank() && nombre.isBlank() && cantidad == 0) return null

            return QrData(
                idCaja         = idCaja,
                nombre         = nombre,
                cantidad       = cantidad,
                destino        = destino,
                pesoKg         = pesoKg,
                categoria      = categoria,
                tipoMovimiento = tipoMovimiento,
                fecha          = fechaActual(),
                prioridad      = prioridad,
                esFragil       = esFragil,
                idProveedor    = idProveedor,
                idTipoCaja     = idTipoCaja
            )
        } catch (e: Exception) {
            // No es JSON, caemos a parseo por líneas
        }

        // Intento 2: líneas clave:valor
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

        if (idCaja.isBlank() && nombre.isBlank() && cantidad == 0) return null

        return QrData(
            idCaja         = idCaja,
            nombre         = nombre,
            cantidad       = cantidad,
            destino        = destino,
            pesoKg         = pesoKg,
            categoria      = categoria,
            tipoMovimiento = tipoMovimiento,
            fecha          = fechaActual(),
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