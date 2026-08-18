package com.utags.androidpc.CensusHub.presentation.scan

import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utags.androidpc.CensusHub.domain.model.TagEntry
import com.utags.androidpc.CensusHub.domain.repository.ReaderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import javax.inject.Inject

enum class DisplayMode { HEX, ASCII }

data class ScanUiState(
    val isScanning: Boolean = false,
    val tags: List<TagEntry> = emptyList(),
    val tagCount: Int = 0,
    val readSpeed: Int = 0,
    val elapsedMs: Long = 0L,
    val displayMode: DisplayMode = DisplayMode.HEX,
    val beepEnabled: Boolean = true,
    val readMode: Int = 0,         // 0=EPC, 1=TID, 2=UserData
    val message: String? = null,
    val saveFileName: String? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Internal tag map — LinkedHashMap to preserve insertion order
    private val tagMap = LinkedHashMap<String, TagEntry>()
    private val tagMapLock = Any()
    private val tagFilterMap = HashMap<String, Long>()

    private var scanJob: Job? = null
    private var timerJob: Job? = null
    private var startTimeMs = 0L
    private var lastRefreshCount = 0
    private var totalTagEvents = 0

    private val toneGenerator = try {
        ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME)
    } catch (_: Exception) { null }

    val antennaNo: Int get() {
        // Expose the last-set antenna mask from settings; default to 3 (ant1+ant2)
        return currentAntennaNo
    }
    private var currentAntennaNo = 3

    val tagType: String get() = repository.readerSettings.value.tagType

    fun setAntennaNo(no: Int) { currentAntennaNo = no }

    fun startScan() {
        if (_uiState.value.isScanning) return
        clear()
        startTimeMs = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(isScanning = true)

        // Collect incoming tags
        scanJob = viewModelScope.launch {
            repository.incomingTags.collect { entry ->
                onTagReceived(entry)
            }
        }

        // Start actual scanning
        viewModelScope.launch {
            repository.startScan(currentAntennaNo, _uiState.value.readMode, tagType)
        }

        // Timer and refresh loop (every 1 second)
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTimeMs
                val speed = totalTagEvents - lastRefreshCount
                lastRefreshCount = totalTagEvents
                val snapshot = synchronized(tagMapLock) { tagMap.values.toList() }
                _uiState.value = _uiState.value.copy(
                    elapsedMs = elapsed,
                    readSpeed = if (speed < 0) 0 else speed,
                    tags = snapshot,
                    tagCount = snapshot.size
                )
            }
        }
    }

    fun stopScan() {
        if (!_uiState.value.isScanning) return
        scanJob?.cancel()
        timerJob?.cancel()
        scanJob = null
        timerJob = null

        viewModelScope.launch {
            repository.stopScan()
        }

        // Final snapshot
        val snapshot = synchronized(tagMapLock) { tagMap.values.toList() }
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            tags = snapshot,
            tagCount = snapshot.size
        )
    }

    fun clear() {
        stopScan()
        synchronized(tagMapLock) { tagMap.clear() }
        tagFilterMap.clear()
        totalTagEvents = 0
        lastRefreshCount = 0
        repository.clearScannedEpcs()
        _uiState.value = _uiState.value.copy(
            tags = emptyList(),
            tagCount = 0,
            readSpeed = 0,
            elapsedMs = 0L
        )
    }

    private fun onTagReceived(entry: TagEntry) {
        val filterTime = repository.readerSettings.value.filterTime.toLong()
        val currentTime = System.currentTimeMillis()

        if (filterTime > 0) {
            val lastSeen = tagFilterMap[entry.epc]
            if (lastSeen != null && (currentTime - lastSeen) < filterTime) return
        }
        tagFilterMap[entry.epc] = currentTime

        synchronized(tagMapLock) {
            val key = entry.epc + entry.tid
            val existing = tagMap[key]
            if (existing != null) {
                existing.readCount++
            } else {
                tagMap[key] = entry.copy(readCount = 1)
            }
        }

        totalTagEvents++

        if (_uiState.value.beepEnabled) {
            try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50) } catch (_: Exception) {}
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        clear()
        _uiState.value = _uiState.value.copy(displayMode = mode)
    }

    fun toggleBeep() {
        _uiState.value = _uiState.value.copy(beepEnabled = !_uiState.value.beepEnabled)
    }

    fun setReadMode(mode: Int) {
        if (_uiState.value.isScanning) return
        _uiState.value = _uiState.value.copy(readMode = mode)
    }

    fun saveToExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "Session_Data_$timestamp.csv"
                val mode = _uiState.value.displayMode
                val snapshot = synchronized(tagMapLock) { tagMap.values.toList() }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { os ->
                            BufferedWriter(OutputStreamWriter(os)).use { writer ->
                                writer.write("EPC,Count\n")
                                snapshot.forEach { tag ->
                                    val value = displayValue(tag, mode)
                                    writer.write("$value,${tag.readCount}\n")
                                }
                            }
                        }
                        _uiState.value = _uiState.value.copy(message = "Saved to Downloads: $fileName", saveFileName = fileName)
                    }
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(dir, fileName)
                    FileWriter(file).use { fw ->
                        BufferedWriter(fw).use { writer ->
                            writer.write("EPC,Count\n")
                            snapshot.forEach { tag ->
                                val value = displayValue(tag, mode)
                                writer.write("$value,${tag.readCount}\n")
                            }
                        }
                    }
                    _uiState.value = _uiState.value.copy(message = "Saved to Downloads: $fileName", saveFileName = fileName)
                }
            } catch (e: Exception) {
                Log.e("ScanVM", "Save failed", e)
                _uiState.value = _uiState.value.copy(message = "Save failed: ${e.message}")
            }
        }
    }

    fun displayValue(tag: TagEntry, mode: DisplayMode = _uiState.value.displayMode): String = when (mode) {
        DisplayMode.HEX -> tag.epc
        DisplayMode.ASCII -> hexToAscii(tag.epc)
    }

    private fun hexToAscii(hex: String): String {
        val clean = hex.replace(Regex("[^0-9A-Fa-f]"), "").let {
            if (it.length % 2 != 0) it.dropLast(1) else it
        }
        return try {
            val sb = StringBuilder()
            for (i in clean.indices step 2) {
                val code = clean.substring(i, i + 2).toInt(16)
                sb.append(if (code in 32..126) code.toChar() else '.')
            }
            sb.toString()
        } catch (_: Exception) { "Error" }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, saveFileName = null)
    }

    override fun onCleared() {
        super.onCleared()
        try { toneGenerator?.release() } catch (_: Exception) {}
    }
}
