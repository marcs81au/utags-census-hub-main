package com.utags.androidpc.CensusHub.presentation.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utags.androidpc.CensusHub.domain.model.ConnectionConfig
import com.utags.androidpc.CensusHub.domain.model.ConnectionType
import com.utags.androidpc.CensusHub.domain.repository.ReaderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val selectedTypeIndex: Int = 3, // Default Bluetooth
    val connectionParam: String = "",
    val bluetoothDevices: List<String> = emptyList(),
    val usbSerialDevices: List<String> = emptyList(),
    val usbHidDevices: List<String> = emptyList(),
    val connId: String = "",
    val appVersion: String = "",
    val sdkVersion: String = "",
    val errorMessage: String? = null,
    val message: String? = null
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val repository: ReaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(
                    isConnected = connected,
                    connId = if (connected) repository.connId.value else "",
                    sdkVersion = repository.getSdkVersion()
                )
            }
        }
    }

    fun setAppVersion(version: String) {
        _uiState.value = _uiState.value.copy(appVersion = version)
    }

    fun onTypeSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTypeIndex = index)
        val type = ConnectionType.fromIndex(index)
        when (type) {
            ConnectionType.RS232 -> _uiState.value = _uiState.value.copy(connectionParam = "/dev/ttySAC1:115200")
            ConnectionType.TCP -> _uiState.value = _uiState.value.copy(connectionParam = "192.168.1.116:9090")
            ConnectionType.RS485 -> _uiState.value = _uiState.value.copy(connectionParam = "1:/dev/ttySAC1:115200")
            ConnectionType.USB_SERIAL -> loadUsbSerialDevices()
            ConnectionType.USB_RS485 -> loadUsbSerialDevices()
            ConnectionType.USB_HID -> loadUsbHidDevices()
            ConnectionType.BLUETOOTH -> loadBluetoothDevices()
        }
    }

    fun onParamChanged(param: String) {
        _uiState.value = _uiState.value.copy(connectionParam = param)
    }

    fun loadBluetoothDevices() {
        viewModelScope.launch {
            val devices = repository.getBluetoothDevices()
            _uiState.value = _uiState.value.copy(bluetoothDevices = devices)
            if (devices.isNotEmpty() && _uiState.value.connectionParam.isEmpty()) {
                _uiState.value = _uiState.value.copy(connectionParam = devices[0])
            }
        }
    }

    private fun loadUsbSerialDevices() {
        viewModelScope.launch {
            val devices = repository.getUsbSerialDevices()
            _uiState.value = _uiState.value.copy(usbSerialDevices = devices)
            if (devices.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(connectionParam = devices[0])
            }
        }
    }

    private fun loadUsbHidDevices() {
        viewModelScope.launch {
            val devices = repository.getUsbHidDevices()
            _uiState.value = _uiState.value.copy(usbHidDevices = devices)
            if (devices.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(connectionParam = devices[0])
            }
        }
    }

    fun connect(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isConnecting) return

        val type = ConnectionType.fromIndex(state.selectedTypeIndex)
        val param = state.connectionParam

        if (param.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter or select a connection parameter")
            return
        }

        _uiState.value = _uiState.value.copy(isConnecting = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.connect(ConnectionConfig(type = type, param = param))
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        connId = param,
                        message = "Connected successfully",
                        sdkVersion = repository.getSdkVersion()
                    )
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        errorMessage = "Connection failed: ${e.message}"
                    )
                }
            )
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, errorMessage = null)
    }

    fun openBluetoothSettings() {
        // Handled in UI layer via Intent
    }
}
