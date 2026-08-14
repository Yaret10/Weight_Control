package com.example.myapplication.ui

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.bluetooth.BluetoothService
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.PesajeRepository
import com.example.myapplication.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(context: Context) : ViewModel() {
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val context = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application)
                MainViewModel(context)
            }
        }
    }

    private val repository: PesajeRepository
    private val bluetoothService = BluetoothService(context)

    init {
        val dao = AppDatabase.getDatabase(context).appDao()
        repository = PesajeRepository(dao)
    }

    // Bluetooth
    val currentWeight = bluetoothService.weightFlow
    val isWeightStable = bluetoothService.isWeightStable
    val isBluetoothConnected = bluetoothService.isConnected
    val isBluetoothConnecting = bluetoothService.isConnecting
    val bluetoothConnectionError = bluetoothService.connectionError
    private var connectionJob: Job? = null
    
    fun getPairedDevices() = bluetoothService.getPairedDevices()

    fun connectToDevice(device: BluetoothDevice) {
        connectionJob?.cancel()
        bluetoothService.disconnect()
        connectionJob = viewModelScope.launch {
            bluetoothService.connect(device)
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        bluetoothService.disconnect()
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }

    // Data
    val clientes = repository.allClientes
    val productos = repository.allProductos

    private val _selectedCliente = MutableStateFlow<Cliente?>(null)
    val selectedCliente: StateFlow<Cliente?> = _selectedCliente.asStateFlow()

    private val _selectedProducto = MutableStateFlow<Producto?>(null)
    val selectedProducto: StateFlow<Producto?> = _selectedProducto.asStateFlow()

    fun selectCliente(cliente: Cliente) {
        _selectedCliente.value = cliente
    }

    fun selectProducto(producto: Producto) {
        _selectedProducto.value = producto
    }

    private val _clienteFilter = MutableStateFlow<Int?>(null)
    private val _productoFilter = MutableStateFlow<Int?>(null)
    private val _fechaInicioFilter = MutableStateFlow<Long?>(null)
    private val _fechaFinFilter = MutableStateFlow<Long?>(null)

    val history = combine(
        _clienteFilter, _productoFilter, _fechaInicioFilter, _fechaFinFilter
    ) { c, p, start, end ->
        repository.getFilteredHistory(c, p, start, end)
    }.flatMapLatest { it }

    fun setFilters(clienteId: Int?, productoId: Int?, start: Long?, end: Long?) {
        _clienteFilter.value = clienteId
        _productoFilter.value = productoId
        _fechaInicioFilter.value = start
        _fechaFinFilter.value = end
    }

    fun addCliente(nombre: String, iden: String) {
        viewModelScope.launch { repository.addCliente(Cliente(nombre = nombre, identificacion = iden)) }
    }

    fun addProducto(nombre: String, desc: String) {
        viewModelScope.launch { repository.addProducto(Producto(nombre = nombre, descripcion = desc)) }
    }

    fun saveRegistro(clienteId: Int, productoId: Int, peso: Double) {
        viewModelScope.launch {
            repository.addRegistro(Registro(
                clienteId = clienteId,
                productoId = productoId,
                peso = peso,
                fecha = System.currentTimeMillis()
            ))
        }
    }

    fun deleteAllRegistros() {
        viewModelScope.launch {
            repository.deleteAllRegistros()
        }
    }

    fun deleteRegistro(registroId: Int) {
        viewModelScope.launch {
            repository.deleteRegistro(registroId)
        }
    }
}
