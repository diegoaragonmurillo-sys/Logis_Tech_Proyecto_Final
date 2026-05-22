package com.example.logist_tech.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logist_tech.R
import com.example.logist_tech.models.Producto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val AzulLogis = Color(0xFF2980B9)
private val AzulOscuro = Color(0xFF123B6D)
private val FondoBlanco = Color(0xFFFFFFFF)

@Composable
fun HistoryScreen(onNavigateBack: () -> Unit = {}) {

    var selectedFilter by remember { mutableStateOf<String?>("Entrada") }
    var searchQuery by remember { mutableStateOf("") }
    var fechaSeleccionada by remember { mutableStateOf<String?>("21/05/2026") }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val historial = remember {
        listOf(
            Producto(id = "1", nombre = "Laptop Lenovo", cantidad = 5, pesoKg = 2.5,
                categoria = "Tecnología", destino = "Lima", estado = "Correcto",
                tipoMovimiento = "Entrada", fecha = "21/05/2026"),
            Producto(id = "2", nombre = "Mouse Logitech", cantidad = 3, pesoKg = 0.5,
                categoria = "Accesorios", destino = "Arequipa", estado = "Correcto",
                tipoMovimiento = "Salida", fecha = "20/05/2026"),
            Producto(id = "3", nombre = "Monitor Samsung", cantidad = 2, pesoKg = 4.5,
                categoria = "Pantallas", destino = "Cusco", estado = "Anomalía",
                tipoMovimiento = "Entrada", fecha = "19/05/2026")
        )
    }

    val filteredList = historial.filter { producto ->
        (selectedFilter == null || producto.tipoMovimiento == selectedFilter) &&
                (fechaSeleccionada == null || producto.fecha == fechaSeleccionada) &&
                producto.nombre.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoBlanco)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AzulLogis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_logo_logis),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    contentScale = ContentScale.FillWidth
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Historial",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulOscuro
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar producto...", color = Color.Gray) },
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.Search,
                            contentDescription = "Search", tint = AzulLogis)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE7EAF0),
                        unfocusedContainerColor = Color(0xFFE7EAF0),
                        focusedBorderColor = AzulLogis,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fechaSeleccionada ?: "Todas las fechas",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha", color = AzulOscuro) },
                    trailingIcon = {
                        IconButton(onClick = { mostrarCalendario = true }) {
                            Icon(imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar", tint = AzulLogis)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = AzulLogis,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                if (mostrarCalendario) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { mostrarCalendario = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { ms ->
                                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                    fechaSeleccionada = fmt.format(java.util.Date(ms))
                                }
                                mostrarCalendario = false
                            }) { Text("Aceptar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
                        }
                    ) { DatePicker(state = datePickerState) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterButton("Entrada", selectedFilter == "Entrada") {
                        selectedFilter = if (selectedFilter == "Entrada") null else "Entrada"
                    }
                    FilterButton("Salida", selectedFilter == "Salida") {
                        selectedFilter = if (selectedFilter == "Salida") null else "Salida"
                    }
                    if (selectedFilter != null || fechaSeleccionada != null || searchQuery.isNotEmpty()) {
                        TextButton(onClick = {
                            selectedFilter = null
                            fechaSeleccionada = null
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null,
                                tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron registros", color = Color.Gray, fontSize = 16.sp)
                }
            }
        }

        items(filteredList) { producto -> HistoryCard(producto) }
    }
}

@Composable
fun FilterButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AzulLogis else Color.White
        ),
        border = BorderStroke(1.dp, if (selected) AzulLogis else Color.LightGray),
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(text = text, color = if (selected) Color.White else Color.Black)
    }
}

@Composable
fun HistoryCard(producto: Producto) {
    val cardColor = when (producto.estado) {
        "Anomalía" -> Color(0xFFFFE0E0)
        else -> Color(0xFFDCE6EF)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Outlined.Inventory2,
                contentDescription = null, tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = producto.nombre, fontSize = 16.sp,
                    fontWeight = FontWeight.Medium, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Fecha: ${producto.fecha}", fontSize = 13.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = producto.tipoMovimiento,
                    fontSize = 13.sp,
                    color = if (producto.tipoMovimiento == "Entrada") Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
            Icon(imageVector = Icons.Default.MoreVert,
                contentDescription = null, tint = Color.DarkGray)
        }
    }
}