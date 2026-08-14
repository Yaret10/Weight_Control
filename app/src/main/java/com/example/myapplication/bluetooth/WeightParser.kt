package com.example.myapplication.bluetooth

object WeightParser {
    /**
     * Parse strings like "ST,GS,+  12.50kg" or "US,GS,+  10.00kg"
     * Returns the numeric value or null if invalid.
     */
    fun parse(data: String): Double? {
        return try {
            // Buscamos el valor numérico. Ej: "+  12.50"
            // Una forma simple es extraer todo lo que parezca un número con decimales
            val regex = """[+-]?\s*(\d+\.?\d*)""".toRegex()
            val match = regex.find(data)
            match?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun isStable(data: String): Boolean {
        return data.split(',', ' ', '\r', '\n').any { it.equals("ST", ignoreCase = true) }
    }

    fun isUnstable(data: String): Boolean {
        return data.split(',', ' ', '\r', '\n').any { it.equals("US", ignoreCase = true) }
    }
}
