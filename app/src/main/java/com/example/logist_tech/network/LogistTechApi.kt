package com.example.logist_tech.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class Caja(
    val codigo_qr: String = "",
    val id_operador: String = "",
    val producto: String = "",
    val cantidad: Int = 0,
    val peso_kg: Double = 0.0,
    val prioridad: String = "",
    val categoria: String = "",
    val es_fragil: Int = 0,
    val estado: String = "",
    val fecha_registro: String = "",
    val id_proveedor: String = "",
    val id_tipo_caja: String = "",
    val id_ubicacion: String? = ""
)

data class TipoCaja(
    val id: Int = 0,
    val nombre: String = "",
    val largo: Double = 0.0,
    val ancho: Double = 0.0,
    val alto: Double = 0.0,
    val volumen: Double = 0.0
)

data class Ubicacion(
    val id_coordenada: String = "",
    val pasillo: String = "",
    val estante: Int = 0,
    val nivel: Int = 0,
    val estado_ocupacion: Int = 0
)

data class HistorialMovimiento(
    val id: Int = 0,
    val id_caja: String = "",
    val producto: String = "",
    val id_operador: String = "",
    val tipo_operador: String = "",
    val estado_anterior: String = "",
    val estado_nuevo: String = "",
    val fecha_cambio: String = ""
)

data class RegistroCajaRequest(
    val codigo_qr: String,
    val producto: String,
    val cantidad: Int,
    val peso_kg: Double,
    val prioridad: String,
    val categoria: String,
    val es_fragil: Int,
    val id_proveedor: String,
    val id_tipo_caja: String
)

// El backend saca id_operador y tipo_operador del JWT — no hace falta en el body
data class CambioEstadoRequest(
    val codigo_qr: String,
    val nuevo_estado: String,
    val id_ubicacion: String? = ""
)

data class ApiResponse(
    val status: String,
    val message: String? = null
)

data class UbicacionRequest(
    val id_coordenada: String,
    val pasillo: String,
    val estante: Int,
    val nivel: Int
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val token: String? = null,
    val rol: String = "",
    val username: String? = null
)

// ── Modelos para el Reporte con Gemini ────────────────────────────────────────
data class ReporteDatos(
    val total_cajas: Int = 0,
    val entregadas: Int = 0,
    val pendientes: Int = 0,
    val cajas_por_estado: Map<String, Int> = emptyMap(),
    val top_operador: String = "",
    val operadores: List<Map<String, Any>> = emptyList(),
    val estantes_ocupados: Int = 0,
    val estantes_libres: Int = 0,
    val estantes_total: Int = 0,
    val en_proceso: Int = 0
)

data class ReporteResponse(
    val periodo: String = "",
    val fecha_inicio: String = "",
    val fecha_fin: String = "",
    val datos: ReporteDatos = ReporteDatos(),
    val analisis_ia: String = ""
)

interface LogistTechApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("tipos_caja")
    suspend fun getTiposCaja(): Response<List<TipoCaja>>

    @POST("tipos_caja")
    suspend fun registrarTipoCaja(@Body request: TipoCaja): Response<ApiResponse>

    @GET("ubicaciones/disponibles")
    suspend fun getUbicacionesDisponibles(): Response<List<Ubicacion>>

    @POST("ubicaciones")
    suspend fun registrarUbicacion(@Body request: UbicacionRequest): Response<ApiResponse>

    @POST("registrarCaja")
    suspend fun registrarCaja(@Body request: RegistroCajaRequest): Response<ApiResponse>

    @PUT("cambiarEstado")
    suspend fun cambiarEstado(@Body request: CambioEstadoRequest): Response<ApiResponse>

    @GET("cajas/todas")
    suspend fun getTodasLasCajas(): Response<List<Caja>>

    @GET("historial/todos")
    suspend fun getHistorialGlobal(): Response<List<HistorialMovimiento>>

    @GET("caja/{qr}")
    suspend fun getCaja(@Path("qr") qr: String): Response<Caja>

    @GET("reporte/general")
    suspend fun getReporte(@Query("periodo") periodo: String): Response<ReporteResponse>
}