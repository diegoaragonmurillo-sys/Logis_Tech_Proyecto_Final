package com.example.logist_tech.api

data class CajaRequest(
    val id: String,
    val producto: String,
    val cantidad: Int
)

data class CajaResponse(
    val id: String,
    val producto: String,
    val cantidad: Int
)

data class EstadoRequest(
    val id_ubicacion: Int,
    val estado_nuevo: String,
    val id_usuario: Int
)

data class DespachoRequest(
    val id_caja: String,
    val destino: String,
    val transporte_placa: String,
    val id_usuario_despacho: Int
)