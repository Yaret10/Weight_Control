package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operadores")
data class Operador(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String
)
