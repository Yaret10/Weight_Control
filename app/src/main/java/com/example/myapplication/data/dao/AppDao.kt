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

    // Registros
    @Insert
    suspend fun insertRegistro(registro: Registro)

    @Query("DELETE FROM registros")
    suspend fun deleteAllRegistros()

    @Query("DELETE FROM registros WHERE id = :registroId")
    suspend fun deleteRegistro(registroId: Int)

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
