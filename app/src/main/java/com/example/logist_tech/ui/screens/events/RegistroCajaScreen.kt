package com.example.logist_tech.ui.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import com.example.logist_tech.network.RetrofitClient
import com.example.logist_tech.ocr.OcrProcessor
import com.example.logist_tech.scanner.ScannerResultHolder
import com.example.logist_tech.ui.viewmodels.LogistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroCajaScreen(
    codigoQr: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LogistViewModel = viewModel()
) {
    // ── Datos desde OCR ───────────────────────────────────────────────
    val ocrTexto = ScannerResultHolder.textoOcr
    val ocrData  = remember(ocrTexto) {
        if (ocrTexto.isNotBlank()) OcrProcessor.parsearTextoOcr(ocrTexto) else null
    }

    // ── Lógica del ID ─────────────────────────────────────────────────
    // Prioridad: QR > OCR (campo ID en la hoja) > vacío (receptor escribe)
    val idDesdeQr  = codigoQr.ifBlank { "" }
    val idDesdeOcr = remember(ocrTexto) {
        if (ocrTexto.isNotBlank()) OcrProcessor.extraerIdDeTexto(ocrTexto) else ""
    }

    var codigoEditable by remember {
        mutableStateOf(
            when {
                idDesdeQr.isNotBlank()  -> idDesdeQr
                idDesdeOcr.isNotBlank() -> idDesdeOcr
                else                    -> "CJ-"
            }
        )
    }

    // Válido si cumple formato CJ-XXXX (exactamente 4 dígitos)
    val codigoValido = Regex("^CJ-\\d{4}$").matches(codigoEditable)

    // ── Campos del formulario ─────────────────────────────────────────
    var producto    by remember { mutableStateOf(ocrData?.nombre?.ifBlank { "" } ?: "") }
    var cantidad    by remember { mutableStateOf(if ((ocrData?.cantidad ?: 0) > 0) ocrData!!.cantidad.toString() else "1") }
    var peso        by remember { mutableStateOf(if ((ocrData?.pesoKg ?: 0.0) > 0.0) ocrData!!.pesoKg.toString() else "0.5") }
    var categoria   by remember { mutableStateOf(ocrData?.categoria?.ifBlank { "GENERAL" } ?: "GENERAL") }
    var prioridad   by remember { mutableStateOf(ocrData?.prioridad?.ifBlank { "NORMAL" } ?: "NORMAL") }
    var esFragil    by remember { mutableStateOf((ocrData?.esFragil ?: 0) == 1) }
    var idProveedor by remember { mutableStateOf(ocrData?.idProveedor?.ifBlank { "PROV-001" } ?: "PROV-001") }
    var selectedTipoId  by remember { mutableStateOf(ocrData?.idTipoCaja ?: "") }
    var expandedTipos   by remember { mutableStateOf(false) }

    // ── Badge campos pre-llenados ─────────────────────────────────────
    val camposPreLlenados = remember(ocrData) {
        buildList {
            if (idDesdeOcr.isNotBlank())                    add("ID")
            if (!ocrData?.nombre.isNullOrBlank())           add("Producto")
            if ((ocrData?.cantidad ?: 0) > 0)               add("Cantidad")
            if ((ocrData?.pesoKg ?: 0.0) > 0.0)             add("Peso")
            if (!ocrData?.categoria.isNullOrBlank())        add("Categoría")
            if (!ocrData?.prioridad.isNullOrBlank())        add("Prioridad")
            if (!ocrData?.idProveedor.isNullOrBlank())      add("Proveedor")
            if (!ocrData?.idTipoCaja.isNullOrBlank())       add("Tipo Caja")
            if ((ocrData?.esFragil ?: 0) == 1)              add("Frágil")
        }
    }

    var cajaYaExiste by remember { mutableStateOf<Boolean?>(null) }

    var showAddTipoDialog by remember { mutableStateOf(false) }
    var nuevoNombreTipo   by remember { mutableStateOf("") }
    var nuevoLargo        by remember { mutableStateOf("") }
    var nuevoAncho        by remember { mutableStateOf("") }
    var nuevoAlto         by remember { mutableStateOf("") }

    // ── Verificar duplicado solo si ya hay un código válido ───────────
    LaunchedEffect(codigoEditable) {
        if (!codigoValido) {
            cajaYaExiste = false
            return@LaunchedEffect
        }
        try {
            val response = RetrofitClient.api.getTodasLasCajas()
            cajaYaExiste = if (response.isSuccessful) {
                response.body()?.any { it.codigo_qr == codigoEditable } ?: false
            } else false
        } catch (e: Exception) {
            cajaYaExiste = false
        }
        viewModel.loadInitialData()
    }

    // ── Diálogo nuevo tipo de caja ────────────────────────────────────
    if (showAddTipoDialog) {
        AlertDialog(
            onDismissRequest = { showAddTipoDialog = false },
            title = { Text("Nuevo Tipo de Caja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nuevoNombreTipo,
                        onValueChange = { nuevoNombreTipo = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(value = nuevoLargo, onValueChange = { nuevoLargo = it }, label = { Text("Largo") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = nuevoAncho, onValueChange = { nuevoAncho = it }, label = { Text("Ancho") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = nuevoAlto,  onValueChange = { nuevoAlto  = it }, label = { Text("Alto")  }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.registrarTipoCaja(
                        nuevoNombreTipo,
                        nuevoLargo.toDoubleOrNull() ?: 0.0,
                        nuevoAncho.toDoubleOrNull() ?: 0.0,
                        nuevoAlto.toDoubleOrNull()  ?: 0.0
                    ) { showAddTipoDialog = false }
                }) { Text("GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTipoDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de Paquete") },
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
            when (cajaYaExiste) {

                // ── Caja duplicada ────────────────────────────────────
                true -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF57F17), modifier = Modifier.size(100.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("REGISTRO DUPLICADO", color = Color(0xFFF57F17), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "La caja \"$codigoEditable\" ya está registrada en el sistema.",
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("VOLVER") }
                    }
                }

                // ── Formulario de registro ────────────────────────────
                false -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // ── Campo ID de Caja ──────────────────────────
                        OutlinedTextField(
                            value = codigoEditable,
                            onValueChange = { nuevo ->
                                // Solo editable si NO vino del QR
                                if (idDesdeQr.isBlank()) {
                                    val sinPrefijo = nuevo
                                        .removePrefix("CJ-")
                                        .filter { it.isDigit() }
                                        .take(4)
                                    codigoEditable = "CJ-$sinPrefijo"
                                }
                            },
                            label = { Text("ID de Caja *") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = idDesdeQr.isNotBlank(),
                            isError = !codigoValido,
                            supportingText = {
                                if (!codigoValido)
                                    Text("Formato requerido: CJ-0001 (4 dígitos)", color = Color.Red, fontSize = 11.sp)
                                else
                                    Text("✓ Formato correcto", color = Color(0xFF2E7D32), fontSize = 11.sp)
                            }
                        )

                        // ── Badge OCR ─────────────────────────────────
                        if (camposPreLlenados.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✓ ", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    Text(
                                        "Pre-llenado por OCR: ${camposPreLlenados.joinToString(", ")}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        // ── Producto ──────────────────────────────────
                        OutlinedTextField(
                            value = producto,
                            onValueChange = { producto = it },
                            label = { Text("Producto *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = producto.isBlank()
                        )

                        // ── Cantidad y Peso ───────────────────────────
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cantidad,
                                onValueChange = { cantidad = it },
                                label = { Text("Cantidad *") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = peso,
                                onValueChange = { peso = it },
                                label = { Text("Peso (kg)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ── Categoría ─────────────────────────────────
                        OutlinedTextField(
                            value = categoria,
                            onValueChange = { categoria = it },
                            label = { Text("Categoría") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── Proveedor ─────────────────────────────────
                        OutlinedTextField(
                            value = idProveedor,
                            onValueChange = { idProveedor = it },
                            label = { Text("ID Proveedor") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── Tipo de caja ──────────────────────────────
                        ExposedDropdownMenuBox(
                            expanded = expandedTipos,
                            onExpandedChange = { expandedTipos = !expandedTipos }
                        ) {
                            OutlinedTextField(
                                value = viewModel.tiposCaja.find { it.id.toString() == selectedTipoId }?.nombre
                                    ?: "Seleccionar Tipo *",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Caja") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipos) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                isError = selectedTipoId.isBlank()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTipos,
                                onDismissRequest = { expandedTipos = false }
                            ) {
                                viewModel.tiposCaja.forEach { tipo ->
                                    DropdownMenuItem(
                                        text = { Text("${tipo.nombre} (${tipo.largo}×${tipo.ancho}×${tipo.alto})") },
                                        onClick = {
                                            selectedTipoId = tipo.id.toString()
                                            expandedTipos = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "+ Agregar nuevo tipo...",
                                            color = Color(0xFF2980B9),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = { expandedTipos = false; showAddTipoDialog = true }
                                )
                            }
                        }

                        // ── Frágil ────────────────────────────────────
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = esFragil, onCheckedChange = { esFragil = it })
                            Text("Es frágil")
                        }

                        Spacer(Modifier.height(4.dp))

                        // ── Botón registrar ───────────────────────────
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Button(
                                onClick = {
                                    viewModel.registrarCaja(
                                        codigoEditable,
                                        producto,
                                        cantidad.toIntOrNull() ?: 1,
                                        peso.toDoubleOrNull() ?: 0.0,
                                        prioridad,
                                        categoria,
                                        esFragil,
                                        idProveedor,
                                        selectedTipoId,
                                        onSuccess
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = codigoValido && producto.isNotBlank() && selectedTipoId.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9))
                            ) {
                                Text("REGISTRAR CAJA", fontWeight = FontWeight.Bold)
                            }
                        }

                        // ── Mensaje de error ──────────────────────────
                        viewModel.message?.let {
                            Text(
                                it,
                                color = Color.Red,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── Cargando ──────────────────────────────────────────
                null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2980B9))
                        Spacer(Modifier.height(12.dp))
                        Text("Validando código QR...", color = Color.Gray)
                    }
                }
            }
        }
    }
}