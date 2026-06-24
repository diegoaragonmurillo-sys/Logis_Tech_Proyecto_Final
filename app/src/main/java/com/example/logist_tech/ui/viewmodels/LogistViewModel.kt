package com.example.logist_tech.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.logist_tech.auth.SessionManager
import com.example.logist_tech.network.*
import kotlinx.coroutines.launch

class LogistViewModel : ViewModel() {

    var tiposCaja by mutableStateOf<List<TipoCaja>>(emptyList())
    var ubicacionesDisponibles by mutableStateOf<List<Ubicacion>>(emptyList())
    var isLoading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var ultimaNotificacion by mutableStateOf<String?>(null)
    var listaNotificaciones by mutableStateOf<List<Notificacion>>(emptyList())

    var misCajas by mutableStateOf<List<Caja>>(emptyList())
    var todasLasCajas by mutableStateOf<List<Caja>>(emptyList())
    var miHistorialOperaciones by mutableStateOf<List<HistorialMovimiento>>(emptyList())
    var historialGlobal by mutableStateOf<List<HistorialMovimiento>>(emptyList())

    fun loadInitialData() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Sin cliente: todos los roles son de operación
                loadTodasLasCajas()
                loadHistorialOperador()

                // Limpiar listas para forzar refresco visual
                tiposCaja = emptyList()
                ubicacionesDisponibles = emptyList()

                val respTipos = RetrofitClient.api.getTiposCaja()
                if (respTipos.isSuccessful) {
                    val lista = respTipos.body() ?: emptyList()
                    if (lista.isEmpty()) {
                        Log.d("VM", "Lista vacía, intentando registro automático de prueba...")
                        registrarTipoCaja("Caja Estándar AI", 40.0, 40.0, 40.0) {}
                    } else {
                        tiposCaja = lista
                    }
                }

                val respUbic = RetrofitClient.api.getUbicacionesDisponibles()
                if (respUbic.isSuccessful) {
                    ubicacionesDisponibles = respUbic.body() ?: emptyList()
                }

                loadNotificaciones()
            } catch (e: Exception) {
                message = "Error al cargar datos: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMisCajas() {
        viewModelScope.launch {
            try {
                val userId = SessionManager.usuarioId.lowercase().trim()
                Log.d("VM_DEBUG", "Solicitando cajas para: '$userId'")

                val resp = RetrofitClient.api.getCajasPorCliente(userId)
                if (resp.isSuccessful) {
                    val lista = resp.body() ?: emptyList()
                    Log.d("VM_DEBUG", "SERVIDOR RESPONDIÓ OK. Cajas encontradas: ${lista.size}")
                    misCajas = emptyList()
                    misCajas = lista
                } else {
                    Log.e("VM_DEBUG", "ERROR SERVIDOR: ${resp.code()} - ${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("VM_DEBUG", "FALLO DE RED O PARSEO", e)
            }
        }
    }

    fun loadTodasLasCajas() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getTodasLasCajas()
                if (resp.isSuccessful) {
                    todasLasCajas = emptyList()
                    todasLasCajas = resp.body() ?: emptyList()
                    Log.d("VM_DEBUG", "Dashboard actualizado: ${todasLasCajas.size} cajas")
                }
            } catch (e: Exception) { Log.e("VM_DEBUG", "Error dashboard", e) }
        }
    }

    fun loadHistorialOperador() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getHistorialOperador(SessionManager.usuarioId)
                if (resp.isSuccessful) miHistorialOperaciones = resp.body() ?: emptyList()
            } catch (e: Exception) { Log.e("VM", "Error historial", e) }
        }
    }

    fun loadHistorialGlobal() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getHistorialGlobal()
                if (resp.isSuccessful) historialGlobal = resp.body() ?: emptyList()
            } catch (e: Exception) { Log.e("VM", "Error historial global", e) }
        }
    }

    fun loadNotificaciones() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getNotificaciones(SessionManager.usuarioId)
                if (response.isSuccessful) {
                    listaNotificaciones = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error cargando notificaciones", e)
            }
        }
    }

    fun registrarTokenFCM(token: String) {
        // No usar Firebase
    }

    fun listenToNotifications() {
        WebSocketManager.connect(SessionManager.usuarioId) { texto ->
            ultimaNotificacion = texto
        }
    }

    fun registrarTipoCaja(
        nombre: String,
        largo: Double,
        ancho: Double,
        alto: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                val volumen = largo * ancho * alto
                val request = TipoCaja(
                    id = 0,
                    nombre = nombre,
                    largo = largo,
                    ancho = ancho,
                    alto = alto,
                    volumen = volumen
                )
                val response = RetrofitClient.api.registrarTipoCaja(request)
                if (response.isSuccessful) {
                    loadInitialData()
                    onSuccess()
                } else {
                    message = "Error al crear tipo: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                message = "Error de red: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun registrarCaja(
        codigoQr: String,
        producto: String,
        cantidad: Int,
        peso: Double,
        prioridad: String,
        categoria: String,
        esFragil: Boolean,
        idProveedor: String,
        idTipoCaja: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = RegistroCajaRequest(
                    codigo_qr = codigoQr,
                    id_cliente = SessionManager.usuarioId,
                    producto = producto,
                    cantidad = cantidad,
                    peso_kg = peso,
                    prioridad = prioridad,
                    categoria = categoria,
                    es_fragil = if (esFragil) 1 else 0,
                    id_proveedor = idProveedor,
                    id_tipo_caja = idTipoCaja
                )
                val response = RetrofitClient.api.registrarCaja(request)
                if (response.isSuccessful) {
                    loadInitialData()
                    onSuccess()
                } else {
                    message = "Error al registrar: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                message = "Error de red: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun registrarUbicacion(
        idCoordenada: String,
        pasillo: String,
        estante: Int,
        nivel: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = UbicacionRequest(idCoordenada, pasillo, estante, nivel)
                val response = RetrofitClient.api.registrarUbicacion(request)
                if (response.isSuccessful) {
                    loadInitialData()
                    onSuccess()
                } else {
                    message = "Error: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                message = "Error de red: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun cambiarEstado(
        codigoQr: String,
        nuevoEstado: String,
        idUbicacion: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                val request = CambioEstadoRequest(
                    codigo_qr = codigoQr,
                    id_operador = SessionManager.usuarioId,
                    tipo_operador = SessionManager.rol.name,
                    nuevo_estado = nuevoEstado,
                    id_ubicacion = idUbicacion
                )
                val response = RetrofitClient.api.cambiarEstado(request)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    message = "Error: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                message = "Error de red: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}