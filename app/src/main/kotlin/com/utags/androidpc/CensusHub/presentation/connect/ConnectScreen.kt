package com.utags.androidpc.CensusHub.presentation.connect

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.utags.androidpc.CensusHub.R
import com.utags.androidpc.CensusHub.domain.model.ConnectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    appVersion: String,
    onConnected: () -> Unit,
    onExit: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setAppVersion(appVersion)
        viewModel.loadBluetoothDevices()
    }

    LaunchedEffect(state.message) {
        if (state.message != null) viewModel.clearMessage()
    }

    state.errorMessage?.let { err ->
        LaunchedEffect(err) {
            // shown inline in UI
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tv_MainMenu_Title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status + version card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            if (state.isConnected) "Connected" else "Not Connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    if (state.isConnected) {
                        Text("ConnID: ${state.connId}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Text("APP: $appVersion   SDK: ${state.sdkVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            // Connection type selector
            val connectionTypes = stringArrayResource(R.array.Array_Connect_Type)
            var typeExpanded by remember { mutableStateOf(false) }

            Text("Connection Type", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = connectionTypes.getOrElse(state.selectedTypeIndex) { "" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    connectionTypes.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.onTypeSelected(index)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Parameter input — depends on selected type
            val selectedType = ConnectionType.fromIndex(state.selectedTypeIndex)

            when (selectedType) {
                ConnectionType.BLUETOOTH -> {
                    BluetoothParamSection(
                        devices = state.bluetoothDevices,
                        selectedParam = state.connectionParam,
                        onParamChanged = viewModel::onParamChanged,
                        onSearch = {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        },
                        onRefresh = { viewModel.loadBluetoothDevices() }
                    )
                }
                ConnectionType.USB_SERIAL, ConnectionType.USB_HID, ConnectionType.USB_RS485 -> {
                    val devices = when (selectedType) {
                        ConnectionType.USB_HID -> state.usbHidDevices
                        else -> state.usbSerialDevices
                    }
                    UsbParamSection(
                        devices = devices,
                        selectedParam = state.connectionParam,
                        onParamChanged = viewModel::onParamChanged
                    )
                }
                else -> {
                    Text("Connection Parameter", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = state.connectionParam,
                        onValueChange = viewModel::onParamChanged,
                        placeholder = { Text(defaultParam(selectedType)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Error message
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Connect / Enter button
            if (state.isConnected) {
                Button(
                    onClick = onConnected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter UTAGS Census Hub")
                }
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect")
                }
            } else {
                Button(
                    onClick = { viewModel.connect(onConnected) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isConnecting
                ) {
                    if (state.isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.str_please_waitting))
                    } else {
                        Text(stringResource(R.string.str_Login))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothParamSection(
    devices: List<String>,
    selectedParam: String,
    onParamChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text("Bluetooth Device", style = MaterialTheme.typography.labelLarge)

    if (devices.isEmpty()) {
        Text("No paired devices found. Pair a device first.", style = MaterialTheme.typography.bodySmall)
    } else {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedParam.ifEmpty { devices.firstOrNull() ?: "" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device) },
                        onClick = {
                            onParamChanged(device)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Refresh") }
        OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f)) { Text("BT Settings") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsbParamSection(
    devices: List<String>,
    selectedParam: String,
    onParamChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text("USB Device", style = MaterialTheme.typography.labelLarge)

    if (devices.isEmpty()) {
        Text("No USB devices found.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = selectedParam,
            onValueChange = onParamChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    } else {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedParam.ifEmpty { devices.firstOrNull() ?: "" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device) },
                        onClick = {
                            onParamChanged(device)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun defaultParam(type: ConnectionType): String = when (type) {
    ConnectionType.RS232 -> "/dev/ttySAC1:115200"
    ConnectionType.TCP -> "192.168.1.116:9090"
    ConnectionType.RS485 -> "1:/dev/ttySAC1:115200"
    else -> ""
}

@Composable
fun stringArrayResource(id: Int): Array<String> {
    return LocalContext.current.resources.getStringArray(id)
}
