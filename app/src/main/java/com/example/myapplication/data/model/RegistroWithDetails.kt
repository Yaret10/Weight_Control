package com.example.myapplication.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class RegistroWithDetails(
    @Embedded val registro: Registro,
    @Relation(
        parentColumn = "clienteId",
        entityColumn = "id"
    )
    val cliente: Cliente,
    @Relation(
        parentColumn = "productoId",
        entityColumn = "id"
    )
    val producto: Producto,
    @Relation(
        parentColumn = "operadorId",
        entityColumn = "id"
    )
    val operador: Operador?
)
