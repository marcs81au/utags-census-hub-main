package com.utags.androidpc.CensusHub.presentation.write

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    antennaNo: Int,
    onBack: () -> Unit,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.btn_UHFMenu_Write), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // EPC selector from previously scanned tags
            if (state.scannedEpcs.isNotEmpty()) {
                var epcExpanded by remember { mutableStateOf(false) }
                Text(stringResource(R.string.tv_Write_MatchTitle), style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = epcExpanded,
                    onExpandedChange = { epcExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.scannedEpcs.getOrElse(state.selectedEpcIndex) { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Scanned EPC") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = epcExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = epcExpanded,
                        onDismissRequest = { epcExpanded = false }
                    ) {
                        state.scannedEpcs.forEachIndexed { index, epc ->
                            DropdownMenuItem(
                                text = { Text(epc) },
                                onClick = {
                                    viewModel.onEpcSelected(index)
                                    epcExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // TID match field
            OutlinedTextField(
                value = state.matchTid,
                onValueChange = viewModel::onMatchTidChanged,
                label = { Text(stringResource(R.string.tv_Write_MatchTID)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            // Read match tag button
            Button(
                onClick = { viewModel.readMatchTag(antennaNo) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isReading && !state.isWriting
            ) {
                if (state.isReading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.btn_Write_ReadMatch))
            }

            HorizontalDivider()

            // Write type selector
            var writeTypeExpanded by remember { mutableStateOf(false) }
            val writeTypeLabels = arrayOf("EPC", "UserData", "Access Password")
            val currentWriteTypeLabel = when (state.writeType) {
                WriteType.EPC -> "EPC"
                WriteType.USER_DATA -> "UserData"
                WriteType.ACCESS_PASSWORD -> "Access Password"
            }

            ExposedDropdownMenuBox(
                expanded = writeTypeExpanded,
                onExpandedChange = { writeTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentWriteTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.tv_Write_WriteType)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = writeTypeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = writeTypeExpanded,
                    onDismissRequest = { writeTypeExpanded = false }
                ) {
                    writeTypeLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.onWriteTypeSelected(WriteType.values()[index])
                                writeTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Access password field (only shown for Access Password write type)
            if (state.writeType == WriteType.ACCESS_PASSWORD) {
                OutlinedTextField(
                    value = state.accessPassword,
                    onValueChange = viewModel::onAccessPasswordChanged,
                    label = { Text(stringResource(R.string.tv_Write_AccessPassword)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    supportingText = { Text("Must be 8 hex characters") }
                )
            }

            // Write data field
            OutlinedTextField(
                value = state.writeData,
                onValueChange = viewModel::onWriteDataChanged,
                label = { Text(stringResource(R.string.tv_Write_WriteData)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                supportingText = { Text("Hex characters only (0-9, A-F)") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::clearWriteData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = { viewModel.writeData(antennaNo) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isWriting && !state.isReading
                ) {
                    if (state.isWriting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.btn_Write_WriteData))
                }
            }
        }
    }
}
