package com.example.myapplication.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase.FusionMethod
import com.example.myapplication.presentation.viewmodel.SettingsViewModel
import com.example.myapplication.presentation.viewmodel.SettingsViewModel.ExportImportStatus
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * Settings screen for the application.
 * Allows users to configure various parameters for the indoor positioning system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportImportStatus by viewModel.exportImportStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // File pickers
    val exportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val file = uriToFile(context, uri)
            viewModel.exportSettings(file)
        }
    }
    
    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val file = uriToFile(context, uri)
            viewModel.importSettings(file)
        }
    }
    
    // Handle export/import status
    LaunchedEffect(exportImportStatus) {
        when (exportImportStatus) {
            is ExportImportStatus.Success -> {
                snackbarHostState.showSnackbar((exportImportStatus as ExportImportStatus.Success).message)
                viewModel.resetExportImportStatus()
            }
            is ExportImportStatus.Error -> {
                snackbarHostState.showSnackbar((exportImportStatus as ExportImportStatus.Error).message)
                viewModel.resetExportImportStatus()
            }
            else -> {}
        }
    }
    
    // Show progress dialog when operation is in progress
    if (exportImportStatus is ExportImportStatus.InProgress) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text((exportImportStatus as ExportImportStatus.InProgress).message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BLE Settings Section
            SettingsSection(title = "BLE Settings") {
                // BLE Scan Interval
                var bleScanInterval by remember { mutableLongStateOf(uiState?.bleScanIntervalMs ?: 1000L) }
                SettingSlider(
                    title = "BLE Scan Interval",
                    value = bleScanInterval.toFloat(),
                    valueRange = 500f..5000f,
                    steps = 9,
                    onValueChange = { bleScanInterval = it.toLong() },
                    onValueChangeFinished = { viewModel.updateBleScanInterval(bleScanInterval) },
                    valueText = "$bleScanInterval ms"
                )
                
                // Environmental Factor
                var environmentalFactor by remember { mutableFloatStateOf(uiState?.environmentalFactor ?: 2.0f) }
                SettingSlider(
                    title = "Environmental Factor",
                    value = environmentalFactor,
                    valueRange = 1f..4f,
                    steps = 6,
                    onValueChange = { environmentalFactor = it },
                    onValueChangeFinished = { viewModel.updateEnvironmentalFactor(environmentalFactor) },
                    valueText = environmentalFactor.toString()
                )
                
                // Beacon Staleness Timeout
                var beaconStalenessTimeout by remember { mutableLongStateOf(uiState?.beaconStalenessTimeoutMs ?: 5000L) }
                SettingSlider(
                    title = "Beacon Staleness Timeout",
                    value = beaconStalenessTimeout.toFloat(),
                    valueRange = 1000f..10000f,
                    steps = 9,
                    onValueChange = { beaconStalenessTimeout = it.toLong() },
                    onValueChangeFinished = { viewModel.updateBeaconStalenessTimeout(beaconStalenessTimeout) },
                    valueText = "$beaconStalenessTimeout ms"
                )
            }
            
            // PDR Settings Section
            SettingsSection(title = "Pedestrian Dead Reckoning") {
                // Step Length
                var stepLength by remember { mutableFloatStateOf(uiState?.stepLengthCm ?: 75.0f) }
                SettingSlider(
                    title = "Step Length",
                    value = stepLength,
                    valueRange = 50f..100f,
                    steps = 10,
                    onValueChange = { stepLength = it },
                    onValueChangeFinished = { viewModel.updateStepLength(stepLength) },
                    valueText = "$stepLength cm"
                )
            }
            
            // Sensor Fusion Settings Section
            SettingsSection(title = "Sensor Fusion") {
                // Sensor Fusion Weight
                var sensorFusionWeight by remember { mutableFloatStateOf(uiState?.sensorFusionWeight ?: 0.5f) }
                SettingSlider(
                    title = "BLE/PDR Weight",
                    value = sensorFusionWeight,
                    valueRange = 0f..1f,
                    steps = 10,
                    onValueChange = { sensorFusionWeight = it },
                    onValueChangeFinished = { viewModel.updateSensorFusionWeight(sensorFusionWeight) },
                    valueText = "$sensorFusionWeight"
                )
                
                // Fusion Method
                val fusionMethods = FusionMethod.values()
                val currentMethodIndex = fusionMethods.indexOf(uiState?.fusionMethod ?: FusionMethod.WEIGHTED_AVERAGE)
                var selectedMethodIndex by remember { mutableStateOf(currentMethodIndex) }
                
                Text(
                    text = "Fusion Method",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fusionMethods.forEachIndexed { index, method ->
                        Button(
                            onClick = {
                                selectedMethodIndex = index
                                viewModel.updateFusionMethod(method)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = index != selectedMethodIndex
                        ) {
                            Text(method.name.replace("_", " "))
                        }
                    }
                }
            }
            
            // Debug Settings Section
            SettingsSection(title = "Debug Options") {
                // Debug Mode
                SettingSwitch(
                    title = "Debug Mode",
                    checked = uiState?.debugModeEnabled ?: false,
                    onCheckedChange = { viewModel.updateDebugMode(it) }
                )
                
                // Data Logging
                SettingSwitch(
                    title = "Data Logging",
                    checked = uiState?.dataLoggingEnabled ?: false,
                    onCheckedChange = { viewModel.updateDataLogging(it) }
                )
            }
            
            // Configuration Management Section
            SettingsSection(title = "Configuration Management") {
                Text(
                    text = "Export and import application settings",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            exportFilePicker.launch("settings.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Settings")
                    }
                    
                    Button(
                        onClick = { 
                            importFilePicker.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Settings")
                    }
                }
            }
            
            // Reset Button
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}

/**
 * A section in the settings screen with a title and content.
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            content()
        }
    }
}

/**
 * A slider setting with a title and current value display.
 */
@Composable
fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/**
 * A switch setting with a title.
 */
@Composable
fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}

/**
 * Helper function to convert a URI to a File.
 * For export, it creates a new file at the URI location.
 * For import, it creates a temporary file with the content from the URI.
 */
private fun uriToFile(context: Context, uri: Uri): File {
    val contentResolver = context.contentResolver
    val cacheDir = context.cacheDir
    
    // For import: create a temp file with the content from the URI
    if (uri.scheme == "content") {
        val fileName = uri.lastPathSegment ?: "settings.json"
        val tempFile = File(cacheDir, fileName)
        
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        return tempFile
    }
    
    // For export: return a file that points to the URI
    // The actual file will be created by the system when we write to it
    val fileName = uri.lastPathSegment ?: "settings.json"
    return File(cacheDir, fileName).apply {
        // Make sure the file exists and is writable
        if (!exists()) {
            createNewFile()
        }
    }
}