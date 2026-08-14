package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.MainViewModel

@Composable
fun ManagementScreen(viewModel: MainViewModel) {
    var clienteNombre by remember { mutableStateOf("") }
    var clienteIden by remember { mutableStateOf("") }
    var productoNombre by remember { mutableStateOf("") }
    var productoDesc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Gestión de Catálogos", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Clientes
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nuevo Cliente", style = MaterialTheme.typography.titleLarge)
                TextField(value = clienteNombre, onValueChange = { clienteNombre = it }, label = { Text("Nombre") })
                TextField(value = clienteIden, onValueChange = { clienteIden = it }, label = { Text("DNI/RUC") })
                Button(onClick = { 
                    viewModel.addCliente(clienteNombre, clienteIden)
                    clienteNombre = ""; clienteIden = ""
                }) { Text("Añadir Cliente") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Productos
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nuevo Producto", style = MaterialTheme.typography.titleLarge)
                TextField(value = productoNombre, onValueChange = { productoNombre = it }, label = { Text("Nombre Producto") })
                TextField(value = productoDesc, onValueChange = { productoDesc = it }, label = { Text("Descripción") })
                Button(onClick = { 
                    viewModel.addProducto(productoNombre, productoDesc)
                    productoNombre = ""; productoDesc = ""
                }) { Text("Añadir Producto") }
            }
        }
    }
}
