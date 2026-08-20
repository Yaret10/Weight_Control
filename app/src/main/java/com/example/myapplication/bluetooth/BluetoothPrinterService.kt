package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.myapplication.data.model.RegistroWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.nio.charset.Charset
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Conexión independiente para impresoras térmicas Bluetooth ESC/POS. */
class BluetoothPrinterService(context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val preferences = context.getSharedPreferences("bluetooth_printer", Context.MODE_PRIVATE)
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    suspend fun print(
        registros: List<RegistroWithDetails>,
        placaVehiculo: String = "",
        conductor: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val result = runCatching {
            require(registros.isNotEmpty()) { "No hay registros seleccionados" }
            val device = findPrinter()
                ?: error("No se encontró una impresora emparejada llamada BlueTooth Printer")

            var lastError: Exception? = null
            repeat(2) { attempt ->
                try {
                    ensureConnected(device)
                    socket!!.outputStream.apply {
                        write(createTicket(registros, placaVehiculo, conductor))
                        flush()
                    }
                    preferences.edit().putString(KEY_ADDRESS, device.address).apply()
                    return@runCatching "${registros.size} registro(s) enviado(s) a ${device.name ?: PRINTER_NAME}"
                } catch (error: Exception) {
                    lastError = error
                    close()
                    if (attempt == 0) delay(700)
                }
            }
            throw IOException(lastError?.localizedMessage ?: "La impresora no respondió", lastError)
        }
        // La impresora sigue emparejada; solo liberamos su conexión activa para
        // que no interfiera con la conexión permanente de la balanza.
        close()
        result
    }

    @SuppressLint("MissingPermission")
    private fun findPrinter(): BluetoothDevice? {
        val devices = adapter?.bondedDevices.orEmpty()
        val savedAddress = preferences.getString(KEY_ADDRESS, null)
        return devices.firstOrNull { it.address == savedAddress }
            ?: devices.firstOrNull { it.name?.replace(" ", "")?.equals("BluetoothPrinter", true) == true }
            ?: devices.firstOrNull {
                val name = it.name.orEmpty()
                name.contains("bluetooth", true) && name.contains("printer", true)
            }
    }

    @SuppressLint("MissingPermission")
    private fun ensureConnected(device: BluetoothDevice) {
        if (socket?.isConnected == true) return
        close()
        adapter?.cancelDiscovery()

        var failure: Exception? = null
        val factories = listOf<() -> BluetoothSocket>(
            { device.createInsecureRfcommSocketToServiceRecord(sppUuid) },
            { device.createRfcommSocketToServiceRecord(sppUuid) }
        )
        for (factory in factories) {
            try {
                socket = factory().also { it.connect() }
                return
            } catch (error: Exception) {
                failure = error
                close()
            }
        }
        throw IOException("No se pudo conectar con ${device.name ?: PRINTER_NAME}", failure)
    }

    private fun createTicket(items: List<RegistroWithDetails>, placaVehiculo: String, conductor: String): ByteArray {
        val output = ByteArrayOutputStream()
        fun command(vararg bytes: Int) = output.write(bytes.map(Int::toByte).toByteArray())
        fun line(text: String = "") {
            output.write(text.take(CHARS_PER_LINE).toByteArray(PRINTER_CHARSET))
            output.write('\n'.code)
        }

        command(0x1B, 0x40) // Inicializar ESC/POS.
        command(0x1B, 0x74, 0x02) // Página CP850 para tildes y eñes.
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        items.forEach { item ->
            command(0x1B, 0x61, 0x01)
            command(0x1B, 0x45, 0x01)
            line("MAPSAC - PATSAC")
            command(0x1B, 0x45, 0x00)
            line("BALANZA LOGÍSTICA")
            line()
            command(0x1B, 0x61, 0x00)
            line("-".repeat(CHARS_PER_LINE))
            line("TICKET NRO: ${item.registro.codigoTicket}")
            line("FECHA: ${dateFormat.format(Date(item.registro.fecha))}")
            line("HORA DE PESAJE: ${timeFormat.format(Date(item.registro.fecha))}")
            wrap("OPERADOR: ${item.operador?.nombre ?: "Sin operador"}").forEach(::line)
            line("-".repeat(CHARS_PER_LINE))
            wrap("CLIENTE: ${item.cliente.nombre}").forEach(::line)
            val placaTicket = placaVehiculo.ifBlank { item.registro.placaVehiculo }
            val conductorTicket = conductor.ifBlank { item.registro.conductor }
            if (placaTicket.isNotBlank()) wrap("PLACA VEHÍCULO: $placaTicket").forEach(::line)
            if (conductorTicket.isNotBlank()) wrap("CONDUCTOR: $conductorTicket").forEach(::line)
            wrap("PRODUCTO: ${item.producto.nombre}").forEach(::line)
            line("-".repeat(CHARS_PER_LINE))
            line("PRIMER PESO (Bruto): ${String.format(Locale.US, "%.3f", item.registro.pesoBruto)} kg")
            line("SEGUNDO PESO (Tara): ${String.format(Locale.US, "%.3f", item.registro.pesoTara)} kg")
            line("-".repeat(CHARS_PER_LINE))
            command(0x1B, 0x45, 0x01)
            line("PESO NETO: ${String.format(Locale.US, "%.3f", item.registro.peso)} kg")
            command(0x1B, 0x45, 0x00)
            line(); line()
            command(0x1B, 0x61, 0x01)
            line("¡CONTROL DE PESAJE COMPLETADO!")
            line(); line(); line()
        }
        command(0x1D, 0x56, 0x42, 0x00) // Corte; las portátiles sin cortador lo ignoran.
        return output.toByteArray()
    }

    private fun wrap(text: String): List<String> = text.chunked(CHARS_PER_LINE)

    fun close() {
        try { socket?.close() } catch (_: IOException) { }
        socket = null
    }

    private companion object {
        const val PRINTER_NAME = "BlueTooth Printer"
        const val KEY_ADDRESS = "address"
        const val CHARS_PER_LINE = 48
        val PRINTER_CHARSET: Charset = Charset.forName("CP850")
    }
}
