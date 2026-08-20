package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Operador::class,
            parentColumns = ["id"],
            childColumns = ["operadorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("clienteId"), Index("productoId"), Index("operadorId"), Index(value = ["codigoTicket"], unique = true)]
)
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clienteId: Int,
    val productoId: Int,
    val operadorId: Int?,
    val codigoTicket: String,
    val placaVehiculo: String,
    val conductor: String,
    val pesoBruto: Double,
    val pesoTara: Double,
    val peso: Double,
    val fecha: Long // Timestamp
)
