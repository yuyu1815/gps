package com.example.myapplication.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.domain.model.FusionMethod
import com.example.myapplication.presentation.viewmodel.SettingsViewModel
import com.example.myapplication.domain.model.InitialFixMode
import com.example.myapplication.presentation.viewmodel.SettingsViewModel.ExportImportStatus
import com.example.myapplication.ui.component.LanguageSelector
import org.koin.androidx.compose.koinViewModel

/**
 * Hyper-style settings screen with modern, clean design.
 * Features:
 * - Card-based layout for better organization
 * - Smooth animations and transitions
 * - Intuitive controls and feedback
 * - Minimal, distraction-free interface
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

    val exportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
            uriToFile(context, uri)?.let { file ->
                viewModel.exportSettings(file)
            }
        }
    }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            uriToFile(context, uri)?.let { file ->
                viewModel.importSettings(file)
            }
        }
    }

    LaunchedEffect(exportImportStatus) {
        when (val status = exportImportStatus) {
            is ExportImportStatus.Success -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.resetExportImportStatus()
            }
            is ExportImportStatus.Error -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.resetExportImportStatus()
            }
            else -> {}
        }
    }

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
            HyperTopBar(
                title = stringResource(id = com.example.myapplication.R.string.settings_title),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Settings Section
            HyperSettingsSection(
                title = stringResource(id = com.example.myapplication.R.string.settings_language),
                icon = Icons.Default.Language
            ) {
                LanguageSelector(
                    onLanguageChanged = { languageCode ->
                        viewModel.updateAppLanguage(languageCode)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // PDR Settings Section
            HyperSettingsSection(
                title = stringResource(id = com.example.myapplication.R.string.settings_pdr),
                icon = Icons.Default.DirectionsWalk
            ) {
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_step_length),
                    value = uiState?.stepLengthCm ?: 70f,
                    onValueChange = { viewModel.updateStepLength(it) },
                    valueRange = 50f..100f,
                    valueText = "%.1f cm".format(uiState?.stepLengthCm ?: 70f),
                    description = stringResource(id = com.example.myapplication.R.string.settings_step_length_desc)
                )
            }

            // Sensor Fusion Settings Section
            HyperSettingsSection(
                title = stringResource(id = com.example.myapplication.R.string.settings_sensor_fusion),
                icon = Icons.Default.Sensors
            ) {
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_fusion_weight),
                    value = uiState?.sensorFusionWeight ?: 0.5f,
                    onValueChange = { viewModel.updateSensorFusionWeight(it) },
                    valueRange = 0f..1f,
                    valueText = "%.2f".format(uiState?.sensorFusionWeight ?: 0.5f),
                    description = stringResource(id = com.example.myapplication.R.string.settings_fusion_weight_desc)
                )

                HyperDropdownSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_fusion_method),
                    selectedValue = uiState?.fusionMethod?.name ?: "EKF",
                    options = FusionMethod.values().map { it.name },
                    onValueChange = { viewModel.updateFusionMethod(FusionMethod.valueOf(it)) },
                    description = stringResource(id = com.example.myapplication.R.string.settings_fusion_method_desc)
                )

                // Initial Fix Mode selector
                HyperDropdownSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_initial_fix_mode),
                    selectedValue = (uiState?.initialFixMode ?: InitialFixMode.AUTO).name,
                    options = InitialFixMode.values().map { it.name },
                    onValueChange = { viewModel.updateInitialFixMode(InitialFixMode.valueOf(it)) },
                    description = stringResource(id = com.example.myapplication.R.string.settings_initial_fix_mode_desc)
                )
                
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_sensor_monitoring_delay),
                    value = (uiState?.sensorMonitoringDelay ?: 100).toFloat(),
                    onValueChange = { viewModel.updateSensorMonitoringDelay(it.toInt()) },
                    valueRange = 50f..1000f,
                    valueText = "${uiState?.sensorMonitoringDelay ?: 100} ms",
                    description = stringResource(id = com.example.myapplication.R.string.settings_sensor_monitoring_delay_desc)
                )
                
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_accelerometer_delay),
                    value = (uiState?.accelerometerDelay ?: 50).toFloat(),
                    onValueChange = { viewModel.updateAccelerometerDelay(it.toInt()) },
                    valueRange = 20f..500f,
                    valueText = "${uiState?.accelerometerDelay ?: 50} ms",
                    description = stringResource(id = com.example.myapplication.R.string.settings_accelerometer_delay_desc)
                )
                
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_gyroscope_delay),
                    value = (uiState?.gyroscopeDelay ?: 100).toFloat(),
                    onValueChange = { viewModel.updateGyroscopeDelay(it.toInt()) },
                    valueRange = 20f..500f,
                    valueText = "${uiState?.gyroscopeDelay ?: 100} ms",
                    description = stringResource(id = com.example.myapplication.R.string.settings_gyroscope_delay_desc)
                )
                
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_magnetometer_delay),
                    value = (uiState?.magnetometerDelay ?: 200).toFloat(),
                    onValueChange = { viewModel.updateMagnetometerDelay(it.toInt()) },
                    valueRange = 50f..1000f,
                    valueText = "${uiState?.magnetometerDelay ?: 200} ms",
                    description = stringResource(id = com.example.myapplication.R.string.settings_magnetometer_delay_desc)
                )
                
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_linear_acceleration_delay),
                    value = (uiState?.linearAccelerationDelay ?: 100).toFloat(),
                    onValueChange = { viewModel.updateLinearAccelerationDelay(it.toInt()) },
                    valueRange = 20f..500f,
                    valueText = "${uiState?.linearAccelerationDelay ?: 100} ms",
                    description = stringResource(id = com.example.myapplication.R.string.settings_linear_acceleration_delay_desc)
                )
                
                HyperSliderSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_gravity_delay),
                    value = (uiState?.gravityDelay ?: 500).toFloat(),
                    onValueChange = { viewModel.updateGravityDelay(it.toInt()) },
                    valueRange = 100f..2000f,
                    valueText = "${uiState?.gravityDelay ?: 500} ms",
                    description = stringResource(id = com.example.myapplication.R.string.settings_gravity_delay_desc)
                )
            }

            // Performance Settings Section
            HyperSettingsSection(
                title = stringResource(id = com.example.myapplication.R.string.settings_performance),
                icon = Icons.Default.Speed
            ) {
                HyperSwitchSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_debug_mode),
                    checked = uiState?.debugModeEnabled ?: false,
                    onCheckedChange = { viewModel.updateDebugMode(it) },
                    description = stringResource(id = com.example.myapplication.R.string.settings_debug_mode_desc)
                )

                HyperSwitchSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_data_logging),
                    checked = uiState?.dataLoggingEnabled ?: false,
                    onCheckedChange = { viewModel.updateDataLogging(it) },
                    description = stringResource(id = com.example.myapplication.R.string.settings_data_logging_desc)
                )

                HyperSwitchSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_low_power_mode),
                    checked = uiState?.lowPowerModeEnabled ?: false,
                    onCheckedChange = { viewModel.updateLowPowerMode(it) },
                    description = stringResource(id = com.example.myapplication.R.string.settings_low_power_mode_desc)
                )
            }

            // Data Management Section
            HyperSettingsSection(
                title = stringResource(id = com.example.myapplication.R.string.settings_data_management),
                icon = Icons.Default.Storage
            ) {
                HyperButtonSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_export_settings),
                    onClick = { exportFilePicker.launch("indoor_positioning_settings.json") },
                    description = stringResource(id = com.example.myapplication.R.string.settings_export_settings_desc)
                )

                HyperButtonSetting(
                    title = stringResource(id = com.example.myapplication.R.string.settings_import_settings),
                    onClick = { importFilePicker.launch(arrayOf("application/json")) },
                    description = stringResource(id = com.example.myapplication.R.string.settings_import_settings_desc)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HyperTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun HyperSettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section content
            content()
        }
    }
}

@Composable
private fun HyperSliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HyperDropdownSetting(
    title: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    description: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HyperSwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun HyperButtonSetting(
    title: String,
    onClick: () -> Unit,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title)
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun uriToFile(context: android.content.Context, uri: android.net.Uri): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = java.io.File(context.cacheDir, "imported_settings.json")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        null
    }
}

