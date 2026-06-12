package com.example.logist_tech.ocr

import com.example.logist_tech.anomalias.AnomaliaType
import com.example.logist_tech.models.Producto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lógica principal del módulo OCR/QR.
 *
 * BÚSQUEDA FLEXIBLE: los campos pueden aparecer en CUALQUIER ORDEN.
 * Si un campo obligatorio no está  → anomalía DATOS_INCOMPLETOS.
 * Si un campo opcional no está     → se deja vacío/0, sin penalización.
 *
 * Campos OBLIGATORIOS : ID (solo QR), Producto, Cantidad, Destino
 * Campos OPCIONALES   : Peso, Categoria, Movimiento, Fecha
 */
object OcrProcessor {

    // ─────────────────────────────────────────────────────────────────
    // 1. PARSEAR TEXTO OCR → OcrData
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extrae campos logísticos del texto crudo del OCR.
     * Los campos pueden estar en cualquier orden dentro del documento.
     */
    fun parsearTextoOcr(textoOcr: String): OcrData {
        val lineas = textoOcr.lines().map { it.trim() }.filter { it.isNotBlank() }

        val nombre         = extraerCampoTexto(lineas, listOf("producto", "nombre", "item"))
        val cantidad       = extraerCampoNumeroEntero(lineas, listOf("cantidad", "qty", "unidades"))
        val pesoKg         = extraerCampoNumeroDecimal(lineas, listOf("peso", "kg", "weight"))
        val categoria      = extraerCampoTexto(lineas, listOf("categoria", "category", "tipo"))
        val destino        = extraerCampoTexto(lineas, listOf("destino", "destination", "para"))
        val tipoMovimiento = extraerCampoTexto(lineas, listOf("movimiento", "movement", "tipo_mov"))
        val fecha          = extraerCampoTexto(lineas, listOf("fecha", "date"))

        // Solo obligatorios generan anomalía
        val camposFaltantes = mutableListOf<String>()
        if (nombre.isBlank())  camposFaltantes.add("producto")
        if (cantidad == 0)     camposFaltantes.add("cantidad")
        if (destino.isBlank()) camposFaltantes.add("destino")

        return OcrData(
            nombre         = nombre,
            cantidad       = cantidad,
            pesoKg         = pesoKg,
            destino        = destino,
            categoria      = categoria,
            tipoMovimiento = tipoMovimiento,
            fecha          = fecha,
            textoOriginal  = textoOcr,
            camposFaltantes = camposFaltantes
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. PARSEAR TEXTO PLANO DEL QR → QrData
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parsea el texto plano del QR. Los campos pueden estar en cualquier orden.
     *
     * Ejemplo de QR válido:
     *   Destino: Lima
     *   ID: CJ-001
     *   Cantidad: 10
     *   Producto: Laptop
     *   Fecha: 2026-06-11
     *   Movimiento: ENTRADA
     *   Categoria: Electronica
     *   Peso: 1.5
     *
     * Retorna null si no se encontró ningún campo reconocible.
     */
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

        // Si no se reconoció ningún campo, el QR no tiene formato válido
        if (idCaja.isBlank() && nombre.isBlank() && cantidad == 0 && destino.isBlank()) return null

        return QrData(
            idCaja = idCaja,
            nombre = nombre,
            cantidad = cantidad,
            destino = destino,
            pesoKg = pesoKg,
            categoria = categoria,
            tipoMovimiento = tipoMovimiento,
            fecha = fecha,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. FUNCIONES AUXILIARES DE EXTRACCIÓN (búsqueda flexible)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Busca en CUALQUIER línea que contenga la clave seguida de ":"
     * y extrae el valor. No importa el orden ni si hay texto antes de la clave.
     * Ejemplo: "03 - Producto: Laptop" → "Laptop"
     */
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

    /** Extrae un número entero del valor del campo. */
    private fun extraerCampoNumeroEntero(lineas: List<String>, claves: List<String>): Int {
        val texto = extraerCampoTexto(lineas, claves)
        return Regex("\\d+").find(texto)?.value?.toIntOrNull() ?: 0
    }

    /** Extrae un número decimal del valor del campo. */
    private fun extraerCampoNumeroDecimal(lineas: List<String>, claves: List<String>): Double {
        val texto = extraerCampoTexto(lineas, claves)
        return Regex("\\d+([.,]\\d+)?").find(texto)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. VALIDACIÓN API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Valida si el ID de la caja está registrado en la API.
     * NOTA: Reemplazar por llamada real a GET http://38.250.116.214/api/v1/cajas/{id}
     */
    fun esCajaRegistradaEnApi(idCaja: String): Boolean {
        if (idCaja.isBlank()) return false
        val listaPrueba = setOf("1", "2", "3", "4", "BOX-001", "BOX-002")
        return idCaja.startsWith("CJ-", ignoreCase = true) || idCaja in listaPrueba
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. COMPARAR OCR vs QR → ResultadoComparacion
    // ─────────────────────────────────────────────────────────────────

    fun compararOcrConQr(ocrData: OcrData, qrData: QrData?): ResultadoComparacion {

        // OCR vacío → modo QR puro
        if (ocrData.textoOriginal.isBlank()) {
            if (qrData != null) {
                val faltantes = mutableListOf<String>()
                if (qrData.idCaja.isBlank())  faltantes.add("id")
                if (qrData.nombre.isBlank())  faltantes.add("producto")
                if (qrData.cantidad <= 0)     faltantes.add("cantidad")
                if (qrData.destino.isBlank()) faltantes.add("destino")

                if (faltantes.isNotEmpty()) {
                    return ResultadoComparacion(
                        hayAnomalia = true,
                        tipo        = AnomaliaType.DATOS_INCOMPLETOS,
                        descripcion = "Faltan: ${faltantes.joinToString(", ")}.",
                        prioridad   = "ALTA",
                        ocrData     = ocrData,
                        qrData      = qrData
                    )
                }
                if (!esCajaRegistradaEnApi(qrData.idCaja)) {
                    return ResultadoComparacion(
                        hayAnomalia = true,
                        tipo        = AnomaliaType.CAJA_NO_REGISTRADA_EN_API,
                        descripcion = "La caja '${qrData.idCaja}' no está registrada en la API.",
                        prioridad   = "ALTA",
                        ocrData     = ocrData,
                        qrData      = qrData
                    )
                }
                return ResultadoComparacion(
                    hayAnomalia = false,
                    tipo        = AnomaliaType.SIN_ANOMALIA,
                    descripcion = "✅ QR leído correctamente. Todos los campos presentes.",
                    prioridad   = "BAJA",
                    ocrData     = ocrData,
                    qrData      = qrData
                )
            }
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = AnomaliaType.TEXTO_BORROSO,
                descripcion = "El OCR no pudo leer el documento. Mejora el enfoque o la iluminación.",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = null
            )
        }

        // Campos obligatorios faltantes en OCR
        if (ocrData.camposFaltantes.isNotEmpty()) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = AnomaliaType.DATOS_INCOMPLETOS,
                descripcion = "Faltan: ${ocrData.camposFaltantes.joinToString(", ")}.",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // QR con campos obligatorios faltantes
        val qrIncompleto = qrData != null &&
                (qrData.idCaja.isBlank() || qrData.nombre.isBlank() || qrData.cantidad <= 0)
        if (qrIncompleto) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = AnomaliaType.DATOS_INCOMPLETOS,
                descripcion = "Los datos del QR están incompletos (requiere id, producto y cantidad).",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // ID de caja no registrado en la API
        if (qrData != null && !esCajaRegistradaEnApi(qrData.idCaja)) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = AnomaliaType.CAJA_NO_REGISTRADA_EN_API,
                descripcion = "La caja '${qrData.idCaja}' no está registrada en la API.",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Sin QR → OCR solo
        if (qrData == null) {
            return ResultadoComparacion(
                hayAnomalia = false,
                tipo        = AnomaliaType.SIN_ANOMALIA,
                descripcion = "OCR leído correctamente. Sin QR para comparar.",
                prioridad   = "BAJA",
                ocrData     = ocrData,
                qrData      = null
            )
        }

        // Nombre no coincide entre QR y OCR
        val nombreOcrNorm = ocrData.nombre.trim().lowercase()
        val nombreQrNorm  = qrData.nombre.trim().lowercase()
        if (!nombreOcrNorm.contains(nombreQrNorm) && !nombreQrNorm.contains(nombreOcrNorm)) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = AnomaliaType.QR_OCR_DIFERENTE,
                descripcion = "Producto QR: \"${qrData.nombre}\" ≠ OCR: \"${ocrData.nombre}\".",
                prioridad   = "ALTA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Cantidad diferente entre QR y OCR
        if (qrData.cantidad != ocrData.cantidad) {
            return ResultadoComparacion(
                hayAnomalia = true,
                tipo        = AnomaliaType.CANTIDAD_ERRONEA,
                descripcion = "Cantidad QR: ${qrData.cantidad} ≠ OCR: ${ocrData.cantidad}.",
                prioridad   = "MEDIA",
                ocrData     = ocrData,
                qrData      = qrData
            )
        }

        // Todo OK
        return ResultadoComparacion(
            hayAnomalia = false,
            tipo        = AnomaliaType.SIN_ANOMALIA,
            descripcion = "✅ QR y OCR coinciden correctamente.",
            prioridad   = "BAJA",
            ocrData     = ocrData,
            qrData      = qrData
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 6. CONVERTIR OcrData → Producto
    // ─────────────────────────────────────────────────────────────────

    fun ocrDataToProducto(
        ocrData: OcrData,
        qrData: QrData? = null,
        tipoMovimiento: String = "ENTRADA"
    ): Producto {
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())

        return Producto(
            id             = qrData?.idCaja?.ifBlank { System.currentTimeMillis().toString() }
                ?: System.currentTimeMillis().toString(),
            nombre         = ocrData.nombre.ifBlank        { qrData?.nombre        ?: "Desconocido" },
            cantidad       = if (ocrData.cantidad > 0)       ocrData.cantidad       else (qrData?.cantidad ?: 0),
            pesoKg         = if (ocrData.pesoKg > 0.0)       ocrData.pesoKg         else (qrData?.pesoKg   ?: 0.0),
            categoria      = ocrData.categoria.ifBlank      { qrData?.categoria     ?: "General" },
            destino        = ocrData.destino.ifBlank        { qrData?.destino       ?: "Sin destino" },
            estado         = if (ocrData.camposFaltantes.isEmpty()) "ok" else "incompleto",
            tipoMovimiento = ocrData.tipoMovimiento.ifBlank { qrData?.tipoMovimiento?.ifBlank { tipoMovimiento } ?: tipoMovimiento },
            fecha          = ocrData.fecha.ifBlank          { qrData?.fecha?.ifBlank { fechaHoy } ?: fechaHoy }
        )
    }
}