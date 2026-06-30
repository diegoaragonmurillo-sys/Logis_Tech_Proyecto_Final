package com.example.logist_tech.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    val stockPorProducto = viewModel.todasLasCajas
        .filter { it.estado != "SALIENDO_DE_ALMACEN" }
        .groupBy { it.producto.ifBlank { "Sin nombre" } }
        .map { (producto, cajas) ->
            Triple(producto, cajas.sumOf { it.cantidad }, cajas.size)
        }
        .sortedByDescending { it.second }

    val total      = viewModel.todasLasCajas.size
    val pendientes = viewModel.todasLasCajas.count { it.estado == "REGISTRADO" }
    val enAlmacen  = viewModel.todasLasCajas.count { it.estado == "RECEPCION_EN_ALMACEN" }
    val salieron   = viewModel.todasLasCajas.count { it.estado == "SALIENDO_DE_ALMACEN" }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Stats en grid 2x2 — más aire, números legibles ────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatChip("Total cajas", total.toString(), Icons.Default.Inventory2, Color(0xFF2980B9), Modifier.weight(1f))
                        StatChip("Pendientes", pendientes.toString(), Icons.Default.HourglassBottom, Color(0xFFF59E0B), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatChip("En almacén", enAlmacen.toString(), Icons.Default.Warehouse, Color(0xFF10B981), Modifier.weight(1f))
                        StatChip("Salieron", salieron.toString(), Icons.Default.LocalShipping, Color(0xFFEC4899), Modifier.weight(1f))
                    }
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
                items(
                    items = stockPorProducto,
                    key = { it.first }
                ) { (producto, unidades, cajas) ->
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

            items(
                items = viewModel.todasLasCajas,
                key = { it.codigo_qr }
            ) { caja ->
                MinimalistCajaItem(caja)
            }

            item { Spacer(Modifier.height(8.dp)) }
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
fun StatChip(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
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
        "SALIENDO_DE_ALMACEN"  -> Color(0xFFEC4899)
        else                   -> Color.Gray
    }
}