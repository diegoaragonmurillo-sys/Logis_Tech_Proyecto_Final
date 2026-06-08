package com.example.logist_tech.anomalias

import com.example.logist_tech.ocr.ResultadoComparacion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * T-04 — AnomaliaManager
 * Gestiona el registro y consulta de anomalías detectadas.
 * Recibe un ResultadoComparacion del módulo OCR (T-03) y lo convierte en Anomalia.
 */
object AnomaliaManager {

    // Lista en memoria de anomalías registradas en esta sesión
    private val _anomalias = mutableListOf<Anomalia>()

    // Lista pública de solo lectura
    val anomalias: List<Anomalia> get() = _anomalias.toList()

    // ─────────────────────────────────────────────────────────────────
    // REGISTRAR ANOMALÍA desde ResultadoComparacion (T-03)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convierte un ResultadoComparacion en una Anomalia y la registra.
     * Solo registra si hay anomalía real.
     * Retorna la Anomalia creada, o null si no había anomalía.
     */
    fun registrarDesdeResultado(
        resultado: ResultadoComparacion,
        evidenciaUri: String = ""
    ): Anomalia? {
        if (!resultado.hayAnomalia) return null

        val anomalia = Anomalia(
            id               = System.currentTimeMillis().toString(),
            fecha            = obtenerFechaActual(),
            fechaHora        = obtenerFechaHoraActual(),
            idCaja           = resultado.qrData?.idCaja ?: "",
            tipo             = resultado.tipo,
            descripcion      = resultado.descripcion,
            prioridad        = resultado.prioridad,
            productoNombre   = resultado.ocrData.nombre.ifBlank { "Desconocido" },
            textoOcrOriginal = resultado.ocrData.textoOriginal,
            evidenciaUri     = evidenciaUri
        )
        _anomalias.add(0, anomalia) // más reciente primero
        return anomalia
    }

    // ─────────────────────────────────────────────────────────────────
    // FILTROS
    // ─────────────────────────────────────────────────────────────────

    /** Retorna solo las anomalías de prioridad ALTA. */
    fun obtenerAltas(): List<Anomalia> =
        _anomalias.filter { it.prioridad == "ALTA" }

    /** Retorna solo las anomalías de prioridad MEDIA. */
    fun obtenerMedias(): List<Anomalia> =
        _anomalias.filter { it.prioridad == "MEDIA" }

    /** Retorna solo las anomalías de prioridad BAJA. */
    fun obtenerBajas(): List<Anomalia> =
        _anomalias.filter { it.prioridad == "BAJA" }

    /** Filtra por prioridad. Si es null retorna todas. */
    fun filtrarPorPrioridad(prioridad: String?): List<Anomalia> =
        if (prioridad == null) _anomalias.toList()
        else _anomalias.filter { it.prioridad == prioridad }

    /** Total de anomalías registradas. */
    fun totalAnomalias(): Int = _anomalias.size

    /** Elimina todas las anomalías (para pruebas o reset). */
    fun limpiar() = _anomalias.clear()

    // ─────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────

    private fun obtenerFechaActual(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    private fun obtenerFechaHoraActual(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}