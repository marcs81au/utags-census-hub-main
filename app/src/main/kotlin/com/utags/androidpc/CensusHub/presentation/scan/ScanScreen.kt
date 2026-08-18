package com.utags.androidpc.CensusHub.presentation.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.utags.androidpc.CensusHub.R
import com.utags.androidpc.CensusHub.domain.model.TagEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWrite: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showViewOptions by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Pulse animation for scan button
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanPulse"
    )

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tv_Cenus_Tag_Reader), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (!state.isScanning) onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.btn_UHFMenu_settings)) },
                            onClick = { showMenu = false; onNavigateToSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.btn_UHFMenu_Write)) },
                            onClick = { showMenu = false; onNavigateToWrite() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Read type selector ──────────────────────────────────
            val readTypes = arrayOf("EPC", "TID", "UserData")
            var readTypeExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Type:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ExposedDropdownMenuBox(
                    expanded = readTypeExpanded,
                    onExpandedChange = { if (!state.isScanning) readTypeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = readTypes.getOrElse(state.readMode) { "EPC" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isScanning,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = readTypeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = readTypeExpanded,
                        onDismissRequest = { readTypeExpanded = false }
                    ) {
                        readTypes.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.setReadMode(index); readTypeExpanded = false }
                            )
                        }
                    }
                }
            }

            // ── Stats bar ───────────────────────────────────────────
            val elapsed = state.elapsedMs
            val secs = elapsed / 1000
            val ms = elapsed % 1000
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatTile(label = "TIME", value = "${secs}.${String.format("%03d", ms)}s")
                StatDivider()
                StatTile(label = "SPEED", value = "${state.readSpeed} T/S")
                StatDivider()
                StatTile(label = "TOTAL", value = state.tagCount.toString(), highlight = true)
            }

            // ── Table header ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Tag Data",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "Count",
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // ── Tag list ────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (state.tags.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (state.isScanning) "Scanning…" else "No tags scanned yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        itemsIndexed(state.tags, key = { _, tag -> tag.epc + tag.tid }) { index, tag ->
                            TagListItem(
                                tag = tag,
                                displayValue = viewModel.displayValue(tag),
                                isEven = index % 2 == 0
                            )
                        }
                    }
                }
            }

            // ── Bottom controls ─────────────────────────────────────
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary scan/stop button — full width, taller
                    val scanContainerColor = if (state.isScanning)
                        MaterialTheme.colorScheme.error.copy(alpha = scanPulse)
                    else
                        MaterialTheme.colorScheme.primary
                    Button(
                        onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scanContainerColor)
                    ) {
                        if (state.isScanning) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (state.isScanning) stringResource(R.string.btn_read_stop)
                            else stringResource(R.string.btn_read),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clear() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.Clear_Tags))
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveToExcel() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.btn_save))
                        }
                        OutlinedButton(
                            onClick = { showViewOptions = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.btn_view))
                        }
                    }
                }
            }
        }
    }

    if (showViewOptions) {
        ViewOptionsSheet(
            currentMode = state.displayMode,
            beepEnabled = state.beepEnabled,
            onHex = { viewModel.setDisplayMode(DisplayMode.HEX); showViewOptions = false },
            onAscii = { viewModel.setDisplayMode(DisplayMode.ASCII); showViewOptions = false },
            onToggleBeep = { viewModel.toggleBeep(); showViewOptions = false },
            onDismiss = { showViewOptions = false }
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
    )
}

@Composable
private fun TagListItem(tag: TagEntry, displayValue: String, isEven: Boolean) {
    val bg = if (isEven) MaterialTheme.colorScheme.surface
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            displayValue,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1
        )
        Text(
            tag.readCount.toString(),
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewOptionsSheet(
    currentMode: DisplayMode,
    beepEnabled: Boolean,
    onHex: () -> Unit,
    onAscii: () -> Unit,
    onToggleBeep: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "View Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Hexadecimal") },
                trailingContent = {
                    if (currentMode == DisplayMode.HEX)
                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                },
                modifier = Modifier.clickable(onClick = onHex)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("ASCII") },
                trailingContent = {
                    if (currentMode == DisplayMode.ASCII)
                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                },
                modifier = Modifier.clickable(onClick = onAscii)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(if (beepEnabled) "Turn off beeper" else "Turn on beeper") },
                modifier = Modifier.clickable(onClick = onToggleBeep)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
