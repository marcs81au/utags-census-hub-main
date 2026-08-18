package com.utags.androidpc.CensusHub.presentation.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utags.androidpc.CensusHub.domain.repository.ReaderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WriteType { EPC, USER_DATA, ACCESS_PASSWORD }

data class WriteUiState(
    val matchTid: String = "",
    val writeData: String = "",
    val accessPassword: String = "00000000",
    val writeType: WriteType = WriteType.EPC,
    val scannedEpcs: List<String> = emptyList(),
    val selectedEpcIndex: Int = 0,
    val isReading: Boolean = false,
    val isWriting: Boolean = false,
    val message: String? = null,
    val tagType: String = "6C"
)

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val repository: ReaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WriteUiState())
    val uiState: StateFlow<WriteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.scannedEpcs.collect { epcs ->
                _uiState.value = _uiState.value.copy(scannedEpcs = epcs)
            }
        }
        viewModelScope.launch {
            repository.readerSettings.collect { settings ->
                _uiState.value = _uiState.value.copy(tagType = settings.tagType)
            }
        }
    }

    fun onMatchTidChanged(value: String) {
        _uiState.value = _uiState.value.copy(matchTid = value)
    }

    fun onWriteDataChanged(value: String) {
        _uiState.value = _uiState.value.copy(writeData = value)
    }

    fun onAccessPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(accessPassword = value)
    }

    fun onWriteTypeSelected(type: WriteType) {
        _uiState.value = _uiState.value.copy(writeType = type)
    }

    fun onEpcSelected(index: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedEpcIndex = index,
            writeData = state.scannedEpcs.getOrElse(index) { "" }
        )
    }

    fun readMatchTag(antennaNo: Int) {
        if (_uiState.value.isReading) return
        _uiState.value = _uiState.value.copy(isReading = true)
        viewModelScope.launch {
            val result = repository.readTagForMatch(antennaNo, _uiState.value.tagType)
            result.fold(
                onSuccess = { tid ->
                    _uiState.value = _uiState.value.copy(matchTid = tid, isReading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isReading = false,
                        message = "No tag found: ${e.message}"
                    )
                }
            )
        }
    }

    fun writeData(antennaNo: Int) {
        val state = _uiState.value
        if (state.isWriting) return

        val data = state.writeData
        val tid = state.matchTid
        val pwd = state.accessPassword

        // Validate hex input
        if (!isValidHex(data)) {
            _uiState.value = _uiState.value.copy(message = "Data error! Must be hex characters only.")
            return
        }

        if (state.writeType == WriteType.ACCESS_PASSWORD && pwd.length != 8) {
            _uiState.value = _uiState.value.copy(message = "Access password must be 8 hex characters.")
            return
        }

        if (data.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Write data is empty.")
            return
        }

        _uiState.value = state.copy(isWriting = true)

        viewModelScope.launch {
            val result: Result<Unit> = if (state.tagType == "6C") {
                when (state.writeType) {
                    WriteType.EPC -> repository.writeEpcByTid(data, tid, pwd, antennaNo)
                    WriteType.USER_DATA -> repository.writeUserDataByTid(data, tid, pwd, antennaNo)
                    WriteType.ACCESS_PASSWORD -> repository.writeAccessPasswordByTid(data, tid, pwd, antennaNo)
                }
            } else {
                repository.write6B(tid, data, antennaNo)
            }

            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isWriting = false, message = "Success") },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isWriting = false, message = "Failed: ${e.message}") }
            )
        }
    }

    fun clearWriteData() {
        _uiState.value = _uiState.value.copy(writeData = "")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun isValidHex(input: String): Boolean {
        return input.matches(Regex("^[a-fA-F0-9]*$"))
    }
}
