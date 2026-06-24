package com.example.logist_tech.auth

object SessionManager {

    enum class Rol { BANDA, RECEPTOR }

    var token: String = ""
        private set
    var usuarioId: String = ""
        private set
    var nombreUsuario: String = ""
        private set
    var rol: Rol = Rol.RECEPTOR
        private set

    fun login(token: String, username: String, rol: Rol) {
        this.token = token
        usuarioId = username.trim()
        nombreUsuario = username.trim()
        this.rol = rol
    }

    fun logout() {
        token = ""
        usuarioId = ""
        nombreUsuario = ""
        rol = Rol.RECEPTOR
    }

    fun estaLogueado(): Boolean = token.isNotEmpty()
}