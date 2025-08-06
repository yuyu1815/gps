package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.presentation.viewmodel.LogManagementViewModel
import com.example.myapplication.presentation.viewmodel.LogReplayViewModel
import com.example.myapplication.service.LogFileManager

/**
 * Screen for managing log files.
 *
 * @param onNavigateBack Callback for navigating back
 * @param navController Navigation controller for navigating to other screens
 * @param viewModel The view model for log management
 * @param replayViewModel The view model for log replay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogManagementScreen(
    onNavigateBack: () -> Unit,
    navController: NavController? = null,
    viewModel: LogManagementViewModel = viewModel(),
    replayViewModel: LogReplayViewModel = viewModel()
) {
    // Variable to track if we should navigate to replay screen
    var navigateToReplay by remember { mutableStateOf(false) }
    val logFiles = viewModel.logFiles
    val totalSize = viewModel.totalSize
    var selectedLogType by remember { mutableStateOf(LogFileManager.LogType.ALL) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var selectedLogFile by remember { mutableStateOf<LogFileManager.LogFileInfo?>(null) }
    
    LaunchedEffect(selectedLogType) {
        viewModel.loadLogFiles(selectedLogType)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLogFiles(selectedLogType) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete All")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Log type selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedLogType == LogFileManager.LogType.ALL,
                    onClick = { selectedLogType = LogFileManager.LogType.ALL },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("All")
                }
                SegmentedButton(
                    selected = selectedLogType == LogFileManager.LogType.SENSOR,
                    onClick = { selectedLogType = LogFileManager.LogType.SENSOR },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Sensor")
                }
                SegmentedButton(
                    selected = selectedLogType == LogFileManager.LogType.BLE,
                    onClick = { selectedLogType = LogFileManager.LogType.BLE },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("BLE")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Summary info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Log Files Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total files: ${logFiles.size}")
                    Text("Total size: $totalSize")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Log files list
            if (logFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No log files found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(logFiles) { logFile ->
                        LogFileItem(
                            logFile = logFile,
                            onDeleteClick = { selectedLogFile = logFile },
                            onReplayClick = {
                                // Set the selected log file in the replay view model
                                replayViewModel.selectLogFile(logFile)
                                
                                // Navigate to replay screen if NavController is available
                                if (navController != null) {
                                    navController.navigate("log_replay_screen")
                                } else {
                                    // Otherwise set flag to show replay UI
                                    navigateToReplay = true
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
    
    // Delete all confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Log Files") },
            text = { Text("Are you sure you want to delete all ${selectedLogType.name.lowercase()} log files? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllLogFiles(selectedLogType)
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete single file confirmation dialog
    selectedLogFile?.let { logFile ->
        AlertDialog(
            onDismissRequest = { selectedLogFile = null },
            title = { Text("Delete Log File") },
            text = { Text("Are you sure you want to delete this log file?\n\n${logFile.file.name}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLogFile(logFile.file)
                        selectedLogFile = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLogFile = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Composable for displaying a single log file item.
 *
 * @param logFile The log file info to display
 * @param onDeleteClick Callback for when the delete button is clicked
 * @param onReplayClick Callback for when the replay button is clicked
 */
@Composable
fun LogFileItem(
    logFile: LogFileManager.LogFileInfo,
    onDeleteClick: () -> Unit,
    onReplayClick: () -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showOptions = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Log type indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    when (logFile.type) {
                        LogFileManager.LogType.SENSOR -> Color(0xFF4CAF50) // Green
                        LogFileManager.LogType.BLE -> Color(0xFF2196F3) // Blue
                        else -> Color.Gray
                    },
                    shape = MaterialTheme.shapes.small
                )
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Log file info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = logFile.file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = logFile.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = logFile.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Replay button
        IconButton(onClick = onReplayClick) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Replay",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Delete button
        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        // Options menu
        Box {
            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Replay") },
                    onClick = {
                        onReplayClick()
                        showOptions = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDeleteClick()
                        showOptions = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}