package com.utags.androidpc.CensusHub.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.utags.androidpc.CensusHub.R
import com.utags.androidpc.CensusHub.presentation.connect.stringArrayResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appVersion: String,
    onBack: () -> Unit,
    onAntennaNoUpdated: (Int) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.swrResult) {
        state.swrResult?.let {
            snackbarHostState.showSnackbar("SWR: $it")
            viewModel.clearSwrResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.btn_UHFMenu_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        onAntennaNoUpdated(viewModel.getComputedAntennaNo())
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Version info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("APP: $appVersion", style = MaterialTheme.typography.bodySmall)
                    Text("Firmware: ${state.firmwareVersion}", style = MaterialTheme.typography.bodySmall)
                    Text("Serial: ${state.serialNumber}", style = MaterialTheme.typography.bodySmall)
                }
            }

            // ─── Antenna Power Settings ───────────────────────────
            SectionCard(title = "Antenna Power") {
                val powerRange = (state.minPower..state.maxPower).toList()
                    .ifEmpty { (0..33).toList() }
                val powerValues = powerRange.map { it.toString() }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.antennaSettings.take(state.antennaCount).forEach { ant ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = ant.enabled,
                                onCheckedChange = { viewModel.onAntennaEnabledChanged(ant.number, it) }
                            )
                            Text("Ant ${ant.number}", modifier = Modifier.width(48.dp))

                            var powerExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = powerExpanded,
                                onExpandedChange = { powerExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = ant.power.toString(),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = powerExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = powerExpanded,
                                    onDismissRequest = { powerExpanded = false }
                                ) {
                                    powerValues.forEach { pv ->
                                        DropdownMenuItem(
                                            text = { Text(pv) },
                                            onClick = {
                                                viewModel.onAntennaPowerChanged(ant.number, pv.toInt())
                                                powerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::getPower, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.btn_Get))
                        }
                        Button(onClick = viewModel::setPower, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.btn_Set))
                        }
                    }
                }
            }

            // ─── Scan Antenna Selection ───────────────────────────
            SectionCard(title = stringResource(R.string.tv_Configration_ReadAnt)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        (1..state.antennaCount).forEach { ant ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = state.selectedScanAntennas.contains(ant),
                                    onCheckedChange = { viewModel.onScanAntennaToggled(ant, it) }
                                )
                                Text("ANT$ant", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Button(onClick = viewModel::setScanAntennas, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.btn_Set))
                    }
                }
            }

            // ─── Frequency ────────────────────────────────────────
            SectionCard(title = stringResource(R.string.tv_Configration_Frequency)) {
                val frequencies = stringArrayResource(R.array.Array_Frequency)
                var freqExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = it }) {
                    OutlinedTextField(
                        value = frequencies.getOrElse(state.frequencyIndex) { "" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        frequencies.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onFrequencySelected(idx); freqExpanded = false }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = viewModel::setFrequency) { Text(stringResource(R.string.btn_Set)) }
                }
            }

            // ─── Baseband ─────────────────────────────────────────
            SectionCard(title = stringResource(R.string.tv_Configration_BaseSpeed)) {
                val baseSpeeds = stringArrayResource(R.array.Array_BaseSpeedType)
                val qValues = stringArrayResource(R.array.Array_QValue)
                var bsExpanded by remember { mutableStateOf(false) }
                var qExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(expanded = bsExpanded, onExpandedChange = { bsExpanded = it }) {
                    OutlinedTextField(
                        value = baseSpeeds.getOrElse(state.baseSpeedTypeIndex) { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Base Speed") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bsExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = bsExpanded, onDismissRequest = { bsExpanded = false }) {
                        baseSpeeds.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onBaseSpeedTypeSelected(idx); bsExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = qExpanded, onExpandedChange = { qExpanded = it }) {
                    OutlinedTextField(
                        value = qValues.getOrElse(state.baseSpeedQIndex) { "0" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.tv_Configration_QValue)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = qExpanded, onDismissRequest = { qExpanded = false }) {
                        qValues.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onBaseSpeedQSelected(idx); qExpanded = false }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = viewModel::setBaseband) { Text(stringResource(R.string.btn_Set)) }
                }
            }

            // ─── Device Beep ──────────────────────────────────────
            SectionCard(title = stringResource(R.string.tv_device_name)) {
                val beepOptions = stringArrayResource(R.array.Array_on)
                var beepExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = beepExpanded, onExpandedChange = { beepExpanded = it }) {
                    OutlinedTextField(
                        value = if (state.beepEnabled) "On" else "Off",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = beepExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = beepExpanded, onDismissRequest = { beepExpanded = false }) {
                        beepOptions.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onBeepToggled(idx == 0); beepExpanded = false }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = viewModel::setBeep) { Text(stringResource(R.string.btn_Set)) }
                }
            }

            // ─── Tag Type ─────────────────────────────────────────
            SectionCard(title = stringResource(R.string.tv_Configration_TagType)) {
                val tagTypes = stringArrayResource(R.array.Array_TagType)
                var ttExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = ttExpanded, onExpandedChange = { ttExpanded = it }) {
                    OutlinedTextField(
                        value = tagTypes.getOrElse(state.tagTypeIndex) { "6C" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ttExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = ttExpanded, onDismissRequest = { ttExpanded = false }) {
                        tagTypes.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onTagTypeSelected(idx); ttExpanded = false }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = viewModel::setTagType) { Text(stringResource(R.string.btn_Set)) }
                }
            }

            // ─── Filter Time ──────────────────────────────────────
            SectionCard(title = "Filter Time") {
                OutlinedTextField(
                    value = state.filterTime,
                    onValueChange = viewModel::onFilterTimeChanged,
                    label = { Text("Repeat Time Filter (x10ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.rssiFilter,
                    onValueChange = viewModel::onRssiFilterChanged,
                    label = { Text("RSSI Filter") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.loadAll() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.btn_Get))
                    }
                    Button(onClick = viewModel::setFilterTime, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.btn_Set))
                    }
                }
            }

            // ─── Standing Wave Ratio ──────────────────────────────
            SectionCard(title = "Standing Wave Ratio Test") {
                val antennaCount = state.antennaCount.coerceAtLeast(1)
                val antOptions = (1..antennaCount).map { it.toString() }
                var swrAntExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(expanded = swrAntExpanded, onExpandedChange = { swrAntExpanded = it }) {
                    OutlinedTextField(
                        value = antOptions.getOrElse(state.swrAntennaIndex) { "1" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Antenna") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = swrAntExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = swrAntExpanded, onDismissRequest = { swrAntExpanded = false }) {
                        antOptions.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onSwrAntennaSelected(idx); swrAntExpanded = false }
                            )
                        }
                    }
                }
                Button(onClick = viewModel::testStandingWave, modifier = Modifier.fillMaxWidth()) {
                    Text("Test")
                }
            }

            // ─── Factory Reset ────────────────────────────────────
            SectionCard(title = "Device") {
                Button(
                    onClick = viewModel::restoreFactory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore Factory Defaults")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            content()
        }
    }
}
