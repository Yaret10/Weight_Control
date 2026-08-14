package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothService(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null

    private val _weightFlow = MutableStateFlow<Double?>(null)
    val weightFlow: StateFlow<Double?> = _weightFlow

    private val _isWeightStable = MutableStateFlow(false)
    val isWeightStable: StateFlow<Boolean> = _isWeightStable
    private val recentWeights = ArrayDeque<Double>()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> =
        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        disconnect()
        _isConnecting.value = true
        _connectionError.value = null
        bluetoothAdapter?.cancelDiscovery()

        var lastError: Exception? = null
        for (attempt in 1..2) {
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
                connectWithTimeout(socket!!)
                lastError = null
                break
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastError = error
                closeSocket()
                if (attempt == 1) delay(1_000)
            }
        }

        if (lastError != null || socket == null) {
            _isConnected.value = false
            _isConnecting.value = false
            _connectionError.value = lastError?.localizedMessage
                ?: "No se pudo conectar por Bluetooth SPP"
            closeSocket()
            return@withContext
        }

        _isConnected.value = true
        _isConnecting.value = false
        listenForData(socket!!)
    }

    private suspend fun connectWithTimeout(connectingSocket: BluetoothSocket) = coroutineScope {
        val timeoutCloser = launch(Dispatchers.IO) {
            delay(5_000)
            try {
                connectingSocket.close()
            } catch (_: IOException) {
                // El intento ya terminó.
            }
        }

        try {
            withContext(Dispatchers.IO) { connectingSocket.connect() }
        } finally {
            timeoutCloser.cancel()
        }
    }

    private fun listenForData(connectedSocket: BluetoothSocket) {
        val inputStream = connectedSocket.inputStream
        val buffer = ByteArray(1024)

        while (_isConnected.value) {
            try {
                val bytes = inputStream.read(buffer)
                if (bytes == -1) {
                    _connectionError.value = "El indicador cerró la conexión"
                    break
                }
                if (bytes > 0) {
                    val receivedData = String(buffer, 0, bytes)
                    WeightParser.parse(receivedData)?.let { parsedWeight ->
                        _weightFlow.value = parsedWeight
                        updateWeightStability(receivedData, parsedWeight)
                    }
                }
            } catch (error: IOException) {
                _connectionError.value = "Se perdió la conexión: ${error.localizedMessage ?: "sin respuesta"}"
                break
            }
        }

        _isConnected.value = false
        closeSocket()
    }

    fun disconnect() {
        _isConnected.value = false
        _isConnecting.value = false
        _isWeightStable.value = false
        _weightFlow.value = null
        recentWeights.clear()
        closeSocket()
    }

    private fun updateWeightStability(data: String, weight: Double) {
        when {
            WeightParser.isStable(data) -> {
                _isWeightStable.value = true
                recentWeights.clear()
            }
            WeightParser.isUnstable(data) -> {
                _isWeightStable.value = false
                recentWeights.clear()
            }
            else -> {
                recentWeights.addLast(weight)
                while (recentWeights.size > STABLE_READING_COUNT) recentWeights.removeFirst()
                _isWeightStable.value = recentWeights.size == STABLE_READING_COUNT &&
                    recentWeights.maxOrNull()!! - recentWeights.minOrNull()!! <= STABLE_TOLERANCE_KG
            }
        }
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: IOException) {
            // El socket ya estaba cerrado.
        } finally {
            socket = null
        }
    }


    private companion object {
        const val STABLE_READING_COUNT = 3
        const val STABLE_TOLERANCE_KG = 0.02
    }
}
