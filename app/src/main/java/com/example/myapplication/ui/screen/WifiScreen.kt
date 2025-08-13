package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.presentation.viewmodel.WifiViewModel
import com.example.myapplication.wifi.WifiScanner
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Hyper-style Wi-Fi screen with modern, clean design.
 * Features:
 * - Real-time Wi-Fi scanning visualization
 * - Signal strength indicators
 * - Network selection interface
 * - Clean, minimal design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScreen(
    onNavigateBack: () -> Unit,
    viewModel: WifiViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Use remember to create a KoinComponent for injection
    val wifiScanner = remember { object : KoinComponent {
        val scanner: WifiScanner by inject()
    }.scanner }
    
    val scanResults by wifiScanner.scanResults.collectAsState()
    val isScanning by wifiScanner.isScanning.collectAsState()

    Scaffold(
        topBar = {
            HyperTopBar(
                title = stringResource(id = R.string.wifi_networks),
                onNavigateBack = onNavigateBack,
                onRefresh = { wifiScanner.startScan() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Scanning status
            HyperScanningStatus(
                isScanning = isScanning,
                networksFound = scanResults.size,
                modifier = Modifier.padding(16.dp)
            )

            // Wi-Fi networks list
            if (scanResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scanResults) { scanResult ->
                        WifiNetworkCard(
                            scanResult = scanResult,
                            isSelected = uiState?.selectedBssids?.contains(scanResult.bssid) ?: false,
                            onToggleSelection = { viewModel.toggleSelection(scanResult.bssid) }
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.wifi_networks),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScanning) 
                                stringResource(id = R.string.wifi_scanning) 
                            else 
                                stringResource(id = R.string.wifi_network_scanning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HyperTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
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
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.wifi_refresh)
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
private fun HyperScanningStatus(
    isScanning: Boolean,
    networksFound: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = if (isScanning) stringResource(id = R.string.wifi_scanning) else stringResource(id = R.string.wifi_networks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.wifi_networks_found, networksFound),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Signal strength indicator
            if (!isScanning && networksFound > 0) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalWifi4Bar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun WifiNetworkCard(
    scanResult: com.example.myapplication.wifi.ScanResult,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scanResult.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scanResult.bssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${scanResult.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Signal strength indicator
                SignalStrengthIndicator(rssi = scanResult.rssi)
                
                // Selection checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
            }
        }
    }
}

@Composable
fun SignalStrengthIndicator(rssi: Int) {
    val signalStrength = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        rssi >= -80 -> 1
        else -> 0
    }
    
    val icon = when (signalStrength) {
        4 -> Icons.Default.SignalWifi4Bar
        3 -> Icons.Default.SignalWifi4Bar
        2 -> Icons.Default.SignalWifi4Bar
        1 -> Icons.Default.SignalWifi4Bar
        else -> Icons.Default.SignalWifi0Bar
    }
    
    val color = when (signalStrength) {
        4 -> MaterialTheme.colorScheme.primary
        3 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.tertiary
        1 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.error
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Signal strength: $signalStrength bars",
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}


