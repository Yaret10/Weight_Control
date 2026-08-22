package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import com.example.myapplication.R
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
    private val appContext = context.applicationContext
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
            line() // Cierra cualquier línea pendiente del búfer de la impresora.
            command(0x1B, 0x61, 0x01)
            command(0x1B, 0x45, 0x01)
            line("MAPSAC - PATSAC")
            command(0x1B, 0x45, 0x00)
            line("BALANZA LOGÍSTICA")
            createLogoCommand()?.let { output.write(it) }
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
            if (item.registro.observacion.isNotBlank()) {
                wrap("OBSERVACIONES: ${item.registro.observacion}").forEach(::line)
            }
            line("-".repeat(CHARS_PER_LINE))
            line("PRIMER PESO (Bruto): ${String.format(Locale.US, "%.1f", item.registro.pesoBruto)} kg")
            line("SEGUNDO PESO (Tara): ${String.format(Locale.US, "%.1f", item.registro.pesoTara)} kg")
            line("-".repeat(CHARS_PER_LINE))
            command(0x1B, 0x45, 0x01)
            line("PESO NETO: ${String.format(Locale.US, "%.1f", item.registro.peso)} kg")
            command(0x1B, 0x45, 0x00)
            line(); line()
            command(0x1B, 0x61, 0x01)
            line("¡CONTROL DE PESAJE COMPLETADO!")
            line(); line(); line()
        }
        command(0x1D, 0x56, 0x42, 0x00) // Corte; las portátiles sin cortador lo ignoran.
        return output.toByteArray()
    }

    /** Convierte el logo al formato raster ESC/POS conservando su tamaño actual. */
    private fun createLogoCommand(): ByteArray? = runCatching {
        val encoded = appContext.resources.openRawResource(R.raw.ticket_logo_base64)
            .bufferedReader()
            .use { it.readText() }
        val imageBytes = Base64.decode(encoded, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: error("No se pudo leer el logo")
        val centeredLogo = Bitmap.createBitmap(PRINTER_DOTS_PER_LINE, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(centeredLogo).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, ((PRINTER_DOTS_PER_LINE - bitmap.width) / 2f), 0f, null)
        }
        bitmapToEscPosRaster(centeredLogo).also {
            bitmap.recycle()
            centeredLogo.recycle()
        }
    }.getOrNull()

    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val bytesPerRow = (bitmap.width + 7) / 8
        val raster = ByteArray(bytesPerRow * bitmap.height)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                val luminance = (
                    299 * android.graphics.Color.red(color) +
                        587 * android.graphics.Color.green(color) +
                        114 * android.graphics.Color.blue(color)
                    ) / 1000
                if (luminance < 180 && android.graphics.Color.alpha(color) > 127) {
                    val index = y * bytesPerRow + x / 8
                    raster[index] = (raster[index].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }

        return ByteArrayOutputStream().apply {
            write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            write(bytesPerRow and 0xFF)
            write((bytesPerRow shr 8) and 0xFF)
            write(bitmap.height and 0xFF)
            write((bitmap.height shr 8) and 0xFF)
            write(raster)
        }.toByteArray()
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
        const val PRINTER_DOTS_PER_LINE = 576
        val PRINTER_CHARSET: Charset = Charset.forName("CP850")
    }
}
