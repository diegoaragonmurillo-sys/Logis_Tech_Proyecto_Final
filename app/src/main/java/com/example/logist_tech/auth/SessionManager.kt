package com.example.logist_tech.auth

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    enum class Rol { BANDA, RECEPTOR }

    private const val PREFS_NAME = "logistech_session"
    private const val KEY_TOKEN    = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROL      = "rol"

    private var prefs: SharedPreferences? = null

    // Llamar una sola vez desde MainActivity.onCreate()
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var token: String
        get() = prefs?.getString(KEY_TOKEN, "") ?: ""
        private set(value) { prefs?.edit()?.putString(KEY_TOKEN, value)?.apply() }

    var usuarioId: String
        get() = prefs?.getString(KEY_USERNAME, "") ?: ""
        private set(value) { prefs?.edit()?.putString(KEY_USERNAME, value)?.apply() }

    var nombreUsuario: String
        get() = usuarioId
        private set(_) {}

    var rol: Rol
        get() = try {
            Rol.valueOf(prefs?.getString(KEY_ROL, "RECEPTOR") ?: "RECEPTOR")
        } catch (e: Exception) { Rol.RECEPTOR }
        private set(value) { prefs?.edit()?.putString(KEY_ROL, value.name)?.apply() }

    fun login(token: String, username: String, rol: Rol) {
        this.token    = token
        this.usuarioId = username.trim()
        this.rol      = rol
    }

    fun logout() {
        prefs?.edit()?.clear()?.apply()
    }

    fun estaLogueado(): Boolean = token.isNotEmpty()
}