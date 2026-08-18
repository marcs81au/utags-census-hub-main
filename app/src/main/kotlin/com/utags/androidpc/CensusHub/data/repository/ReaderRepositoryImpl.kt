package com.utags.androidpc.CensusHub.data.repository

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.rfidread.Enumeration.eRF_Range
import com.rfidread.Enumeration.eReadType
import com.rfidread.Interface.IAsynchronousMessage
import com.rfidread.Models.GPI_Model
import com.rfidread.Models.Tag_Model
import com.rfidread.RFIDReader
import com.rfidread.ReaderConfig
import com.rfidread.Tag6C
import com.rfidread.usbserial.driver.UsbSerialPort
import com.utags.androidpc.CensusHub.domain.model.ConnectionConfig
import com.utags.androidpc.CensusHub.domain.model.ConnectionType
import com.utags.androidpc.CensusHub.domain.model.ReaderSettings
import com.utags.androidpc.CensusHub.domain.model.TagEntry
import com.utags.androidpc.CensusHub.domain.repository.ReaderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReaderRepository {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connId = MutableStateFlow("")
    override val connId: StateFlow<String> = _connId.asStateFlow()

    private val _readerSettings = MutableStateFlow(ReaderSettings())
    override val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _incomingTags = MutableSharedFlow<TagEntry>(extraBufferCapacity = 256)
    override val incomingTags: SharedFlow<TagEntry> = _incomingTags.asSharedFlow()

    private val _scannedEpcs = MutableStateFlow<List<String>>(emptyList())
    override val scannedEpcs: StateFlow<List<String>> = _scannedEpcs.asStateFlow()

    private val _matchTidChannel = Channel<String>(Channel.CONFLATED)
    val matchTidChannel = _matchTidChannel

    // USB device lists kept in repo to avoid passing Context everywhere
    private var usbSerialPorts: List<UsbSerialPort> = emptyList()
    private var usbHidDevices: List<UsbDevice> = emptyList()

    private val rfidCallback = object : IAsynchronousMessage {
        override fun WriteDebugMsg(s: String, s1: String) {}
        override fun WriteLog(s: String, s1: String) {}
        override fun PortConnecting(s: String) {}
        override fun PortClosing(s: String) {
            _isConnected.tryEmit(false)
        }

        override fun OutPutTags(model: Tag_Model) {
            val entry = TagEntry(
                epc = model._EPC ?: "",
                tid = model._TID ?: "",
                userData = model._UserData ?: ""
            )
            _incomingTags.tryEmit(entry)

            // Track EPC for write screen
            val epc = model._EPC ?: ""
            if (epc.isNotEmpty() && !_scannedEpcs.value.contains(epc)) {
                _scannedEpcs.tryEmit(_scannedEpcs.value + epc)
            }
        }

        override fun OutPutTagsOver(s: String) {}

        override fun GPIControlMsg(s: String, gpi_model: GPI_Model) {}

        override fun OutPutScanData(s: String, bytes: ByteArray) {}
    }

    private val matchCallback = object : IAsynchronousMessage {
        override fun WriteDebugMsg(s: String, s1: String) {}
        override fun WriteLog(s: String, s1: String) {}
        override fun PortConnecting(s: String) {}
        override fun PortClosing(s: String) {}
        override fun OutPutTags(model: Tag_Model) {
            val tid = model._TID ?: model._EPC ?: ""
            if (tid.isNotEmpty()) {
                _matchTidChannel.trySend(tid)
            }
        }
        override fun OutPutTagsOver(s: String) {}
        override fun GPIControlMsg(s: String, gpi_model: GPI_Model) {}
        override fun OutPutScanData(s: String, bytes: ByteArray) {}
    }

    override suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        if (_isConnected.value) return@withContext Result.success(Unit)
        try {
            val param = config.param
            val ok = when (config.type) {
                ConnectionType.RS232 -> RFIDReader.CreateSerialConn(param, rfidCallback)
                ConnectionType.TCP -> RFIDReader.CreateTcpConn(param, rfidCallback)
                ConnectionType.USB_SERIAL -> {
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    if (!RFIDReader.GetUsbSerialPermission(null, param)) {
                        return@withContext Result.failure(Exception("USB permission denied"))
                    }
                    RFIDReader.CreateUsbSerialConn(param, rfidCallback)
                }
                ConnectionType.BLUETOOTH -> RFIDReader.CreateBT4Conn(param, rfidCallback)
                ConnectionType.RS485 -> RFIDReader.Create485Conn(param, rfidCallback)
                ConnectionType.USB_RS485 -> {
                    val usbParam = param.split(":").getOrElse(1) { param }
                    if (!RFIDReader.GetUsbSerialPermission(null, usbParam)) {
                        return@withContext Result.failure(Exception("USB permission denied"))
                    }
                    RFIDReader.CreateUsb485Conn(param, rfidCallback)
                }
                ConnectionType.USB_HID -> {
                    if (!RFIDReader.GetUsbHIDPermission(null, param)) {
                        return@withContext Result.failure(Exception("USB HID permission denied"))
                    }
                    RFIDReader.CreateUsbConn(param, rfidCallback)
                }
            }
            Thread.sleep(500)
            if (ok) {
                _connId.value = param
                _isConnected.value = true
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            Log.e("ReaderRepo", "Connection error", e)
            Result.failure(e)
        }
    }

    override fun disconnect() {
        val id = _connId.value
        if (id.isNotEmpty()) {
            try { RFIDReader.CloseConn(id) } catch (_: Exception) {}
        }
        _isConnected.value = false
        _connId.value = ""
    }

    override suspend fun getBluetoothDevices(): List<String> = withContext(Dispatchers.IO) {
        try { RFIDReader.GetBT4DeviceStrList() ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    override suspend fun getUsbSerialDevices(): List<String> = withContext(Dispatchers.IO) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            usbSerialPorts = RFIDReader.GetUSBSerialList(usbManager) ?: emptyList()
            RFIDReader.GetUsbSerialDeviceStrList(usbSerialPorts) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getUsbHidDevices(): List<String> = withContext(Dispatchers.IO) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            usbHidDevices = RFIDReader.GetUSBHIDList(usbManager) ?: emptyList()
            RFIDReader.GetUsbHIDDeviceStrList(usbHidDevices) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun startScan(antennaNo: Int, readMode: Int, tagType: String): Unit = withContext(Dispatchers.IO) {
        val id = _connId.value
        try {
            RFIDReader.setAsynchronousMessage(id, rfidCallback)
            if (tagType == "6C") {
                when (readMode) {
                    0 -> Tag6C.GetEPC(id, antennaNo, eReadType.Inventory)
                    1 -> Tag6C.GetEPC_TID(id, antennaNo, eReadType.Inventory)
                    2 -> Tag6C.GetEPC_TID_UserData(id, antennaNo, eReadType.Inventory, 0, 6)
                    else -> Tag6C.GetEPC(id, antennaNo, eReadType.Inventory)
                }
            } else {
                when (readMode) {
                    2 -> RFIDReader._Tag6B.Get6B_UserData(id, antennaNo, eReadType.Inventory.GetNum(), 1, 0, 15)
                    else -> RFIDReader._Tag6B.Get6B(id, antennaNo, eReadType.Inventory.GetNum(), 0)
                }
            }
        } catch (e: Exception) {
            Log.e("ReaderRepo", "Start scan error", e)
        }
        Unit
    }

    override suspend fun stopScan(): Unit = withContext(Dispatchers.IO) {
        try { RFIDReader._Config.Stop(_connId.value) } catch (e: Exception) {
            Log.e("ReaderRepo", "Stop scan error", e)
        }
        Unit
    }

    override suspend fun getReaderProperty(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val prop = RFIDReader.GetReaderProperty(_connId.value) ?: return@withContext Result.failure(Exception("No property"))
            val arr = prop.split("|")
            if (arr.size > 2) {
                val minP = arr[0].toIntOrNull() ?: 0
                val maxP = arr[1].toIntOrNull() ?: 30
                val antCount = arr[2].toIntOrNull() ?: 1
                _readerSettings.value = _readerSettings.value.copy(
                    minPower = minP, maxPower = maxP, antennaCount = antCount
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPower(): Result<Map<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val raw = RFIDReader._Config.GetANTPowerParam2(_connId.value) ?: return@withContext Result.failure(Exception("No power data"))
            val map = mutableMapOf<Int, Int>()
            raw.split("&").forEach { item ->
                val parts = item.split(",")
                if (parts.size == 2) {
                    val ant = parts[0].toIntOrNull() ?: return@forEach
                    val pwr = parts[1].toIntOrNull() ?: return@forEach
                    map[ant] = pwr
                }
            }
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setPower(powers: Map<Int, Int>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val javaMap = HashMap<Int, Int>(powers)
            val ret = RFIDReader._Config.SetANTPowerParam(_connId.value, javaMap)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Set power failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFrequency(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val rf = RFIDReader._Config.GetReaderRF(_connId.value)
            Result.success(rf.GetNum())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setFrequency(index: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = RFIDReader._Config.SetReaderRF(_connId.value, eRF_Range.GetEnum(index))
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Set frequency failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBaseband(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val raw = RFIDReader._Config.GetEPCBaseBandParam(_connId.value) ?: return@withContext Result.failure(Exception("No baseband data"))
            val parts = raw.split("|")
            val typeIdx = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val qRaw = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val qIdx = if (qRaw == 4) 1 else 0
            Result.success(Pair(typeIdx, qIdx))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setBaseband(typeIndex: Int, qValue: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val actualType = if (typeIndex == 14) 255 else typeIndex  // index 14 = AUTO = 255
            val ret = RFIDReader._Config.SetEPCBaseBandParam(_connId.value, actualType, qValue, null, null)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Set baseband failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setBeep(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            RFIDReader.SetBeep(_connId.value, if (enabled) 0 else 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFilterTime(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val raw = RFIDReader._Config.GetTagUpdateParam(_connId.value) ?: return@withContext Result.failure(Exception("No filter data"))
            val parts = raw.split("|")
            val repeat = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val rssi = parts.getOrNull(1)?.toIntOrNull() ?: 0
            Result.success(Pair(repeat, rssi))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setFilterTime(repeatTimeFilter: Int, rssiFilter: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = RFIDReader._Config.SetTagUpdateParam(_connId.value, repeatTimeFilter, rssiFilter)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Set filter time failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setTagType(type: String) {
        _readerSettings.value = _readerSettings.value.copy(tagType = type)
    }

    override suspend fun restoreFactory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = RFIDReader._Config.SetReaderRestoreFactory(_connId.value)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Restore factory failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStandingWaveRatio(antNo: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = RFIDReader._Config.GetAntennaStandingWaveRatio(_connId.value, antNo)
            Result.success(result ?: "N/A")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFirmwareVersion(): String = try {
        ReaderConfig.GetReaderBaseBandSoftVersion(_connId.value) ?: "--"
    } catch (_: Exception) { "--" }

    override fun getSerialNumber(): String = try {
        RFIDReader.GetSerialNum(_connId.value) ?: "--"
    } catch (_: Exception) { "--" }

    override fun getSdkVersion(): String = try {
        RFIDReader.GetVER() ?: "--"
    } catch (_: Exception) { "--" }

    override suspend fun writeEpcByTid(epc: String, tid: String, accessPwd: String, antennaNo: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = Tag6C.WriteEPC_MatchTID(_connId.value, antennaNo, epc, tid, 0, accessPwd)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Write EPC failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeUserDataByTid(data: String, tid: String, accessPwd: String, antennaNo: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = Tag6C.WriteUserData_MatchTID(_connId.value, antennaNo, data, 0, tid, 0, accessPwd)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Write UserData failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeAccessPasswordByTid(newPwd: String, tid: String, currentPwd: String, antennaNo: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = Tag6C.WriteAccessPassWord_MatchTID(_connId.value, antennaNo, newPwd, tid, 0, currentPwd)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Write Access Password failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun write6B(tid: String, data: String, antennaNo: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ret = RFIDReader._Tag6B.Write6B(_connId.value, antennaNo, tid, 8, data)
            if (ret == 0) Result.success(Unit) else Result.failure(Exception("Write 6B failed: $ret"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readTagForMatch(antennaNo: Int, tagType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            RFIDReader.setAsynchronousMessage(_connId.value, matchCallback)
            if (tagType == "6C") {
                Tag6C.GetEPC_TID(_connId.value, antennaNo, eReadType.Inventory)
            } else {
                RFIDReader._Tag6B.Get6B(_connId.value, antennaNo, eReadType.Inventory.GetNum(), 0)
            }

            // Wait up to 2 seconds for a TID response
            var tid = ""
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 2000) {
                val received = _matchTidChannel.tryReceive().getOrNull()
                if (received != null) {
                    tid = received
                    break
                }
                Thread.sleep(50)
            }
            RFIDReader._Config.Stop(_connId.value)
            // Restore main callback
            RFIDReader.setAsynchronousMessage(_connId.value, rfidCallback)

            if (tid.isNotEmpty()) Result.success(tid)
            else Result.failure(Exception("No tag found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun clearScannedEpcs() {
        _scannedEpcs.value = emptyList()
    }
}
