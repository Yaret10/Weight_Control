package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Clientes
    @Insert
    suspend fun insertCliente(cliente: Cliente)

    @Query("SELECT * FROM clientes ORDER BY nombre ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    // Productos
    @Insert
    suspend fun insertProducto(producto: Producto)

    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProductos(): Flow<List<Producto>>

    // Operadores
    @Insert
    suspend fun insertOperador(operador: Operador)

    @Query("SELECT * FROM operadores ORDER BY nombre ASC")
    fun getAllOperadores(): Flow<List<Operador>>

    // Registros
    @Insert
    suspend fun insertRegistro(registro: Registro)

    @Query("SELECT lastNumber FROM ticket_sequences WHERE year = :year")
    suspend fun getLastTicketNumber(year: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTicketSequence(sequence: TicketSequence)

    @Transaction
    suspend fun insertRegistroConCorrelativo(registro: Registro, year: Int): String {
        val next = (getLastTicketNumber(year) ?: 0) + 1
        val code = "%04d-%05d".format(year, next)
        saveTicketSequence(TicketSequence(year, next))
        insertRegistro(registro.copy(codigoTicket = code))
        return code
    }

    @Query("DELETE FROM registros")
    suspend fun deleteAllRegistros()

    @Query("DELETE FROM registros WHERE id = :registroId")
    suspend fun deleteRegistro(registroId: Int)

    @Query("""
        UPDATE registros
        SET placaVehiculo = CASE WHEN :placaVehiculo = '' THEN placaVehiculo ELSE :placaVehiculo END,
            conductor = CASE WHEN :conductor = '' THEN conductor ELSE :conductor END
        WHERE id IN (:registroIds)
    """)
    suspend fun updateDatosTransporte(
        registroIds: List<Int>,
        placaVehiculo: String,
        conductor: String
    )

    @Transaction
    @Query("""
        SELECT * FROM registros 
        WHERE (:clienteId IS NULL OR clienteId = :clienteId)
        AND (:productoId IS NULL OR productoId = :productoId)
        AND (:fechaInicio IS NULL OR fecha >= :fechaInicio)
        AND (:fechaFin IS NULL OR fecha <= :fechaFin)
        ORDER BY fecha DESC
    """)
    fun getFilteredRegistros(
        clienteId: Int? = null,
        productoId: Int? = null,
        fechaInicio: Long? = null,
        fechaFin: Long? = null
    ): Flow<List<RegistroWithDetails>>
}
