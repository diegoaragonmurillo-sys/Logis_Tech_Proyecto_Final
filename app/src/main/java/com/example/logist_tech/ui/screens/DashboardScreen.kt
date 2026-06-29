package com.example.logist_tech.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logist_tech.network.Caja
import com.example.logist_tech.ui.viewmodels.LogistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogistViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadTodasLasCajas()
    }

    val enAlmacen = listOf(
        "RECEPCION_EN_ALMACEN",
        "EN_ESTANTE",
        "SALIDA_DE_ESTANTE",
        "SALIENDO_DE_ALMACEN"
    )

    val stockPorProducto = viewModel.todasLasCajas
        .filter { it.estado != "ENTREGADO" }
        .groupBy { it.producto.ifBlank { "Sin nombre" } }
        .map { (producto, cajas) ->
            Triple(producto, cajas.sumOf { it.cantidad }, cajas.size)
        }
        .sortedByDescending { it.second }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Operativo", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
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
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Fila 1 de stats ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip(
                        "Total cajas",
                        viewModel.todasLasCajas.size.toString(),
                        Color(0xFF2980B9),
                        Modifier.weight(1f)
                    )
                    StatChip(
                        "Pendientes",
                        viewModel.todasLasCajas.count { it.estado == "REGISTRADO" }.toString(),
                        Color(0xFFF59E0B),
                        Modifier.weight(1f)
                    )
                    StatChip(
                        "Entregadas",
                        viewModel.todasLasCajas.count { it.estado == "ENTREGADO" }.toString(),
                        Color(0xFF64748B),
                        Modifier.weight(1f)
                    )
                }
            }

            // ── Fila 2 de stats ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip(
                        "En almacén",
                        viewModel.todasLasCajas.count { it.estado in enAlmacen }.toString(),
                        Color(0xFF10B981),
                        Modifier.weight(1f)
                    )
                    StatChip(
                        "En estante",
                        viewModel.todasLasCajas.count { it.estado == "EN_ESTANTE" }.toString(),
                        Color(0xFF10B981),
                        Modifier.weight(1f)
                    )
                    StatChip(
                        "Saliendo",
                        viewModel.todasLasCajas.count {
                            it.estado in listOf("SALIDA_DE_ESTANTE", "SALIENDO_DE_ALMACEN")
                        }.toString(),
                        Color(0xFFEC4899),
                        Modifier.weight(1f)
                    )
                }
            }

            // ── Stock por producto ─────────────────────────────────────
            item {
                Text(
                    text = "STOCK POR PRODUCTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.2.sp
                )
            }

            if (stockPorProducto.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay productos en stock", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(stockPorProducto) { (producto, unidades, cajas) ->
                    StockProductoCard(producto, unidades, cajas)
                }
            }

            // ── Flujo de cajas ─────────────────────────────────────────
            item {
                Text(
                    text = "FLUJO DE CAJAS EN TIEMPO REAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.2.sp
                )
            }

            items(viewModel.todasLasCajas) { caja ->
                MinimalistCajaItem(caja)
            }
        }
    }
}

@Composable
fun StockProductoCard(producto: String, unidades: Int, cajas: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "$cajas ${if (cajas == 1) "caja" else "cajas"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = unidades.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2980B9)
                )
                Text(text = "unidades", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun MinimalistCajaItem(caja: Caja) {
    val stateColor = getEstadoColor(caja.estado)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(stateColor))
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = caja.producto.ifBlank { "Sin nombre" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "ID: ${caja.codigo_qr}  •  ${caja.cantidad} uds",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(stateColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = caja.estado.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = stateColor
                        )
                    }
                }
            }
        }
    }
}

fun getEstadoColor(estado: String): Color {
    return when (estado) {
        "REGISTRADO"           -> Color(0xFF3B82F6)
        "RECEPCION_EN_ALMACEN" -> Color(0xFFF59E0B)
        "EN_ESTANTE"           -> Color(0xFF10B981)
        "SALIDA_DE_ESTANTE"    -> Color(0xFF8B5CF6)
        "SALIENDO_DE_ALMACEN"  -> Color(0xFFEC4899)
        "ENTREGADO"            -> Color(0xFF64748B)
        else                   -> Color.Gray
    }
}