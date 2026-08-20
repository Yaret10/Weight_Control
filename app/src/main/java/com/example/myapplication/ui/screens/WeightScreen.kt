package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val operadores by viewModel.operadores.collectAsState(initial = emptyList())
    val selectedCliente by viewModel.selectedCliente.collectAsState()
    val selectedProducto by viewModel.selectedProducto.collectAsState()
    val selectedOperador by viewModel.selectedOperador.collectAsState()
    val taraRegistrada by viewModel.taraRegistrada.collectAsState()
    val indicadorTarado by viewModel.indicadorTarado.collectAsState()
    val context = LocalContext.current

    var clienteExpanded by remember { mutableStateOf(false) }
    var productoExpanded by remember { mutableStateOf(false) }
    var operadorExpanded by remember { mutableStateOf(false) }
    var taraManual by rememberSaveable { mutableStateOf("") }
    val pesoNeto = weight ?: 0.0
    val pesoBruto = pesoNeto + taraRegistrada

    LaunchedEffect(weight, taraRegistrada) {
        if (taraRegistrada > 0.0 && kotlin.math.abs(weight ?: Double.MAX_VALUE) <= 0.01) {
            viewModel.confirmarIndicadorTarado()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = operadorExpanded, onExpandedChange = { operadorExpanded = !operadorExpanded }) {
            TextField(
                value = selectedOperador?.nombre ?: "Seleccionar Operador",
                onValueChange = {},
                readOnly = true,
                label = { Text("Operador") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operadorExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = operadorExpanded, onDismissRequest = { operadorExpanded = false }) {
                operadores.forEach { operador ->
                    DropdownMenuItem(
                        text = { Text(operador.nombre) },
                        onClick = { viewModel.selectOperador(operador); operadorExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Registro de tara", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.registrarTara(100.0) },
                enabled = taraRegistrada == 0.0,
                modifier = Modifier.weight(1f)
            ) { Text("100 kg") }
            Button(
                onClick = { viewModel.registrarTara(60.0) },
                enabled = taraRegistrada == 0.0,
                modifier = Modifier.weight(1f)
            ) { Text("60 kg") }
        }
        OutlinedTextField(
            value = taraManual,
            onValueChange = { value ->
                taraManual = value.filter { it.isDigit() || it == '.' || it == ',' }
            },
            label = { Text("Otra tara (kg)") },
            singleLine = true,
            enabled = taraRegistrada == 0.0,
            trailingIcon = {
                TextButton(
                    onClick = {
                        taraManual.replace(',', '.').toDoubleOrNull()?.let(viewModel::registrarTara)
                    },
                    enabled = taraRegistrada == 0.0 &&
                        (taraManual.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
                ) { Text("USAR") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.capturarTara(weight ?: 0.0) },
                enabled = taraRegistrada == 0.0 && isConnected && isWeightStable &&
                    (weight ?: 0.0) > 0.0,
                modifier = Modifier.weight(1f)
            ) { Text("CAPTURAR TARA") }
            if (taraRegistrada > 0.0) {
                OutlinedButton(onClick = viewModel::limpiarTara) { Text("QUITAR REGISTRO") }
            }
        }
        if (taraRegistrada > 0.0) {
            Text(
                "Tara lista para el ticket: ${String.format(Locale.US, "%.3f", taraRegistrada)} kg",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display de Peso
        Text("PESO NETO", style = MaterialTheme.typography.titleLarge)
        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = String.format(Locale.US, "%.3f kg", pesoNeto),
                    color = Color.Green,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Peso bruto: ${String.format(Locale.US, "%.3f", pesoBruto)} kg",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Tara registrada: ${String.format(Locale.US, "%.3f", taraRegistrada)} kg",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = if (isWeightStable) "Peso estable" else "Esperando peso estable",
            color = if (isWeightStable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                selectedCliente?.id?.let { cId ->
                    selectedProducto?.id?.let { pId ->
                        selectedOperador?.id?.let { oId ->
                            viewModel.saveRegistro(
                                cId, pId, oId, "", "",
                                pesoNeto, taraRegistrada
                            )
                            Toast.makeText(context, "Peso capturado correctamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = isConnected && isWeightStable && weight != null &&
                selectedCliente != null && selectedProducto != null && selectedOperador != null &&
                taraRegistrada > 0.0 && indicadorTarado && pesoNeto > 0.0
        ) {
            Text("GUARDAR REGISTRO", fontSize = 20.sp)
        }
    }
}
