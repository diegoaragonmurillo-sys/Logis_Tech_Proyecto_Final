package com.example.logist_tech.anomalias

/**
 * Tarea C-01 — AnomaliaType
 * Constantes bien documentadas que definen los tipos de anomalías posibles
 * en el flujo de inspección de la faja transportadora.
 */
object AnomaliaType {
    /** Todo coincide correctamente. No hay discrepancias. */
    const val SIN_ANOMALIA = "SIN_ANOMALIA"

    /** El nombre del producto o la cantidad difieren entre QR y OCR. */
    const val QR_OCR_DIFERENTE = "QR_OCR_DIFERENTE"

    /** La cantidad leída por el OCR es inválida (0 o menor) o difiere del QR. */
    const val CANTIDAD_ERRONEA = "CANTIDAD_ERRONEA"

    /** No se detectó ningún nombre de producto en el OCR. */
    const val PRODUCTO_INEXISTENTE = "PRODUCTO_INEXISTENTE"

    /** El texto capturado por el OCR está vacío o es completamente ilegible. */
    const val TEXTO_BORROSO = "TEXTO_BORROSO"

    /** El QR o el OCR no traen los campos obligatorios (id/idCaja, producto, cantidad, etc.). */
    const val DATOS_INCOMPLETOS = "DATOS_INCOMPLETOS"

    /** El identificador de la caja no está registrado en la base de datos de la API (Error 404). */
    const val CAJA_NO_REGISTRADA_EN_API = "CAJA_NO_REGISTRADA_EN_API"
}
