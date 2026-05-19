package com.example.logist_tech.models

data class Producto(
    val id: String,
    val nombre: String,
    val cantidad: Int,
    val pesoKg: Double,
    val categoria: String,
    val destino: String,
    val estado: String,
    val tipoMovimiento: String,
    val fecha: String
)