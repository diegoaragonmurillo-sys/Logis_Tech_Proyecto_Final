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

    var selectedUbicacionId by remember { mutableStateOf("") }
    var expandedUbicaciones by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var estadoAConfirmar by remember { mutableStateOf("") }
    var showAddUbicDialog by remember { mutableStateOf(false) }
    var nuevoPasillo by remember { mutableStateOf("") }
    var nuevoEstante by remember { mutableStateOf("") }
    var nuevoNivel by remember { mutableStateOf("") }

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

        if (rol == SessionManager.Rol.BANDA) {
            viewModel.loadInitialData()
        }
    }

    // Diálogo de confirmación
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Acción") },
            text = { Text("¿Deseas cambiar la caja $codigoQr al estado ${estadoAConfirmar.replace("_", " ")}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.cambiarEstado(
                            codigoQr,
                            estadoAConfirmar,
                            if (estadoAConfirmar == "EN_ESTANTE") selectedUbicacionId else null,
                            onSuccess
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9))
                ) { Text("CONFIRMAR") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    // Diálogo agregar ubicación
    if (showAddUbicDialog) {
        AlertDialog(
            onDismissRequest = { showAddUbicDialog = false },
            title = { Text("Nueva Ubicación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nuevoPasillo, onValueChange = { nuevoPasillo = it }, label = { Text("Pasillo (ej: P1)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nuevoEstante, onValueChange = { nuevoEstante = it }, label = { Text("Estante (ej: 1)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nuevoNivel, onValueChange = { nuevoNivel = it }, label = { Text("Nivel (ej: 1)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val coord = "$nuevoPasillo-E$nuevoEstante-N$nuevoNivel"
                    viewModel.registrarUbicacion(coord, nuevoPasillo, nuevoEstante.toIntOrNull() ?: 0, nuevoNivel.toIntOrNull() ?: 0) {
                        showAddUbicDialog = false
                        nuevoPasillo = ""; nuevoEstante = ""; nuevoNivel = ""
                    }
                }) { Text("CREAR") }
            },
            dismissButton = { TextButton(onClick = { showAddUbicDialog = false }) { Text("CANCELAR") } }
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

                // ── Caja no encontrada ────────────────────────────────────────
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

                // ── Caja encontrada ───────────────────────────────────────────
                true -> {
                    val cajaData = caja
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Info de la caja
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
                                    InfoRow("Estado actual", cajaData.estado.replace("_", " "))
                                    if (!cajaData.id_ubicacion.isNullOrBlank()) {
                                        InfoRow("Ubicación", cajaData.id_ubicacion)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        when (rol) {
                            // ── RECEPTOR: solo puede confirmar ingreso ────────
                            SessionManager.Rol.RECEPTOR -> {
                                Text("Acción disponible", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Confirmar Recepción", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Valida el ingreso físico al almacén.", color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                } else {
                                    Button(
                                        onClick = { estadoAConfirmar = "RECEPCION_EN_ALMACEN"; showConfirmDialog = true },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9))
                                    ) { Text("CONFIRMAR INGRESO") }
                                }
                            }

                            // ── BANDA: puede mover la caja por todos los estados
                            SessionManager.Rol.BANDA -> {
                                Text("Acciones de Banda", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                                // Selector de ubicación para EN_ESTANTE
                                ExposedDropdownMenuBox(
                                    expanded = expandedUbicaciones,
                                    onExpandedChange = { expandedUbicaciones = !expandedUbicaciones }
                                ) {
                                    OutlinedTextField(
                                        value = if (selectedUbicacionId.isEmpty()) "Seleccionar Ubicación (para estante)" else selectedUbicacionId,
                                        onValueChange = {}, readOnly = true,
                                        label = { Text("Coordenada Física") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUbicaciones) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedUbicaciones,
                                        onDismissRequest = { expandedUbicaciones = false }
                                    ) {
                                        viewModel.ubicacionesDisponibles.forEach { ubic ->
                                            DropdownMenuItem(
                                                text = { Text(ubic.id_coordenada) },
                                                onClick = { selectedUbicacionId = ubic.id_coordenada; expandedUbicaciones = false }
                                            )
                                        }
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("+ Nueva Ubicación...", color = Color(0xFF2980B9), fontWeight = FontWeight.Bold) },
                                            onClick = { expandedUbicaciones = false; showAddUbicDialog = true }
                                        )
                                    }
                                }

                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                } else {
                                    // Botones para cada estado posible
                                    EstadoBoton("RECEPCION EN ALMACEN", Color(0xFFF59E0B)) {
                                        estadoAConfirmar = "RECEPCION_EN_ALMACEN"; showConfirmDialog = true
                                    }
                                    EstadoBoton("UBICAR EN ESTANTE", Color(0xFF10B981), enabled = selectedUbicacionId.isNotBlank()) {
                                        estadoAConfirmar = "EN_ESTANTE"; showConfirmDialog = true
                                    }
                                    EstadoBoton("SALIDA DE ESTANTE", Color(0xFF8B5CF6)) {
                                        estadoAConfirmar = "SALIDA_DE_ESTANTE"; showConfirmDialog = true
                                    }
                                    EstadoBoton("SALIENDO DE ALMACEN", Color(0xFFEC4899)) {
                                        estadoAConfirmar = "SALIENDO_DE_ALMACEN"; showConfirmDialog = true
                                    }
                                    EstadoBoton("MARCAR ENTREGADO", Color(0xFF64748B)) {
                                        estadoAConfirmar = "ENTREGADO"; showConfirmDialog = true
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cargando ──────────────────────────────────────────────────
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