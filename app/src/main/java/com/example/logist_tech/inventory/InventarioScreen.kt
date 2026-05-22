package com.example.logist_tech.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.logist_tech.R
import com.example.logist_tech.models.Producto
import com.example.logist_tech.ui.theme.Logist_TechTheme

private val AzulLogis   = Color(0xFF2980B9)
private val FondoBlanco = Color(0xFFFFFFFF)
private val ItemCrema   = Color(0xFFFAEFE3)
private val TextoOscuro = Color(0xFF1A1A2E)
private val GrisTexto   = Color(0xFF888888)

@Composable
fun InventarioScreen(
    onNavigateBack: () -> Unit = {}
) {
    var movimientos    by remember { mutableStateOf(InventarioTestData.getDatosPrueba()) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    val stockActual    = StockManager.calcularStockTotal(movimientos)
    val productosBajos = StockManager.productosBajoStock(movimientos)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoBlanco)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Botón atrás ──
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.padding(start = 8.dp, top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFFE67E22) // naranja como el mockup
                )
            }

            // ── Logo ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_logis),
                    contentDescription = "LogisTech Logo",
                    modifier = Modifier.height(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Título "Inventario:" en azul ──
            Text(
                text = "Inventario:",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AzulLogis,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Lista ──
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stockActual.entries.toList()) { entry ->
                    val esBajo  = entry.key in productosBajos
                    val detalle = movimientos.lastOrNull { it.nombre == entry.key }
                    ProductoItem(
                        nombre     = entry.key,
                        stock      = entry.value,
                        categoria  = detalle?.categoria ?: "—",
                        destino    = detalle?.destino ?: "—",
                        bajoBstock = esBajo,
                        onEntrada  = {
                            movimientos = movimientos + Producto(
                                id = System.currentTimeMillis().toString(),
                                nombre = entry.key, cantidad = 1, pesoKg = 0.0,
                                categoria = detalle?.categoria ?: "",
                                destino = detalle?.destino ?: "",
                                estado = "ok", tipoMovimiento = "ENTRADA",
                                fecha = java.time.LocalDate.now().toString()
                            )
                        },
                        onSalida = {
                            movimientos = movimientos + Producto(
                                id = System.currentTimeMillis().toString(),
                                nombre = entry.key, cantidad = 1, pesoKg = 0.0,
                                categoria = detalle?.categoria ?: "",
                                destino = detalle?.destino ?: "",
                                estado = "ok", tipoMovimiento = "SALIDA",
                                fecha = java.time.LocalDate.now().toString()
                            )
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // ── FAB ──
        FloatingActionButton(
            onClick = { mostrarDialogo = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = AzulLogis,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
        }
    }

    if (mostrarDialogo) {
        RegistroMovimientoDialog(
            onDismiss   = { mostrarDialogo = false },
            onConfirmar = { nuevo ->
                movimientos = movimientos + nuevo
                mostrarDialogo = false
            }
        )
    }
}

// ─────────────────────────────────────────────
// ITEM — ícono · nombre/subtexto · stock · menú ⋮
// ─────────────────────────────────────────────

@Composable
fun ProductoItem(
    nombre: String,
    stock: Int,
    categoria: String,
    destino: String,
    bajoBstock: Boolean,
    onEntrada: () -> Unit,
    onSalida: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItemCrema),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono circular
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (bajoBstock) Color(0xFFE53935).copy(alpha = 0.15f)
                        else AzulLogis.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bajoBstock) Icons.Default.Warning
                    else Icons.Default.Inventory,
                    contentDescription = null,
                    tint = if (bajoBstock) Color(0xFFE53935) else AzulLogis,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Nombre y subtexto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextoOscuro
                )
                Text(
                    text = "$categoria · $destino",
                    fontSize = 12.sp,
                    color = GrisTexto
                )
            }

            // Stock
            Text(
                text = "$stock u.",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (bajoBstock) Color(0xFFE53935) else TextoOscuro,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Botones + y -
            IconButton(
                onClick = onSalida,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Salida",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onEntrada,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Entrada",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// DIÁLOGO
// ─────────────────────────────────────────────

@Composable
fun RegistroMovimientoDialog(
    onDismiss: () -> Unit,
    onConfirmar: (Producto) -> Unit
) {
    var nombre         by remember { mutableStateOf("") }
    var cantidad       by remember { mutableStateOf("") }
    var categoria      by remember { mutableStateOf("") }
    var destino        by remember { mutableStateOf("") }
    var tipoMovimiento by remember { mutableStateOf("ENTRADA") }
    var errorMsg       by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Registrar movimiento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextoOscuro
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ENTRADA", "SALIDA").forEach { tipo ->
                        FilterChip(
                            selected = tipoMovimiento == tipo,
                            onClick  = { tipoMovimiento = tipo },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (tipo == "ENTRADA") Icons.Default.Add
                                        else Icons.Default.Remove,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tipo, fontSize = 13.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (tipo == "ENTRADA") AzulLogis
                                else Color(0xFFE53935),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = categoria, onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = destino, onValueChange = { destino = it },
                    label = { Text("Destino") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = Color(0xFFE53935), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cantInt = cantidad.toIntOrNull()
                            when {
                                nombre.isBlank()                -> errorMsg = "Nombre obligatorio"
                                cantInt == null || cantInt <= 0 -> errorMsg = "Cantidad inválida"
                                categoria.isBlank()             -> errorMsg = "Categoría obligatoria"
                                destino.isBlank()               -> errorMsg = "Destino obligatorio"
                                else -> onConfirmar(
                                    Producto(
                                        id             = System.currentTimeMillis().toString(),
                                        nombre         = nombre.trim(),
                                        cantidad       = cantInt,
                                        pesoKg         = 0.0,
                                        categoria      = categoria.trim(),
                                        destino        = destino.trim(),
                                        estado         = "ok",
                                        tipoMovimiento = tipoMovimiento,
                                        fecha          = java.time.LocalDate.now().toString()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulLogis)
                    ) { Text("Confirmar", color = Color.White) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InventarioScreenPreview() {
    Logist_TechTheme {
        InventarioScreen()
    }
}