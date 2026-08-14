package com.example.myapplication.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.MainViewModel

@SuppressLint("MissingPermission")
@Composable
fun BluetoothSetupScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val devices = viewModel.getPairedDevices()
    val isConnecting by viewModel.isBluetoothConnecting.collectAsState()
    val isConnected by viewModel.isBluetoothConnected.collectAsState()
    val connectionError by viewModel.bluetoothConnectionError.collectAsState()

    LaunchedEffect(isConnected) {
        if (isConnected) onBack()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Seleccionar Balanza Emparejada", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(devices) { device ->
                ListItem(
                    headlineContent = { Text(device.name ?: "Desconocido") },
                    supportingContent = { Text(device.address) },
                    modifier = Modifier.clickable {
                        if (!isConnecting) viewModel.connectToDevice(device)
                    }
                )
            }
        }

        if (isConnecting) {
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Conectando con la balanza...")
            }
        }

        connectionError?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se pudo conectar: $error",
                color = MaterialTheme.colorScheme.error
            )
        }
        
        if (devices.isEmpty()) {
            Text("No hay dispositivos emparejados. Por favor, empareja la balanza ID 226 en los ajustes de Android.")
        }
    }
}
