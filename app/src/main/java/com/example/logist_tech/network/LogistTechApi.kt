package com.example.logist_tech.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class Caja(
    val codigo_qr: String = "",
    val id_cliente: String = "",
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

data class Notificacion(
    val id: Int = 0,
    val id_caja: String = "",
    val mensaje_enviado: String = "",
    val fecha_envio: String = "",
    val estado_envio: String = ""
)

data class HistorialMovimiento(
    val id: Int = 0,
    val id_caja: String = "",
    val producto: String = "", // Cruce con tabla caja
    val id_operador: String = "",
    val tipo_operador: String = "",
    val estado_anterior: String = "",
    val estado_nuevo: String = "",
    val fecha_cambio: String = ""
)

data class RegistroCajaRequest(
    val codigo_qr: String,
    val id_cliente: String,
    val producto: String,
    val cantidad: Int,
    val peso_kg: Double,
    val prioridad: String,
    val categoria: String,
    val es_fragil: Int,
    val id_proveedor: String,
    val id_tipo_caja: String
)

data class CambioEstadoRequest(
    val codigo_qr: String,
    val nuevo_estado: String,
    val id_operador: String,
    val tipo_operador: String,
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

    @GET("cajas/cliente/{usuario_id}")
    suspend fun getCajasPorCliente(@Path("usuario_id") usuarioId: String): Response<List<Caja>>

    @GET("notificaciones/{usuario_id}")
    suspend fun getNotificaciones(@Path("usuario_id") usuarioId: String): Response<List<Notificacion>>

    @GET("historial/operador/{usuario_id}")
    suspend fun getHistorialOperador(@Path("usuario_id") usuarioId: String): Response<List<HistorialMovimiento>>

    @GET("historial/todos")
    suspend fun getHistorialGlobal(): Response<List<HistorialMovimiento>>

    @GET("caja/{qr}")
    suspend fun getCaja(@Path("qr") qr: String): Response<Caja>
}
