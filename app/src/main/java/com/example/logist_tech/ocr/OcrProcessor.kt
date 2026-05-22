package com.example.logist_tech.ocr

import org.json.JSONObject

/**
 * T-03 — OcrProcessor
 * Lógica principal del módulo OCR:
 *  1. Parsea el texto crudo del OCR en campos logísticos (nombre, cantidad, peso, destino)
 *  2. Parsea el JSON del QR en un QrData
 *  3. Compara ambos y genera un ResultadoComparacion para el módulo de Anomalías (T-04)
 */
object OcrProcessor {

    // ─────────────────────────────────────────────────────────────────
    // 1. PARSEAR TEXTO OCR → OcrData
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extrae campos logísticos del texto crudo detectado por ML Kit OCR.
     *
     * Formatos de texto aceptados (insensible a mayúsculas):
     *   Producto: Coca Cola
     *   Cantidad: 24
     *   Peso: 1.5
     *   Destino: Lima
     *
     * Si un campo no se encuentra, se registra en [OcrData.camposFaltantes].
     */
    fun parsearTextoOcr(textoOcr: String): OcrData {
        val camposFaltantes = mutableListOf<String>()

        // Normalizar: quitar espacios extra, convertir a minúsculas para búsqueda
        val lineas = textoOcr.lines().map { it.trim() }

        val nombre   = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val peso     = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val destino  = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))

        if (nombre.isBlank())  camposFaltantes.add("nombre")
        if (cantidad == 0)     camposFaltantes.add("cantidad")
        if (peso == 0.0)       camposFaltantes.add("pesoKg")
        if (destino.isBlank()) camposFaltantes.add("destino")

        return OcrData(
            nombre          = nombre,
            cantidad        = cantidad,
            pesoKg          = peso,
            destino         = destino,
            textoOriginal   = textoOcr,
            camposFaltantes = camposFaltantes
        )
    }

    /** Busca la primera línea que empiece con alguna de las claves y extrae el valor como texto. */
    private fun extraerCampoTexto(lineas: List<String>, claves: List<String>): String {
        for (linea in lineas) {
            val lineaLower = linea.lowercase()
            for (clave in claves) {
                if (lineaLower.startsWith(clave)) {
                    // Extraer lo que viene después de ":" o de la clave misma
                    val valor = linea
                        .substringAfter(":", "")
                        .trim()
                    if (valor.isNotBlank()) return valor
                }
            }
        }
        return ""
    }

    /** Busca un valor numérico entero (ej: cantidad). */
    private fun extraerCampoNumeroEntero(lineas: List<String>, claves: List<String>): Int {
        val texto = extraerCampoTexto(lineas, claves)
        // Extrae solo los dígitos del valor encontrado
        return Regex("\\d+").find(texto)?.value?.toIntOrNull() ?: 0
    }

    /** Busca un valor numérico decimal (ej: peso). */
    private fun extraerCampoNumeroDecimal(lineas: List<String>, claves: List<String>): Double {
        val texto = extraerCampoTexto(lineas, claves)
        return Regex("\\d+([.,]\\d+)?").find(texto)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. PARSEAR JSON DEL QR → QrData
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parsea el JSON del código QR en un [QrData].
     * Formato esperado: {"producto":"Laptop","cantidad":10}
     * Retorna null si el QR no tiene el formato correcto o está vacío.
     */
    fun parsearQr(qrJson: String): QrData? {
        if (qrJson.isBlank() || qrJson == "Esperando código QR...") return null
        return try {
            val json     = JSONObject(qrJson)
            val nombre   = json.optString("producto", "").trim()
            val cantidad = json.optInt("cantidad", 0)
            if (nombre.isBlank()) null
            else QrData(nombre = nombre, cantidad = cantidad)
        } catch (e: Exception) {
            // El QR no es JSON válido — puede ser un QR de otro formato
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. COMPARAR OCR vs QR → ResultadoComparacion (para T-04)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compara los datos extraídos del OCR con los del QR y detecta anomalías.
     * Este resultado se pasa al módulo de Anomalías (T-04).
     *
     * Reglas de detección:
     *  - Texto borroso / vacío                      → ALTA
     *  - Producto inexistente en OCR                → ALTA
     *  - Cantidad vacía en OCR                      → MEDIA
     *  - Nombre QR ≠ nombre OCR                     → ALTA
     *  - Cantidad QR ≠ cantidad OCR                 → MEDIA
     *  - Todo coincide                              → SIN_ANOMALIA
     */
    fun compararOcrConQr(ocrData: OcrData, qrData: QrData?): ResultadoComparacion {

        // Caso 1: El OCR no pudo leer nada (documento borroso o fuera de foco)
        if (ocrData.textoOriginal.isBlank()) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = "TEXTO_BORROSO",
                descripcion = "El OCR no pudo leer el documento. Intente mejorar el enfoque o la iluminación.",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Caso 2: No se detectó nombre de producto en el OCR
        if (ocrData.nombre.isBlank()) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = "PRODUCTO_INEXISTENTE",
                descripcion = "No se detectó nombre de producto en el documento OCR.",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Caso 3: No se detectó cantidad en el OCR
        if (ocrData.cantidad == 0) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = "CANTIDAD_VACIA",
                descripcion = "No se detectó cantidad en el documento. Verifique el formato del documento.",
                prioridad   = "MEDIA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Si no hay QR para comparar, no hay más anomalías que reportar
        if (qrData == null) {
            return ResultadoComparacion(
                hayAnomalia = false,
                tipo        = "SIN_ANOMALIA",
                descripcion = "OCR leído correctamente. Sin QR para comparar.",
                prioridad   = "BAJA",
                ocrData     = ocrData,
                qrData      = null
            )
        }

        // Caso 4: El nombre del producto no coincide entre QR y OCR
        val nombreOcrNorm = ocrData.nombre.trim().lowercase()
        val nombreQrNorm  = qrData.nombre.trim().lowercase()
        if (!nombreOcrNorm.contains(nombreQrNorm) && !nombreQrNorm.contains(nombreOcrNorm)) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = "QR_OCR_DIFERENTE",
                descripcion = "El producto del QR (\"${qrData.nombre}\") no coincide con el OCR (\"${ocrData.nombre}\").",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Caso 5: La cantidad no coincide entre QR y OCR
        if (qrData.cantidad != ocrData.cantidad) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = "QR_OCR_DIFERENTE",
                descripcion = "Cantidad QR: ${qrData.cantidad}  ≠  Cantidad OCR: ${ocrData.cantidad}.",
                prioridad   = "MEDIA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Todo coincide → sin anomalía
        return ResultadoComparacion(
            hayAnomalia = false,
            tipo        = "SIN_ANOMALIA",
            descripcion = "✅ QR y OCR coinciden correctamente.",
            prioridad   = "BAJA",
            ocrData     = ocrData,
            qrData      = qrData
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. CONVERTIR OcrData → Producto (para conectar con StockManager)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convierte un [OcrData] en un [com.example.logist_tech.models.Producto]
     * listo para ser registrado en el inventario (T-05/T-06).
     */
    fun ocrDataToProducto(
        ocrData: OcrData,
        tipoMovimiento: String = "ENTRADA"
    ): com.example.logist_tech.models.Producto {
        return com.example.logist_tech.models.Producto(
            id             = System.currentTimeMillis().toString(),
            nombre         = ocrData.nombre.ifBlank { "Desconocido" },
            cantidad       = ocrData.cantidad,
            pesoKg         = ocrData.pesoKg,
            categoria      = "General",
            destino        = ocrData.destino.ifBlank { "Sin destino" },
            estado         = if (ocrData.camposFaltantes.isEmpty()) "ok" else "incompleto",
            tipoMovimiento = tipoMovimiento,
            fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
        )
    }
}