package com.example.logist_tech

import com.example.logist_tech.anomalias.AnomaliaManager
import com.example.logist_tech.anomalias.AnomaliaType
import com.example.logist_tech.ocr.OcrData
import com.example.logist_tech.ocr.OcrProcessor
import com.example.logist_tech.ocr.QrData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    @Before
    fun setup() {
        AnomaliaManager.limpiar()
    }

    @Test
    fun testSinAnomalia() {
        val ocrData = OcrData("Laptop", 10, 1.5, "Lima", "Producto: Laptop\nCantidad: 10\nPeso: 1.5\nDestino: Lima", emptyList())
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertFalse(resultado.hayAnomalia)
        assertEquals(AnomaliaType.SIN_ANOMALIA, resultado.tipo)
    }

    @Test
    fun testTextoBorroso() {
        val ocrData = OcrData("", 0, 0.0, "", "", emptyList())
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.TEXTO_BORROSO, resultado.tipo)
    }

    @Test
    fun testDatosIncompletosEnQr() {
        val ocrData = OcrData("Laptop", 10, 1.5, "Lima", "Producto: Laptop\nCantidad: 10", emptyList())
        // QR con datos incompletos (id vacío)
        val qrData = QrData("", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.DATOS_INCOMPLETOS, resultado.tipo)
    }

    @Test
    fun testCajaNoRegistradaEnApi() {
        val ocrData = OcrData("Laptop", 10, 1.5, "Lima", "Producto: Laptop\nCantidad: 10", emptyList())
        // ID que no empieza con "CJ-" y no está en la lista de prueba
        val qrData = QrData("INVALID_BOX_ID", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.CAJA_NO_REGISTRADA_EN_API, resultado.tipo)
    }

    @Test
    fun testProductoInexistenteEnOcr() {
        val ocrData = OcrData("", 10, 1.5, "Lima", "Cantidad: 10", listOf("nombre"))
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.PRODUCTO_INEXISTENTE, resultado.tipo)
    }

    @Test
    fun testCantidadErroneaEnOcr() {
        val ocrData = OcrData("Laptop", 0, 1.5, "Lima", "Producto: Laptop", listOf("cantidad"))
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.CANTIDAD_ERRONEA, resultado.tipo)
    }

    @Test
    fun testQrOcrDiferenteNombre() {
        val ocrData = OcrData("Celular", 10, 1.5, "Lima", "Producto: Celular\nCantidad: 10", emptyList())
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.QR_OCR_DIFERENTE, resultado.tipo)
    }

    @Test
    fun testCantidadErroneaDiferenteCantidad() {
        val ocrData = OcrData("Laptop", 5, 1.5, "Lima", "Producto: Laptop\nCantidad: 5", emptyList())
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        assertEquals(AnomaliaType.CANTIDAD_ERRONEA, resultado.tipo)
    }

    @Test
    fun testRegistroDeAnomaliaTrazabilidad() {
        val ocrData = OcrData("Laptop", 5, 1.5, "Lima", "Producto: Laptop\nCantidad: 5", emptyList())
        val qrData = QrData("CJ-001", "Laptop", 10)
        
        val resultado = OcrProcessor.compararOcrConQr(ocrData, qrData)
        assertTrue(resultado.hayAnomalia)
        
        val anomaliaRegistrada = AnomaliaManager.registrarDesdeResultado(resultado, "mock/uri")
        assertNotNull(anomaliaRegistrada)
        assertEquals("CJ-001", anomaliaRegistrada!!.idCaja)
        assertEquals(AnomaliaType.CANTIDAD_ERRONEA, anomaliaRegistrada.tipo)
        assertTrue(anomaliaRegistrada.fechaHora.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
        assertEquals(1, AnomaliaManager.totalAnomalias())
    }
}