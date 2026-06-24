package com.example.logist_tech.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    // Cargar reporte al entrar y cuando cambia el período
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Selector de período ──────────────────────────────────────────
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

            when {
                viewModel.reporteLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = Color(0xFF2980B9))
                            Text("Generando reporte con IA...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                viewModel.reporteError != null -> {
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

                viewModel.reporte != null -> {
                    val datos = viewModel.reporte!!.datos
                    val analisis = viewModel.reporte!!.analisis_ia

                    // ── Fecha del reporte ────────────────────────────────────
                    Text(
                        text = "${viewModel.reporte!!.fecha_inicio}  →  ${viewModel.reporte!!.fecha_fin}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // ── Cards de métricas ────────────────────────────────────
                    Text(
                        text = "MÉTRICAS DEL PERÍODO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.2.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricaCard("Total Cajas", datos.total_cajas.toString(), Icons.Default.Inventory2, Color(0xFF2980B9), Modifier.weight(1f))
                        MetricaCard("Entregadas", datos.entregadas.toString(), Icons.Default.CheckCircle, Color(0xFF10B981), Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricaCard("Pendientes", datos.pendientes.toString(), Icons.Default.HourglassBottom, Color(0xFFF59E0B), Modifier.weight(1f))
                        MetricaCard("En Proceso", datos.en_proceso.toString(), Icons.Default.Loop, Color(0xFF8B5CF6), Modifier.weight(1f))
                    }

                    // ── Almacén ──────────────────────────────────────────────
                    Text(
                        text = "ALMACÉN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.2.sp
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AlmacenRow("Estantes ocupados", datos.estantes_ocupados.toString(), Color(0xFFE53935))
                            HorizontalDivider()
                            AlmacenRow("Estantes libres", datos.estantes_libres.toString(), Color(0xFF10B981))
                            HorizontalDivider()
                            AlmacenRow("Total estantes", datos.estantes_total.toString(), Color(0xFF2980B9))

                            if (datos.estantes_total > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("Ocupación", fontSize = 12.sp, color = Color.Gray)
                                LinearProgressIndicator(
                                    progress = { datos.estantes_ocupados.toFloat() / datos.estantes_total.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = Color(0xFF2980B9),
                                    trackColor = Color(0xFFE0E0E0)
                                )
                                Text(
                                    "${(datos.estantes_ocupados.toFloat() / datos.estantes_total.toFloat() * 100).toInt()}% ocupado",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // ── Operador más activo ──────────────────────────────────
                    if (datos.top_operador != "Sin actividad") {
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

                    // ── Estados de cajas ─────────────────────────────────────
                    if (datos.cajas_por_estado.isNotEmpty()) {
                        Text(
                            text = "DISTRIBUCIÓN POR ESTADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.2.sp
                        )

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                datos.cajas_por_estado.forEach { (estado, cantidad) ->
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

                    // ── Análisis de Gemini ───────────────────────────────────
                    Text(
                        text = "ANÁLISIS IA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.2.sp
                    )

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

                    Spacer(Modifier.height(8.dp))

                    // Botón refrescar
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

@Composable
private fun AlmacenRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun estadoColor(estado: String): Color = when (estado) {
    "REGISTRADO"           -> Color(0xFF3B82F6)
    "RECEPCION_EN_ALMACEN" -> Color(0xFFF59E0B)
    "EN_ESTANTE"           -> Color(0xFF10B981)
    "SALIDA_DE_ESTANTE"    -> Color(0xFF8B5CF6)
    "SALIENDO_DE_ALMACEN"  -> Color(0xFFEC4899)
    "ENTREGADO"            -> Color(0xFF64748B)
    else                   -> Color.Gray
}