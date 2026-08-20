package com.example.myapplication.data

import com.example.myapplication.data.dao.AppDao
import com.example.myapplication.data.model.*
import kotlinx.coroutines.flow.Flow

class PesajeRepository(private val appDao: AppDao) {
    
    val allClientes = appDao.getAllClientes()
    val allProductos = appDao.getAllProductos()
    val allOperadores = appDao.getAllOperadores()

    suspend fun addCliente(cliente: Cliente) = appDao.insertCliente(cliente)
    suspend fun addProducto(producto: Producto) = appDao.insertProducto(producto)
    suspend fun addOperador(operador: Operador) = appDao.insertOperador(operador)
    suspend fun addRegistro(registro: Registro, year: Int) = appDao.insertRegistroConCorrelativo(registro, year)
    suspend fun deleteAllRegistros() = appDao.deleteAllRegistros()
    suspend fun deleteRegistro(registroId: Int) = appDao.deleteRegistro(registroId)
    suspend fun updateDatosTransporte(registroIds: List<Int>, placaVehiculo: String, conductor: String) =
        appDao.updateDatosTransporte(registroIds, placaVehiculo, conductor)

    fun getFilteredHistory(
        clienteId: Int? = null,
        productoId: Int? = null,
        fechaInicio: Long? = null,
        fechaFin: Long? = null
    ): Flow<List<RegistroWithDetails>> {
        return appDao.getFilteredRegistros(clienteId, productoId, fechaInicio, fechaFin)
    }
}
