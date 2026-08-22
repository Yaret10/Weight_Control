package com.example.myapplication.bluetooth

object WeightParser {
    private const val F7_1_FRAME_LENGTH = 17
    private const val F7_1_WEIGHT_START = 3
    private const val F7_1_WEIGHT_END = 10
    private const val F7_1_WEIGHT_SCALE = 10.0

    /**
     * Parse strings like "ST,GS,+  12.50kg" or "US,GS,+  10.00kg"
     * Returns the numeric value or null if invalid.
     */
    fun parse(data: String): Double? {
        return try {
            parseF71(data)?.let { return it }

            // Buscamos el valor numérico. Ej: "+  12.50"
            // Una forma simple es extraer todo lo que parezca un número con decimales
            val regex = """[+-]?\s*(\d+\.?\d*)""".toRegex()
            val match = regex.find(data)
            match?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /** Formato continuo 1 del ID226: STX + estado(2) + peso(7) + tara(6) + CR. */
    private fun parseF71(data: String): Double? {
        if (data.length != F7_1_FRAME_LENGTH || data.first() != '\u0002' || data.last() != '\r') {
            return null
        }

        val rawWeight = data.substring(F7_1_WEIGHT_START, F7_1_WEIGHT_END).trim()
        return rawWeight.toDoubleOrNull()?.div(F7_1_WEIGHT_SCALE)
    }

    fun isStable(data: String): Boolean {
        return data.split(',', ' ', '\r', '\n').any { it.equals("ST", ignoreCase = true) }
    }

    fun isUnstable(data: String): Boolean {
        return data.split(',', ' ', '\r', '\n').any { it.equals("US", ignoreCase = true) }
    }
}
