package com.example.logist_tech.anomalias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logist_tech.ui.theme.Logist_TechTheme

/**
 * T-04 — AnomaliasScreen
 * Pantalla principal de anomalías. Muestra la lista de inconsistencias
 * detectadas con filtros por prioridad y colores diferenciados.
 */
@Composable
fun AnomaliasScreen() {

    // Filtro activo: null = todas
    var filtroActivo by remember { mutableStateOf<String?>(null) }

    // Lista reactiva de anomalías según filtro
    var anomalias by remember { mutableStateOf(AnomaliaManager.filtrarPorPrioridad(null)) }

    // Actualizar lista cuando cambia el filtro
    LaunchedEffect(filtroActivo) {
        anomalias = AnomaliaManager.filtrarPorPrioridad(filtroActivo)
    }

    Scaffold(
        topBar = { AnomaliasTopBar(total = AnomaliaManager.totalAnomalias()) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            // ── Chips de filtro por prioridad ──
            FiltrosPrioridad(
                filtroActivo = filtroActivo,
                onFiltroSeleccionado = { nuevo ->
                    filtroActivo = if (filtroActivo == nuevo) null else nuevo
                    anomalias = AnomaliaManager.filtrarPorPrioridad(filtroActivo)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Resumen por prioridad ──
            ResumenAnomalias()

            Spacer(modifier = Modifier.height(12.dp))

            // ── Lista de anomalías ──
            if (anomalias.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (filtroActivo == null) "✅ Sin anomalías registradas"
                        else "Sin anomalías de prioridad $filtroActivo",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(anomalias) { anomalia ->
                        AnomaliaCard(anomalia = anomalia)
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomaliasTopBar(total: Int) {
    TopAppBar(
        title = {
            Text(
                text = "⚠️ Anomalías ($total)",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFFFEBEE)
        )
    )
}

// ─────────────────────────────────────────────
// CHIPS DE FILTRO
// ─────────────────────────────────────────────

@Composable
fun FiltrosPrioridad(
    filtroActivo: String?,
    onFiltroSeleccionado: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("ALTA", "MEDIA", "BAJA").forEach { prioridad ->
            val color = colorPorPrioridad(prioridad)
            FilterChip(
                selected = filtroActivo == prioridad,
                onClick  = { onFiltroSeleccionado(prioridad) },
                label    = {
                    Text(
                        text = "${iconoPorPrioridad(prioridad)} $prioridad",
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
// RESUMEN
// ─────────────────────────────────────────────

@Composable
fun ResumenAnomalias() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("ALTA", "MEDIA", "BAJA").forEach { prioridad ->
            val cantidad = AnomaliaManager.filtrarPorPrioridad(prioridad).size
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorPorPrioridad(prioridad).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$cantidad",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = colorPorPrioridad(prioridad)
                    )
                    Text(
                        text = prioridad,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CARD DE ANOMALÍA
// ─────────────────────────────────────────────

@Composable
fun AnomaliaCard(anomalia: Anomalia) {
    val color = colorPorPrioridad(anomalia.prioridad)
    val icono = iconoPorPrioridad(anomalia.prioridad)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Encabezado: icono + tipo + prioridad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$icono ${anomalia.tipo.replace("_", " ")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
                Badge(containerColor = color) {
                    Text(
                        text = anomalia.prioridad,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Producto involucrado
            Text(
                text = "📦 ${anomalia.productoNombre}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Descripción del error
            Text(
                text = anomalia.descripcion,
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Fecha y evidencia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🕐 ${anomalia.fecha}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                if (anomalia.evidenciaUri.isNotBlank()) {
                    Text(
                        text = "📷 Con evidencia",
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// UTILIDADES DE COLOR E ÍCONO
// ─────────────────────────────────────────────

fun colorPorPrioridad(prioridad: String): Color = when (prioridad) {
    "ALTA"  -> Color(0xFFE53935)
    "MEDIA" -> Color(0xFFF57F17)
    else    -> Color(0xFF2E7D32)
}

fun iconoPorPrioridad(prioridad: String): String = when (prioridad) {
    "ALTA"  -> "🔴"
    "MEDIA" -> "🟡"
    else    -> "🟢"
}

// ─────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun AnomaliasScreenPreview() {
    // Cargar datos de prueba para el preview
    AnomaliaManager.limpiar()
    AnomaliaManager.registrarDesdeResultado(
        com.example.logist_tech.ocr.ResultadoComparacion(
            hayAnomalia = true,
            tipo        = "QR_OCR_DIFERENTE",
            descripcion = "Cantidad QR: 20  ≠  Cantidad OCR: 25.",
            prioridad   = "ALTA",
            ocrData     = com.example.logist_tech.ocr.OcrData(
                nombre = "Laptop", cantidad = 25, pesoKg = 1.5,
                destino = "Lima", textoOriginal = "Producto: Laptop\nCantidad: 25",
                camposFaltantes = emptyList()
            ),
            qrData = com.example.logist_tech.ocr.QrData(nombre = "Laptop", cantidad = 20)
        )
    )
    AnomaliaManager.registrarDesdeResultado(
        com.example.logist_tech.ocr.ResultadoComparacion(
            hayAnomalia = true,
            tipo        = "CANTIDAD_VACIA",
            descripcion = "No se detectó cantidad en el documento.",
            prioridad   = "MEDIA",
            ocrData     = com.example.logist_tech.ocr.OcrData(
                nombre = "Caja Azul", cantidad = 0, pesoKg = 0.0,
                destino = "Cusco", textoOriginal = "Producto: Caja Azul",
                camposFaltantes = listOf("cantidad")
            ),
            qrData = null
        )
    )
    Logist_TechTheme {
        AnomaliasScreen()
    }
}