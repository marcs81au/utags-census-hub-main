package com.utags.androidpc.CensusHub.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utags.androidpc.CensusHub.domain.repository.ReaderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AntennaSetting(
    val number: Int,
    val enabled: Boolean,
    val power: Int
)

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    // Antenna power settings (up to 8 antennas)
    val antennaSettings: List<AntennaSetting> = (1..8).map { AntennaSetting(it, it == 1, 30) },
    val antennaCount: Int = 1,
    val minPower: Int = 0,
    val maxPower: Int = 33,
    // Scan antenna selection (which antennas to scan with)
    val selectedScanAntennas: Set<Int> = setOf(1),
    // Frequency
    val frequencyIndex: Int = 0,
    // Baseband
    val baseSpeedTypeIndex: Int = 0,
    val baseSpeedQIndex: Int = 0,
    // Beep
    val beepEnabled: Boolean = true,
    // Tag type
    val tagTypeIndex: Int = 0,
    // Filter time
    val filterTime: String = "0",
    val rssiFilter: String = "0",
    // Version info
    val softwareVersion: String = "--",
    val firmwareVersion: String = "--",
    val serialNumber: String = "--",
    // Power panel open/closed
    val powerPanelExpanded: Boolean = false,
    // Standing wave result
    val swrResult: String? = null,
    val swrAntennaIndex: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ReaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.readerSettings.collect { settings ->
                val count = settings.antennaCount.coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(
                    antennaCount = count,
                    minPower = settings.minPower,
                    maxPower = settings.maxPower.coerceAtLeast(1),
                    tagTypeIndex = if (settings.tagType == "6C") 0 else 1
                )
            }
        }
        loadAll()
    }

    fun loadAll() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            // Reader property
            repository.getReaderProperty()

            val state = _uiState.value
            val antCount = repository.readerSettings.value.antennaCount.coerceAtLeast(1)

            // Version info
            val fw = repository.getFirmwareVersion()
            val serial = repository.getSerialNumber()

            // Power
            val powerResult = repository.getPower()
            val powerMap = powerResult.getOrNull() ?: emptyMap()
            val antSettings = (1..antCount).map { ant ->
                AntennaSetting(
                    number = ant,
                    enabled = ant == 1,
                    power = powerMap[ant] ?: 30
                )
            }

            // Frequency
            val freqResult = repository.getFrequency()
            val freqIdx = freqResult.getOrNull() ?: 0

            // Baseband
            val bbResult = repository.getBaseband()
            val (bbType, bbQ) = bbResult.getOrNull() ?: Pair(0, 0)

            // Filter time
            val filterResult = repository.getFilterTime()
            val (repeatTime, rssi) = filterResult.getOrNull() ?: Pair(0, 0)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                firmwareVersion = fw,
                serialNumber = serial,
                antennaSettings = antSettings,
                antennaCount = antCount,
                frequencyIndex = freqIdx,
                baseSpeedTypeIndex = if (bbType == 255) 14 else bbType,
                baseSpeedQIndex = if (bbQ == 4) 1 else 0,
                filterTime = repeatTime.toString(),
                rssiFilter = rssi.toString(),
                minPower = repository.readerSettings.value.minPower,
                maxPower = repository.readerSettings.value.maxPower.coerceAtLeast(1)
            )
        }
    }

    fun setPower() {
        viewModelScope.launch {
            val state = _uiState.value
            val enabledAntennas = state.antennaSettings.filter { it.enabled }
            if (enabledAntennas.isEmpty()) {
                _uiState.value = state.copy(message = "No antennas selected")
                return@launch
            }
            val powerMap = enabledAntennas.associate { it.number to it.power }
            val result = repository.setPower(powerMap)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Power set successfully" else "Set power failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun getPower() {
        viewModelScope.launch {
            val result = repository.getPower()
            result.fold(
                onSuccess = { powerMap ->
                    val updated = _uiState.value.antennaSettings.map { setting ->
                        powerMap[setting.number]?.let { pwr -> setting.copy(power = pwr) } ?: setting
                    }
                    _uiState.value = _uiState.value.copy(
                        antennaSettings = updated,
                        message = "Power retrieved"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(message = "Get power failed: ${e.message}")
                }
            )
        }
    }

    fun setFrequency() {
        viewModelScope.launch {
            val result = repository.setFrequency(_uiState.value.frequencyIndex)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Frequency set" else "Set frequency failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun setBaseband() {
        viewModelScope.launch {
            val state = _uiState.value
            val typeIdx = state.baseSpeedTypeIndex
            val qValue = if (state.baseSpeedQIndex == 1) 4 else 0
            val result = repository.setBaseband(typeIdx, qValue)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Baseband set" else "Set baseband failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun setBeep() {
        viewModelScope.launch {
            val enabled = _uiState.value.beepEnabled
            val result = repository.setBeep(enabled)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Beep ${if (enabled) "ON" else "OFF"}" else "Set beep failed"
            )
        }
    }

    fun setFilterTime() {
        viewModelScope.launch {
            val state = _uiState.value
            val repeat = state.filterTime.toIntOrNull() ?: 0
            val rssi = state.rssiFilter.toIntOrNull() ?: 0
            val result = repository.setFilterTime(repeat, rssi)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Filter time set" else "Set filter time failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun setTagType() {
        val type = if (_uiState.value.tagTypeIndex == 0) "6C" else "6B"
        repository.setTagType(type)
        _uiState.value = _uiState.value.copy(message = "Tag type set to $type")
    }

    fun restoreFactory() {
        viewModelScope.launch {
            val result = repository.restoreFactory()
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Factory defaults restored" else "Restore failed: ${result.exceptionOrNull()?.message}"
            )
            if (result.isSuccess) loadAll()
        }
    }

    fun testStandingWave() {
        viewModelScope.launch {
            val state = _uiState.value
            val antNo = 1 shl state.swrAntennaIndex
            val result = repository.getStandingWaveRatio(antNo)
            _uiState.value = _uiState.value.copy(
                swrResult = result.getOrNull() ?: result.exceptionOrNull()?.message,
                message = null
            )
        }
    }

    fun setScanAntennas() {
        val state = _uiState.value
        var mask = 0
        state.selectedScanAntennas.forEach { ant -> mask += ant }
        _uiState.value = state.copy(message = "Antenna selection applied")
    }

    // State mutators
    fun onAntennaPowerChanged(antNumber: Int, power: Int) {
        val updated = _uiState.value.antennaSettings.map {
            if (it.number == antNumber) it.copy(power = power) else it
        }
        _uiState.value = _uiState.value.copy(antennaSettings = updated)
    }

    fun onAntennaEnabledChanged(antNumber: Int, enabled: Boolean) {
        val updated = _uiState.value.antennaSettings.map {
            if (it.number == antNumber) it.copy(enabled = enabled) else it
        }
        _uiState.value = _uiState.value.copy(antennaSettings = updated)
    }

    fun onScanAntennaToggled(antNumber: Int, selected: Boolean) {
        val current = _uiState.value.selectedScanAntennas.toMutableSet()
        if (selected) current.add(antNumber) else current.remove(antNumber)
        _uiState.value = _uiState.value.copy(selectedScanAntennas = current)
    }

    fun onFrequencySelected(index: Int) {
        _uiState.value = _uiState.value.copy(frequencyIndex = index)
    }

    fun onBaseSpeedTypeSelected(index: Int) {
        _uiState.value = _uiState.value.copy(baseSpeedTypeIndex = index)
    }

    fun onBaseSpeedQSelected(index: Int) {
        _uiState.value = _uiState.value.copy(baseSpeedQIndex = index)
    }

    fun onBeepToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(beepEnabled = enabled)
    }

    fun onTagTypeSelected(index: Int) {
        _uiState.value = _uiState.value.copy(tagTypeIndex = index)
    }

    fun onFilterTimeChanged(value: String) {
        _uiState.value = _uiState.value.copy(filterTime = value)
    }

    fun onRssiFilterChanged(value: String) {
        _uiState.value = _uiState.value.copy(rssiFilter = value)
    }

    fun onPowerPanelToggled() {
        _uiState.value = _uiState.value.copy(powerPanelExpanded = !_uiState.value.powerPanelExpanded)
    }

    fun onSwrAntennaSelected(index: Int) {
        _uiState.value = _uiState.value.copy(swrAntennaIndex = index)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearSwrResult() {
        _uiState.value = _uiState.value.copy(swrResult = null)
    }

    fun getComputedAntennaNo(): Int {
        var mask = 0
        _uiState.value.selectedScanAntennas.forEach { ant -> mask += ant }
        return if (mask == 0) 1 else mask
    }
}
