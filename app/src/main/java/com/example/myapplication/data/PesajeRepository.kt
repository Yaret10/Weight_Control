package com.example.myapplication.data

import com.example.myapplication.data.dao.AppDao
import com.example.myapplication.data.model.*
import kotlinx.coroutines.flow.Flow

class PesajeRepository(private val appDao: AppDao) {
    
    val allClientes = appDao.getAllClientes()
    val allProductos = appDao.getAllProductos()

    suspend fun addCliente(cliente: Cliente) = appDao.insertCliente(cliente)
    suspend fun addProducto(producto: Producto) = appDao.insertProducto(producto)
    suspend fun addRegistro(registro: Registro) = appDao.insertRegistro(registro)
    suspend fun deleteAllRegistros() = appDao.deleteAllRegistros()
    suspend fun deleteRegistro(registroId: Int) = appDao.deleteRegistro(registroId)

    fun getFilteredHistory(
        clienteId: Int? = null,
        productoId: Int? = null,
        fechaInicio: Long? = null,
        fechaFin: Long? = null
    ): Flow<List<RegistroWithDetails>> {
        return appDao.getFilteredRegistros(clienteId, productoId, fechaInicio, fechaFin)
    }
}
