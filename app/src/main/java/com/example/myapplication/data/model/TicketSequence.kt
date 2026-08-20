package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ticket_sequences")
data class TicketSequence(
    @PrimaryKey val year: Int,
    val lastNumber: Int
)
