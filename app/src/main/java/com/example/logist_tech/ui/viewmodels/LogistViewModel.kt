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

    var todasLasCajas by mutableStateOf<List<Caja>>(emptyList())
    var historialGlobal by mutableStateOf<List<HistorialMovimiento>>(emptyList())

    // Estado del reporte
    var reporte by mutableStateOf<ReporteResponse?>(null)
    var reporteLoading by mutableStateOf(false)
    var reporteError by mutableStateOf<String?>(null)

    fun loadInitialData() {
        viewModelScope.launch {
            isLoading = true
            try {
                loadTodasLasCajas()

                tiposCaja = emptyList()
                ubicacionesDisponibles = emptyList()

                val respTipos = RetrofitClient.api.getTiposCaja()
                if (respTipos.isSuccessful) {
                    val lista = respTipos.body() ?: emptyList()
                    if (lista.isEmpty()) {
                        registrarTipoCaja("Caja Estándar", 40.0, 40.0, 40.0) {}
                    } else {
                        tiposCaja = lista
                    }
                }

                val respUbic = RetrofitClient.api.getUbicacionesDisponibles()
                if (respUbic.isSuccessful) {
                    ubicacionesDisponibles = respUbic.body() ?: emptyList()
                }

            } catch (e: Exception) {
                message = "Error al cargar datos: ${e.message}"
            } finally {
                isLoading = false
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
            } catch (e: Exception) {
                Log.e("VM_DEBUG", "Error dashboard", e)
            }
        }
    }

    fun loadHistorialGlobal() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getHistorialGlobal()
                if (resp.isSuccessful) historialGlobal = resp.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("VM", "Error historial global", e)
            }
        }
    }

    fun loadReporte(periodo: String = "diario") {
        viewModelScope.launch {
            reporteLoading = true
            reporteError = null
            try {
                val resp = RetrofitClient.api.getReporte(periodo)
                if (resp.isSuccessful) {
                    reporte = resp.body()
                } else {
                    reporteError = "Error del servidor (${resp.code()})"
                }
            } catch (e: Exception) {
                reporteError = "No se pudo cargar el reporte: ${e.message}"
            } finally {
                reporteLoading = false
            }
        }
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
                val request = TipoCaja(id = 0, nombre = nombre, largo = largo, ancho = ancho, alto = alto, volumen = volumen)
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
                // id_cliente ya no va en el body — el backend lo saca del JWT
                val request = RegistroCajaRequest(
                    codigo_qr    = codigoQr,
                    producto     = producto,
                    cantidad     = cantidad,
                    peso_kg      = peso,
                    prioridad    = prioridad,
                    categoria    = categoria,
                    es_fragil    = if (esFragil) 1 else 0,
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
                // id_operador y tipo_operador ya no van en el body — el backend los saca del JWT
                val request = CambioEstadoRequest(
                    codigo_qr    = codigoQr,
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