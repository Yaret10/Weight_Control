package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(viewModel: MainViewModel, onNavigateToSetup: () -> Unit) {
    val weight by viewModel.currentWeight.collectAsState()
    val isWeightStable by viewModel.isWeightStable.collectAsState()
    val isConnected by viewModel.isBluetoothConnected.collectAsState()
    val isConnecting by viewModel.isBluetoothConnecting.collectAsState()
    val clientes by viewModel.clientes.collectAsState(initial = emptyList())
    val productos by viewModel.productos.collectAsState(initial = emptyList())
    val selectedCliente by viewModel.selectedCliente.collectAsState()
    val selectedProducto by viewModel.selectedProducto.collectAsState()
    val context = LocalContext.current

    var clienteExpanded by remember { mutableStateOf(false) }
    var productoExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bluetooth Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).padding(4.dp)) {
                // Indicador de color
            }
            Text(
                when {
                    isConnected -> "Conectado a Balanza"
                    isConnecting -> "Conectando..."
                    else -> "Desconectado"
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onNavigateToSetup) { Text("Configurar") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selectores
        ExposedDropdownMenuBox(expanded = clienteExpanded, onExpandedChange = { clienteExpanded = !clienteExpanded }) {
            TextField(
                value = selectedCliente?.nombre ?: "Seleccionar Cliente",
                onValueChange = {},
                readOnly = true,
                label = { Text("Cliente") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clienteExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = clienteExpanded, onDismissRequest = { clienteExpanded = false }) {
                clientes.forEach { cliente ->
                    DropdownMenuItem(
                        text = { Text(cliente.nombre) },
                        onClick = { viewModel.selectCliente(cliente); clienteExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = productoExpanded, onExpandedChange = { productoExpanded = !productoExpanded }) {
            TextField(
                value = selectedProducto?.nombre ?: "Seleccionar Producto",
                onValueChange = {},
                readOnly = true,
                label = { Text("Producto") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productoExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = productoExpanded, onDismissRequest = { productoExpanded = false }) {
                productos.forEach { producto ->
                    DropdownMenuItem(
                        text = { Text(producto.nombre) },
                        onClick = { viewModel.selectProducto(producto); productoExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Display de Peso
        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = String.format(Locale.US, "%.3f kg", weight ?: 0.0),
                    color = Color.Green,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = if (isWeightStable) "Peso estable" else "Esperando peso estable",
            color = if (isWeightStable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                selectedCliente?.id?.let { cId ->
                    selectedProducto?.id?.let { pId ->
                        viewModel.saveRegistro(cId, pId, weight ?: 0.0)
                        Toast.makeText(context, "Peso capturado correctamente", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = isConnected && isWeightStable && weight != null &&
                selectedCliente != null && selectedProducto != null
        ) {
            Text("GUARDAR REGISTRO", fontSize = 20.sp)
        }
    }
}
