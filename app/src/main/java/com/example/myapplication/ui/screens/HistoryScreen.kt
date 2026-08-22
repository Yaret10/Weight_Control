package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.RegistroWithDetails
import com.example.myapplication.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val clientes by viewModel.clientes.collectAsState(initial = emptyList())
    val productos by viewModel.productos.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fileNameFormat = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()) }
    var clienteId by remember { mutableStateOf<Int?>(null) }
    var productoId by remember { mutableStateOf<Int?>(null) }
    var clienteExpanded by remember { mutableStateOf(false) }
    var productoExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var fechaInicio by remember { mutableStateOf<Long?>(null) }
    var fechaFin by remember { mutableStateOf<Long?>(null) }
    val rangePickerState = rememberDateRangePickerState()

    fun applyFilters(start: Long? = fechaInicio, end: Long? = fechaFin) {
        viewModel.setFilters(clienteId, productoId, start, end)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    output.write(createCsv(history).toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo seleccionado")
                Toast.makeText(context, "Archivo exportado correctamente", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, "No se pudo exportar: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Historial de Pesajes", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Mostrar filtros")
                }
                IconButton(
                    onClick = { exportLauncher.launch("pesajes_${fileNameFormat.format(Date())}.csv") },
                    enabled = history.isNotEmpty()
                ) { Icon(Icons.Default.Download, contentDescription = "Exportar CSV") }
            }
        }

        if (showFilters) {
            ExposedDropdownMenuBox(clienteExpanded, { clienteExpanded = !clienteExpanded }) {
            OutlinedTextField(
                value = clientes.firstOrNull { it.id == clienteId }?.nombre ?: "Todos los clientes",
                onValueChange = {}, readOnly = true, label = { Text("Cliente") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(clienteExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(clienteExpanded, { clienteExpanded = false }) {
                DropdownMenuItem(text = { Text("Todos los clientes") }, onClick = {
                    clienteId = null; clienteExpanded = false; applyFilters()
                })
                clientes.forEach { cliente ->
                    DropdownMenuItem(text = { Text(cliente.nombre) }, onClick = {
                        clienteId = cliente.id; clienteExpanded = false; applyFilters()
                    })
                }
            }
        }
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(productoExpanded, { productoExpanded = !productoExpanded }) {
            OutlinedTextField(
                value = productos.firstOrNull { it.id == productoId }?.nombre ?: "Todos los productos",
                onValueChange = {}, readOnly = true, label = { Text("Producto") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productoExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(productoExpanded, { productoExpanded = false }) {
                DropdownMenuItem(text = { Text("Todos los productos") }, onClick = {
                    productoId = null; productoExpanded = false; applyFilters()
                })
                productos.forEach { producto ->
                    DropdownMenuItem(text = { Text(producto.nombre) }, onClick = {
                        productoId = producto.id; productoExpanded = false; applyFilters()
                    })
                }
            }
        }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Text(if (fechaInicio == null) "Filtrar por fecha" else
                    "${dayFormat.format(Date(fechaInicio!!))} - ${dayFormat.format(Date(fechaFin!!))}")
            }
            if (clienteId != null || productoId != null || fechaInicio != null) {
                TextButton(onClick = {
                    clienteId = null; productoId = null; fechaInicio = null; fechaFin = null
                    rangePickerState.setSelection(null, null)
                    viewModel.setFilters(null, null, null, null)
                }) { Text("LIMPIAR") }
            }
            }
        }

        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val availableWidth = maxWidth
            val scale = maxOf(1f, availableWidth.value / TABLE_MIN_WIDTH.value)
            val adaptiveWidths = COLUMN_WIDTHS.map { it * scale }
            val adaptiveTableWidth = adaptiveWidths.fold(0.dp) { total, width -> total + width }
            Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            Column(modifier = Modifier.width(adaptiveTableWidth).fillMaxHeight()) {
                HistoryTableRow(
                    listOf("Ticket", "Peso neto", "Tara", "Bruto", "Fecha", "Cliente"),
                    adaptiveWidths,
                    true
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(history, key = { it.registro.id }) { item ->
                        HistoryTableRow(listOf(
                            item.registro.codigoTicket,
                            "${String.format(Locale.US, "%.1f", item.registro.peso)} kg",
                            "${String.format(Locale.US, "%.1f", item.registro.pesoTara)} kg",
                            "${String.format(Locale.US, "%.1f", item.registro.pesoBruto)} kg",
                            dateFormat.format(Date(item.registro.fecha)),
                            item.cliente.nombre
                        ), adaptiveWidths)
                        HorizontalDivider()
                    }
                }
            }
            }
        }
        Text("${history.size} registro(s)", style = MaterialTheme.typography.bodySmall)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                fechaInicio = rangePickerState.selectedStartDateMillis?.let { pickerDateToLocal(it, false) }
                fechaFin = (rangePickerState.selectedEndDateMillis
                    ?: rangePickerState.selectedStartDateMillis)?.let { pickerDateToLocal(it, true) }
                showDatePicker = false
                applyFilters(fechaInicio, fechaFin)
            }, enabled = rangePickerState.selectedStartDateMillis != null) { Text("APLICAR") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("CANCELAR") } }
        ) {
            DateRangePicker(
                state = rangePickerState,
                title = { Text("Seleccione un rango de fechas", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun HistoryTableRow(values: List<String>, widths: List<Dp>, header: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (header) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        values.forEachIndexed { index, value ->
            Text(
                text = value,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.width(widths[index]).padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun createCsv(history: List<RegistroWithDetails>): String = buildString {
    appendLine("Ticket;Fecha;Cliente;Producto;Descripcion producto;Operador;Placa;Conductor;Observaciones;Peso bruto kg;Tara kg;Peso neto kg")
    val exportDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    history.forEach { item ->
        val values = listOf(
            item.registro.codigoTicket, exportDateFormat.format(Date(item.registro.fecha)),
            item.cliente.nombre, item.producto.nombre,
            item.producto.descripcion, item.operador?.nombre.orEmpty(), item.registro.placaVehiculo,
            item.registro.conductor, item.registro.observacion,
            String.format(Locale.US, "%.1f", item.registro.pesoBruto),
            String.format(Locale.US, "%.1f", item.registro.pesoTara),
            String.format(Locale.US, "%.1f", item.registro.peso)
        )
        appendLine(values.joinToString(";") { csvCell(it) })
    }
}

private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""

private val COLUMN_WIDTHS = listOf(110.dp, 100.dp, 90.dp, 100.dp, 145.dp, 195.dp)
private val TABLE_MIN_WIDTH: Dp = COLUMN_WIDTHS.fold(0.dp) { total, width -> total + width }

private fun pickerDateToLocal(timestamp: Long, endOfDay: Boolean): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = timestamp }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        if (endOfDay) set(Calendar.HOUR_OF_DAY, 23) else set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, if (endOfDay) 59 else 0)
        set(Calendar.SECOND, if (endOfDay) 59 else 0)
        set(Calendar.MILLISECOND, if (endOfDay) 999 else 0)
    }.timeInMillis
}
