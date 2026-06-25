package com.example.logist_tech.ocr

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object OcrProcessor {

    fun parsearTextoOcr(textoOcr: String): OcrData {
        val lineas = textoOcr.lines().map { it.trim() }.filter { it.isNotBlank() }

        val nombre         = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad       = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val pesoKg         = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoriaVal   = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val destino        = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val tipoMovimientoVal = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val fechaVal       = extraerCampoTexto(lineas, listOf("fecha", "date"))

        val categoria = if (categoriaVal.isBlank()) "General" else categoriaVal
        val tipoMovimiento = if (tipoMovimientoVal.isBlank()) "ENTRADA" else tipoMovimientoVal
        val fecha = if (fechaVal.isBlank()) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        } else {
            fechaVal
        }

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

        try {
            val json = JSONObject(qrTexto)
            fun optString(json: JSONObject, keys: List<String>): String {
                for (key in keys) {
                    if (json.has(key)) return json.optString(key)
                }
                val lowerKeys = keys.map { it.lowercase(Locale.getDefault()) }
                for (k in json.keys()) {
                    if (k.lowercase(Locale.getDefault()) in lowerKeys) {
                        return json.optString(k)
                    }
                }
                return ""
            }

            fun optInt(json: JSONObject, keys: List<String>): Int {
                for (key in keys) {
                    if (json.has(key)) return json.optInt(key)
                }
                val lowerKeys = keys.map { it.lowercase(Locale.getDefault()) }
                for (k in json.keys()) {
                    if (k.lowercase(Locale.getDefault()) in lowerKeys) {
                        return json.optInt(k)
                    }
                }
                return 0
            }

            fun optDouble(json: JSONObject, keys: List<String>): Double {
                for (key in keys) {
                    if (json.has(key)) return json.optDouble(key)
                }
                val lowerKeys = keys.map { it.lowercase(Locale.getDefault()) }
                for (k in json.keys()) {
                    if (k.lowercase(Locale.getDefault()) in lowerKeys) {
                        return json.optDouble(k)
                    }
                }
                return 0.0
            }

            val idCaja = optString(json, listOf("idCaja", "idcaja", "id", "caja"))
            val nombre = optString(json, listOf("producto", "nombre", "item"))
            val cantidad = optInt(json, listOf("cantidad", "qty", "unidades"))
            val destino = optString(json, listOf("destino", "destination", "para"))
            val pesoKg = optDouble(json, listOf("peso", "pesoKg", "kg", "weight"))
            val categoria = optString(json, listOf("categoria", "category", "tipo"))
            val tipoMovimiento = optString(json, listOf("movimiento", "movement", "tipo_mov"))
            val fecha = optString(json, listOf("fecha", "date"))

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
        } catch (e: Exception) {
            // Fallback to lines parsing
        }

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