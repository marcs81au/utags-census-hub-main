package com.utags.androidpc.CensusHub.domain.repository

import com.utags.androidpc.CensusHub.domain.model.ConnectionConfig
import com.utags.androidpc.CensusHub.domain.model.ReaderSettings
import com.utags.androidpc.CensusHub.domain.model.TagEntry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ReaderRepository {

    val isConnected: StateFlow<Boolean>
    val connId: StateFlow<String>
    val readerSettings: StateFlow<ReaderSettings>
    val incomingTags: SharedFlow<TagEntry>
    val scannedEpcs: StateFlow<List<String>>

    suspend fun connect(config: ConnectionConfig): Result<Unit>
    fun disconnect()

    suspend fun getBluetoothDevices(): List<String>
    suspend fun getUsbSerialDevices(): List<String>
    suspend fun getUsbHidDevices(): List<String>

    suspend fun startScan(antennaNo: Int, readMode: Int, tagType: String)
    suspend fun stopScan()

    suspend fun getReaderProperty(): Result<Unit>

    // Power
    suspend fun getPower(): Result<Map<Int, Int>>
    suspend fun setPower(powers: Map<Int, Int>): Result<Unit>

    // Frequency
    suspend fun getFrequency(): Result<Int>
    suspend fun setFrequency(index: Int): Result<Unit>

    // Baseband
    suspend fun getBaseband(): Result<Pair<Int, Int>>
    suspend fun setBaseband(typeIndex: Int, qValue: Int): Result<Unit>

    // Beep
    suspend fun setBeep(enabled: Boolean): Result<Unit>

    // Filter time
    suspend fun getFilterTime(): Result<Pair<Int, Int>>
    suspend fun setFilterTime(repeatTimeFilter: Int, rssiFilter: Int): Result<Unit>

    // Tag type
    fun setTagType(type: String)

    // Factory reset
    suspend fun restoreFactory(): Result<Unit>

    // Standing wave
    suspend fun getStandingWaveRatio(antNo: Int): Result<String>

    // Firmware / serial
    fun getFirmwareVersion(): String
    fun getSerialNumber(): String
    fun getSdkVersion(): String

    // Write operations
    suspend fun writeEpcByTid(epc: String, tid: String, accessPwd: String, antennaNo: Int): Result<Unit>
    suspend fun writeUserDataByTid(data: String, tid: String, accessPwd: String, antennaNo: Int): Result<Unit>
    suspend fun writeAccessPasswordByTid(newPwd: String, tid: String, currentPwd: String, antennaNo: Int): Result<Unit>
    suspend fun write6B(tid: String, data: String, antennaNo: Int): Result<Unit>

    // Read TID for write matching
    suspend fun readTagForMatch(antennaNo: Int, tagType: String): Result<String>

    fun clearScannedEpcs()
}
