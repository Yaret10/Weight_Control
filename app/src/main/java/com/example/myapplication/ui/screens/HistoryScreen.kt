package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.RegistroWithDetails
import com.example.myapplication.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val fileNameFormat = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var registroToDelete by remember { mutableStateOf<RegistroWithDetails?>(null) }
    val selectedRegistroIds = remember { mutableStateListOf<Int>() }
    val allSelected = history.isNotEmpty() && history.all { it.registro.id in selectedRegistroIds }

    LaunchedEffect(history) {
        selectedRegistroIds.retainAll(history.map { it.registro.id }.toSet())
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    // Permite que Excel reconozca correctamente las tildes y eñes.
                    output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    output.write(createCsv(history).toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo seleccionado")
                Toast.makeText(context, "Archivo exportado correctamente", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    "No se pudo exportar: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historial de Pesajes", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { shouldSelectAll ->
                        selectedRegistroIds.clear()
                        if (shouldSelectAll) {
                            selectedRegistroIds.addAll(history.map { it.registro.id })
                        }
                    },
                    enabled = history.isNotEmpty()
                )
                Text("Seleccionar todos")
            }

            Row {
                IconButton(
                    onClick = {
                        exportLauncher.launch("pesajes_${fileNameFormat.format(Date())}.csv")
                    },
                    enabled = history.isNotEmpty()
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Exportar a Excel")
                }
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = history.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar historial",
                        tint = if (history.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            LocalContentColor.current.copy(alpha = 0.38f)
                        }
                    )
                }
                IconButton(onClick = { /* Mostrar diálogo de filtros */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(history, key = { it.registro.id }) { item ->
                ListItem(
                    headlineContent = {
                        Text("${String.format(Locale.US, "%.3f", item.registro.peso)} kg - ${item.producto.nombre}")
                    },
                    supportingContent = {
                        Text("Cliente: ${item.cliente.nombre}\nFecha: ${dateFormat.format(Date(item.registro.fecha))}")
                    },
                    overlineContent = { Text("ID: ${item.registro.id}") },
                    trailingContent = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.registro.id in selectedRegistroIds,
                                onCheckedChange = { isSelected ->
                                    if (isSelected) {
                                        if (item.registro.id !in selectedRegistroIds) {
                                            selectedRegistroIds.add(item.registro.id)
                                        }
                                    } else {
                                        selectedRegistroIds.remove(item.registro.id)
                                    }
                                }
                            )
                            IconButton(onClick = { registroToDelete = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar registro ${item.registro.id}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }

        Button(
            onClick = { /* La impresión Bluetooth se implementará posteriormente. */ },
            enabled = selectedRegistroIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("IMPRIMIR (${selectedRegistroIds.size})")
        }
    }

    registroToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { registroToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("¿Eliminar este registro?") },
            text = {
                Text(
                    "${String.format(Locale.US, "%.3f", item.registro.peso)} kg - ${item.producto.nombre}\n" +
                        "Cliente: ${item.cliente.nombre}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRegistro(item.registro.id)
                        registroToDelete = null
                        Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("ELIMINAR", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { registroToDelete = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("¿Borrar historial?") },
            text = {
                Text(
                    "Se eliminarán permanentemente todos los registros de pesaje. " +
                        "Los clientes y productos no se borrarán."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllRegistros()
                        showDeleteConfirmation = false
                        Toast.makeText(context, "Historial eliminado", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("BORRAR", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}

private fun createCsv(history: List<RegistroWithDetails>): String = buildString {
    appendLine("ID;Fecha;Identificacion cliente;Cliente;Producto;Descripcion producto;Peso kg")
    val exportDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    history.forEach { item ->
        val values = listOf(
            item.registro.id.toString(),
            exportDateFormat.format(Date(item.registro.fecha)),
            item.cliente.identificacion,
            item.cliente.nombre,
            item.producto.nombre,
            item.producto.descripcion,
            item.registro.peso.toString()
        )
        appendLine(values.joinToString(";") { csvCell(it) })
    }
}

private fun csvCell(value: String): String =
    "\"${value.replace("\"", "\"\"")}\""
