package com.example.logist_tech.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logist_tech.ui.viewmodels.LogistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogistViewModel = viewModel()
) {
    var periodoSeleccionado by remember { mutableStateOf("diario") }

    LaunchedEffect(periodoSeleccionado) {
        viewModel.loadReporte(periodoSeleccionado)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Reporte LogisTech", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("diario" to "Hoy", "semanal" to "Semana", "mensual" to "Mes").forEach { (key, label) ->
                            FilterChip(
                                selected = periodoSeleccionado == key,
                                onClick = { periodoSeleccionado = key },
                                label = { Text(label, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2980B9),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            when {
                viewModel.reporteLoading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = Color(0xFF2980B9))
                                Text("Generando reporte con IA...", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }

                viewModel.reporteError != null -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(viewModel.reporteError ?: "", color = Color(0xFFC62828), fontSize = 14.sp)
                            }
                        }
                    }
                }

                viewModel.reporte != null -> {
                    val reporte = viewModel.reporte!!
                    val datos = reporte.datos
                    val analisis = reporte.analisis_ia

                    // ── Métricas derivadas del flujo de 3 estados ───────
                    // registradas: en estado REGISTRADO, esperando que BANDA confirme recepción
                    // enAlmacen:   en estado RECEPCION_EN_ALMACEN, esperando salida
                    // salieron:    en estado SALIENDO_DE_ALMACEN (= "entregadas" del backend)
                    val registradas = datos.cajas_por_estado["REGISTRADO"] ?: 0
                    val enAlmacen   = datos.cajas_por_estado["RECEPCION_EN_ALMACEN"] ?: 0
                    val salieron    = datos.entregadas

                    item {
                        Text(
                            text = "${reporte.fecha_inicio}  →  ${reporte.fecha_fin}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    item {
                        Text(
                            text = "MÉTRICAS DEL PERÍODO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.2.sp
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricaCard("Total Cajas", datos.total_cajas.toString(), Icons.Default.Inventory2, Color(0xFF2980B9), Modifier.weight(1f))
                                MetricaCard("Pendientes", registradas.toString(), Icons.Default.HourglassBottom, Color(0xFF3B82F6), Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricaCard("En Almacén", enAlmacen.toString(), Icons.Default.Warehouse, Color(0xFFF59E0B), Modifier.weight(1f))
                                MetricaCard("Salieron", salieron.toString(), Icons.Default.LocalShipping, Color(0xFFEC4899), Modifier.weight(1f))
                            }
                        }
                    }

                    if (datos.top_operador != "Sin actividad") {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FB)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Operador más activo", fontSize = 12.sp, color = Color.Gray)
                                        Text(datos.top_operador, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2980B9))
                                    }
                                }
                            }
                        }
                    }

                    // ── Distribución por estado — 100% dinámica desde el backend ──
                    if (datos.cajas_por_estado.isNotEmpty()) {
                        item {
                            Text(
                                text = "DISTRIBUCIÓN POR ESTADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.2.sp
                            )
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Solo itera lo que el backend realmente devolvió.
                                    // Si un estado no tiene cajas, no aparece — sin nada hardcodeado.
                                    datos.cajas_por_estado.entries
                                        .sortedByDescending { it.value }
                                        .forEach { (estado, cantidad) ->
                                            val color = estadoColor(estado)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(color, shape = RoundedCornerShape(2.dp))
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    estado.replace("_", " "),
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF1E293B),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    cantidad.toString(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ANÁLISIS IA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.2.sp
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gemini Flash", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                                }
                                Text(
                                    text = analisis,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { viewModel.loadReporte(periodoSeleccionado) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Regenerar reporte")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricaCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

private fun estadoColor(estado: String): Color = when (estado) {
    "REGISTRADO"           -> Color(0xFF3B82F6)
    "RECEPCION_EN_ALMACEN" -> Color(0xFFF59E0B)
    "SALIENDO_DE_ALMACEN"  -> Color(0xFFEC4899)
    else                   -> Color.Gray
}