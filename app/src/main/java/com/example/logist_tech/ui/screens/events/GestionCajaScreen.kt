package com.example.logist_tech.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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
    var selectedUbicacionId by remember { mutableStateOf("") }
    var expandedUbicaciones by remember { mutableStateOf(false) }

    var cajaExiste by remember { mutableStateOf<Boolean?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var estadoAConfirmar by remember { mutableStateOf("") }

    var showAddUbicDialog by remember { mutableStateOf(false) }
    var nuevoPasillo by remember { mutableStateOf("") }
    var nuevoEstante by remember { mutableStateOf("") }
    var nuevoNivel by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getTodasLasCajas()
            if (response.isSuccessful) {
                cajaExiste = response.body()?.any { it.codigo_qr == codigoQr } ?: false
            } else { cajaExiste = false }
        } catch (e: Exception) { cajaExiste = false }

        if (rol == SessionManager.Rol.BANDA) {
            viewModel.loadInitialData()
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Acción") },
            text = { Text("¿Deseas pasar la caja $codigoQr al estado ${estadoAConfirmar.replace("_", " ")}?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    viewModel.cambiarEstado(codigoQr, estadoAConfirmar, if (estadoAConfirmar == "EN_ESTANTE") selectedUbicacionId else null, onSuccess)
                }) { Text("CONFIRMAR") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showAddUbicDialog) {
        AlertDialog(
            onDismissRequest = { showAddUbicDialog = false },
            title = { Text("Nueva Ubicación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nuevoPasillo, onValueChange = { nuevoPasillo = it }, label = { Text("Pasillo") })
                    OutlinedTextField(value = nuevoEstante, onValueChange = { nuevoEstante = it }, label = { Text("Estante") })
                    OutlinedTextField(value = nuevoNivel, onValueChange = { nuevoNivel = it }, label = { Text("Nivel") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val coord = "P$nuevoPasillo-E$nuevoEstante-N$nuevoNivel"
                    viewModel.registrarUbicacion(coord, nuevoPasillo, nuevoEstante.toIntOrNull() ?: 0, nuevoNivel.toIntOrNull() ?: 0) {
                        showAddUbicDialog = false
                    }
                }) { Text("CREAR") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gestión de Operación") }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (cajaExiste) {
                false -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(100.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("¡ERROR DE SISTEMA!", color = Color(0xFFC62828), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("La caja con ID \"$codigoQr\" no se encuentra registrada.", textAlign = TextAlign.Center, color = Color.DarkGray)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), modifier = Modifier.fillMaxWidth()) {
                            Text("ENTENDIDO - REGRESAR")
                        }
                    }
                }
                true -> {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Caja Identificada: $codigoQr", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2980B9))
                        HorizontalDivider()

                        when (rol) {
                            SessionManager.Rol.RECEPTOR -> {
                                InfoEvento(
                                    "Recepción",
                                    "Valida el ingreso físico al almacén.",
                                    "CONFIRMAR INGRESO",
                                    { estadoAConfirmar = "RECEPCION_EN_ALMACEN"; showConfirmDialog = true },
                                    viewModel.isLoading
                                )
                            }
                            SessionManager.Rol.BANDA -> {
                                Text("Asignar a Estante", style = MaterialTheme.typography.titleLarge)
                                ExposedDropdownMenuBox(
                                    expanded = expandedUbicaciones,
                                    onExpandedChange = { expandedUbicaciones = !expandedUbicaciones }
                                ) {
                                    OutlinedTextField(
                                        value = if (selectedUbicacionId.isEmpty()) "Seleccionar Ubicación" else selectedUbicacionId,
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
                                            text = { Text("+ Nueva Ubicación...") },
                                            onClick = { expandedUbicaciones = false; showAddUbicDialog = true }
                                        )
                                    }
                                }
                                Button(
                                    onClick = { estadoAConfirmar = "EN_ESTANTE"; showConfirmDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedUbicacionId.isNotBlank()
                                ) { Text("UBICAR EN ESTANTE") }
                            }
                        }
                    }
                }
                null -> {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                        Text("Verificando caja...")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoEvento(titulo: String, desc: String, btn: String, onConfirm: () -> Unit, loading: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(desc, color = Color.Gray)
        }
    }
    Spacer(Modifier.height(8.dp))
    if (loading) CircularProgressIndicator() else Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(btn) }
}