package com.example.logist_tech.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logist_tech.auth.SessionManager
import com.example.logist_tech.network.Caja
import com.example.logist_tech.network.RetrofitClient
import com.example.logist_tech.ui.viewmodels.LogistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionCajaScreen(
    codigoQr: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LogistViewModel = viewModel()
) {
    val rol = SessionManager.rol
    var caja by remember { mutableStateOf<Caja?>(null) }
    var cajaEncontrada by remember { mutableStateOf<Boolean?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var estadoAConfirmar by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getCaja(codigoQr)
            if (response.isSuccessful && response.body() != null) {
                caja = response.body()
                cajaEncontrada = true
            } else {
                cajaEncontrada = false
            }
        } catch (e: Exception) {
            cajaEncontrada = false
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar acción") },
            text = {
                Text("¿Cambiar la caja $codigoQr al estado ${estadoAConfirmar.replace("_", " ")}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.cambiarEstado(codigoQr, estadoAConfirmar, null, onSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9))
                ) { Text("CONFIRMAR") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Caja") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (cajaEncontrada) {

                false -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(100.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("¡CAJA NO REGISTRADA!", color = Color(0xFFC62828), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("La caja \"$codigoQr\" no existe en el sistema.", textAlign = TextAlign.Center, color = Color.DarkGray)
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("REGRESAR") }
                    }
                }

                true -> {
                    val cajaData = caja
                    val estadoActual = cajaData?.estado ?: ""

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Info de la caja ───────────────────────────
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FB))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Caja Identificada", fontSize = 12.sp, color = Color.Gray)
                                Text(codigoQr, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2980B9))
                                if (cajaData != null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    InfoRow("Producto", cajaData.producto)
                                    InfoRow("Cantidad", cajaData.cantidad.toString())
                                    InfoRow("Peso", "${cajaData.peso_kg} kg")
                                    InfoRow("Estado actual", estadoActual.replace("_", " "))
                                }
                            }
                        }

                        // ── Badge estado con color ────────────────────
                        EstadoBadge(estadoActual)

                        HorizontalDivider()

                        when (rol) {
                            // ── RECEPTOR ─────────────────────────────
                            SessionManager.Rol.RECEPTOR -> {
                                if (estadoActual == "REGISTRADO") {
                                    Text("Acción disponible", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (viewModel.isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                    } else {
                                        EstadoBoton("CONFIRMAR RECEPCIÓN", Color(0xFF2980B9)) {
                                            estadoAConfirmar = "RECEPCION_EN_ALMACEN"
                                            showConfirmDialog = true
                                        }
                                    }
                                } else {
                                    MensajeInfo("Esta caja ya fue recibida y está en estado: ${estadoActual.replace("_", " ")}")
                                }
                            }

                            // ── BANDA ─────────────────────────────────
                            SessionManager.Rol.BANDA -> {
                                Text("Acciones de Banda", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                } else {
                                    when (estadoActual) {
                                        "REGISTRADO" -> {
                                            // BANDA puede confirmar recepción directamente
                                            EstadoBoton("CONFIRMAR RECEPCIÓN EN ALMACÉN", Color(0xFFF59E0B)) {
                                                estadoAConfirmar = "RECEPCION_EN_ALMACEN"
                                                showConfirmDialog = true
                                            }
                                        }
                                        "RECEPCION_EN_ALMACEN" -> {
                                            EstadoBoton("CONFIRMAR EN ESTANTE", Color(0xFF10B981)) {
                                                estadoAConfirmar = "EN_ESTANTE"
                                                showConfirmDialog = true
                                            }
                                        }
                                        "EN_ESTANTE" -> {
                                            EstadoBoton("PREPARAR SALIDA", Color(0xFF8B5CF6)) {
                                                estadoAConfirmar = "SALIDA_DE_ESTANTE"
                                                showConfirmDialog = true
                                            }
                                        }
                                        "SALIDA_DE_ESTANTE" -> {
                                            EstadoBoton("CONFIRMAR SALIDA DE ALMACÉN", Color(0xFFEC4899)) {
                                                estadoAConfirmar = "SALIENDO_DE_ALMACEN"
                                                showConfirmDialog = true
                                            }
                                        }
                                        "SALIENDO_DE_ALMACEN" -> {
                                            EstadoBoton("MARCAR COMO ENTREGADO", Color(0xFF64748B)) {
                                                estadoAConfirmar = "ENTREGADO"
                                                showConfirmDialog = true
                                            }
                                        }
                                        "ENTREGADO" -> {
                                            MensajeInfo("✓ Esta caja ya fue entregada exitosamente.")
                                        }
                                        else -> {
                                            MensajeInfo("Estado desconocido: $estadoActual")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2980B9))
                        Spacer(Modifier.height(12.dp))
                        Text("Verificando caja...", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoBadge(estado: String) {
    val color = when (estado) {
        "REGISTRADO"            -> Color(0xFF3B82F6)
        "RECEPCION_EN_ALMACEN"  -> Color(0xFFF59E0B)
        "EN_ESTANTE"            -> Color(0xFF10B981)
        "SALIDA_DE_ESTANTE"     -> Color(0xFF8B5CF6)
        "SALIENDO_DE_ALMACEN"   -> Color(0xFFEC4899)
        "ENTREGADO"             -> Color(0xFF64748B)
        else                    -> Color.Gray
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = "→ Siguiente acción requerida",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MensajeInfo(texto: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = Color(0xFF2980B9), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto, fontSize = 14.sp, color = Color(0xFF1E293B))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EstadoBoton(texto: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) { Text(texto, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}