package com.example.myapplication.ui

import android.bluetooth.BluetoothDevice
import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.bluetooth.BluetoothService
import com.example.myapplication.bluetooth.BluetoothPrinterService
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.PesajeRepository
import com.example.myapplication.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Calendar

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(context: Context) : ViewModel() {
    
    companion object {
        private const val SAVED_SCALE_ADDRESS = "address"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val context = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application)
                MainViewModel(context)
            }
        }
    }

    private val repository: PesajeRepository
    private val bluetoothService = BluetoothService(context)
    private val printerService = BluetoothPrinterService(context)
    private val bluetoothPreferences =
        context.getSharedPreferences("bluetooth_scale", Context.MODE_PRIVATE)

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
    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()
    private val _printMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val printMessages: SharedFlow<String> = _printMessages.asSharedFlow()
    private val _lastSavedRegistro = MutableStateFlow<RegistroWithDetails?>(null)
    val lastSavedRegistro: StateFlow<RegistroWithDetails?> = _lastSavedRegistro.asStateFlow()
    
    fun getPairedDevices() = bluetoothService.getPairedDevices()

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothPreferences.edit().putString(SAVED_SCALE_ADDRESS, device.address).apply()
        connectionJob?.cancel()
        bluetoothService.disconnect()
        connectionJob = viewModelScope.launch {
            bluetoothService.connect(device)
        }
    }

    /** Reconecta la última balanza seleccionada; la primera selección sigue siendo manual. */
    @SuppressLint("MissingPermission")
    fun autoConnectSavedScale() {
        if (isBluetoothConnected.value || isBluetoothConnecting.value) return
        val savedAddress = bluetoothPreferences.getString(SAVED_SCALE_ADDRESS, null) ?: return
        val savedDevice = bluetoothService.getPairedDevices()
            .firstOrNull { it.address.equals(savedAddress, ignoreCase = true) }
            ?: return
        connectToDevice(savedDevice)
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        bluetoothService.disconnect()
    }

    fun printRegistros(registros: List<RegistroWithDetails>, placaVehiculo: String = "", conductor: String = "") {
        if (_isPrinting.value || registros.isEmpty()) return
        viewModelScope.launch {
            _isPrinting.value = true
            val placa = placaVehiculo.trim()
            val nombreConductor = conductor.trim()
            try {
                repository.updateDatosTransporte(
                    registros.map { it.registro.id },
                    placa,
                    nombreConductor
                )
                printerService.print(registros, placa, nombreConductor).fold(
                    onSuccess = { _printMessages.emit(it) },
                    onFailure = { error ->
                        _printMessages.emit("No se pudo imprimir: ${error.localizedMessage ?: "error de Bluetooth"}")
                    }
                )
            } catch (error: Exception) {
                _printMessages.emit("No se pudieron guardar los datos: ${error.localizedMessage ?: "error de base de datos"}")
            } finally {
                _isPrinting.value = false
            }
        }
    }

    override fun onCleared() {
        disconnect()
        printerService.close()
        super.onCleared()
    }

    // Data
    val clientes = repository.allClientes
    val productos = repository.allProductos
    val operadores = repository.allOperadores

    private val _selectedCliente = MutableStateFlow<Cliente?>(null)
    val selectedCliente: StateFlow<Cliente?> = _selectedCliente.asStateFlow()

    private val _selectedProducto = MutableStateFlow<Producto?>(null)
    val selectedProducto: StateFlow<Producto?> = _selectedProducto.asStateFlow()

    private val _selectedOperador = MutableStateFlow<Operador?>(null)
    val selectedOperador: StateFlow<Operador?> = _selectedOperador.asStateFlow()

    private val _taraRegistrada = MutableStateFlow(0.0)
    val taraRegistrada: StateFlow<Double> = _taraRegistrada.asStateFlow()

    private val _taraConfigurada = MutableStateFlow(false)
    val taraConfigurada: StateFlow<Boolean> = _taraConfigurada.asStateFlow()

    private val _indicadorTarado = MutableStateFlow(false)
    val indicadorTarado: StateFlow<Boolean> = _indicadorTarado.asStateFlow()

    private val _isTareCommandRunning = MutableStateFlow(false)
    val isTareCommandRunning: StateFlow<Boolean> = _isTareCommandRunning.asStateFlow()

    fun selectCliente(cliente: Cliente) {
        _selectedCliente.value = cliente
    }

    fun selectProducto(producto: Producto) {
        _selectedProducto.value = producto
    }

    fun selectOperador(operador: Operador) {
        _selectedOperador.value = operador
    }

    fun capturarTara(peso: Double) {
        if (peso <= 0.0 || _taraConfigurada.value || _isTareCommandRunning.value) return
        viewModelScope.launch {
            _isTareCommandRunning.value = true
            bluetoothService.tareIndicator().fold(
                onSuccess = {
                    _taraRegistrada.value = peso
                    _taraConfigurada.value = true
                    _indicadorTarado.value = false
                    _printMessages.emit("Comando T enviado al indicador")
                },
                onFailure = { error ->
                    _printMessages.emit("No se pudo aplicar la tara: ${error.localizedMessage}")
                }
            )
            _isTareCommandRunning.value = false
        }
    }

    fun registrarTara(peso: Double) {
        if (peso >= 0.0 && !_taraConfigurada.value) {
            _taraRegistrada.value = peso
            _taraConfigurada.value = true
            _indicadorTarado.value = true
        }
    }

    fun confirmarIndicadorTarado() {
        if (_taraRegistrada.value > 0.0) _indicadorTarado.value = true
    }

    fun limpiarTara() {
        if (_isTareCommandRunning.value) return
        viewModelScope.launch {
            _isTareCommandRunning.value = true
            bluetoothService.clearTareIndicator().fold(
                onSuccess = {
                    _taraRegistrada.value = 0.0
                    _taraConfigurada.value = false
                    _indicadorTarado.value = false
                    _printMessages.emit("Comando C enviado al indicador")
                },
                onFailure = { error ->
                    _printMessages.emit("No se pudo quitar la tara: ${error.localizedMessage}")
                }
            )
            _isTareCommandRunning.value = false
        }
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

    fun addOperador(nombre: String) {
        viewModelScope.launch { repository.addOperador(Operador(nombre = nombre)) }
    }

    fun saveRegistro(
        clienteId: Int,
        productoId: Int,
        operadorId: Int,
        placaVehiculo: String,
        conductor: String,
        observacion: String,
        pesoNeto: Double,
        pesoTara: Double
    ) {
        viewModelScope.launch {
            _lastSavedRegistro.value = null
            val timestamp = System.currentTimeMillis()
            val year = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
            _lastSavedRegistro.value = repository.addRegistro(Registro(
                clienteId = clienteId,
                productoId = productoId,
                operadorId = operadorId,
                codigoTicket = "",
                placaVehiculo = placaVehiculo,
                conductor = conductor,
                observacion = observacion,
                pesoBruto = pesoNeto + pesoTara,
                pesoTara = pesoTara,
                peso = pesoNeto,
                fecha = timestamp
            ), year)
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
