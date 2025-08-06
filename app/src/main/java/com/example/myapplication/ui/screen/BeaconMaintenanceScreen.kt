package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.BeaconHealthStatus
import com.example.myapplication.presentation.viewmodel.BeaconMaintenanceViewModel
import com.example.myapplication.service.BeaconMaintenanceTool.BeaconDiagnosticInfo
import com.example.myapplication.service.BeaconMaintenanceTool.MaintenancePriority
import com.example.myapplication.service.BeaconMaintenanceTool.MaintenanceTask
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for beacon maintenance management.
 * Provides functionality to view beacon health, manage maintenance tasks, and run diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconMaintenanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: BeaconMaintenanceViewModel = koinViewModel()
) {
    val maintenanceTasks by viewModel.maintenanceTasks.collectAsState()
    val diagnosticReport by viewModel.diagnosticReport.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Tasks", "Diagnostics", "Schedule")
    
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beacon Maintenance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { viewModel.runDiagnostics() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Run Diagnostics")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTabIndex) {
                0 -> MaintenanceTasksTab(
                    tasks = maintenanceTasks,
                    onCompleteTask = { taskId, notes -> viewModel.completeMaintenanceTask(taskId, notes) },
                    dateFormatter = dateFormatter
                )
                1 -> DiagnosticsTab(
                    diagnosticReport = diagnosticReport,
                    dateFormatter = dateFormatter
                )
                2 -> MaintenanceScheduleTab(
                    viewModel = viewModel,
                    dateFormatter = dateFormatter
                )
            }
        }
    }
}

@Composable
fun MaintenanceTasksTab(
    tasks: List<MaintenanceTask>,
    onCompleteTask: (String, String) -> Unit,
    dateFormatter: SimpleDateFormat
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No maintenance tasks found.\nRun diagnostics to generate tasks.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                item {
                    Text(
                        text = "Maintenance Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                val incompleteTasks = tasks.filter { it.completedTimestamp == null }
                val completedTasks = tasks.filter { it.completedTimestamp != null }
                
                if (incompleteTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Tasks (${incompleteTasks.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(incompleteTasks.sortedBy { 
                        when (it.priority) {
                            MaintenancePriority.URGENT -> 0
                            MaintenancePriority.HIGH -> 1
                            MaintenancePriority.MEDIUM -> 2
                            MaintenancePriority.LOW -> 3
                        }
                    }) { task ->
                        MaintenanceTaskItem(
                            task = task,
                            onCompleteTask = onCompleteTask,
                            dateFormatter = dateFormatter
                        )
                    }
                }
                
                if (completedTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed Tasks (${completedTasks.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(completedTasks.sortedByDescending { it.completedTimestamp }) { task ->
                        MaintenanceTaskItem(
                            task = task,
                            onCompleteTask = onCompleteTask,
                            dateFormatter = dateFormatter
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun MaintenanceTaskItem(
    task: MaintenanceTask,
    onCompleteTask: (String, String) -> Unit,
    dateFormatter: SimpleDateFormat
) {
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completionNotes by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            when (task.priority) {
                                MaintenancePriority.URGENT -> Color.Red
                                MaintenancePriority.HIGH -> Color(0xFFF57C00) // Orange
                                MaintenancePriority.MEDIUM -> Color(0xFFFFB300) // Amber
                                MaintenancePriority.LOW -> Color(0xFF4CAF50) // Green
                            },
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = task.beaconName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = task.priority.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (task.priority) {
                        MaintenancePriority.URGENT -> Color.Red
                        MaintenancePriority.HIGH -> Color(0xFFF57C00) // Orange
                        MaintenancePriority.MEDIUM -> Color(0xFFFFB300) // Amber
                        MaintenancePriority.LOW -> Color(0xFF4CAF50) // Green
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created: ${dateFormatter.format(Date(task.createdTimestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (task.completedTimestamp != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Completed: ${dateFormatter.format(Date(task.completedTimestamp!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    TextButton(
                        onClick = { showCompleteDialog = true }
                    ) {
                        Text("Complete")
                    }
                }
            }
            
            if (task.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Notes: ${task.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Complete Maintenance Task") },
            text = {
                Column {
                    Text("Add notes about the maintenance performed:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = completionNotes,
                        onValueChange = { completionNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Maintenance notes (optional)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCompleteTask(task.id, completionNotes)
                        showCompleteDialog = false
                    }
                ) {
                    Text("Complete Task")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCompleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DiagnosticsTab(
    diagnosticReport: Map<String, BeaconDiagnosticInfo>,
    dateFormatter: SimpleDateFormat
) {
    if (diagnosticReport.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No diagnostic data available.\nRun diagnostics to generate a report.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                item {
                    Text(
                        text = "Beacon Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Group beacons by health status
                val criticalBeacons = diagnosticReport.values.filter { it.healthStatus == BeaconHealthStatus.CRITICAL }
                val warningBeacons = diagnosticReport.values.filter { it.healthStatus == BeaconHealthStatus.WARNING }
                val goodBeacons = diagnosticReport.values.filter { it.healthStatus == BeaconHealthStatus.GOOD }
                val unknownBeacons = diagnosticReport.values.filter { it.healthStatus == BeaconHealthStatus.UNKNOWN }
                
                if (criticalBeacons.isNotEmpty()) {
                    item {
                        Text(
                            text = "Critical Issues (${criticalBeacons.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(criticalBeacons) { beaconInfo ->
                        BeaconDiagnosticItem(beaconInfo = beaconInfo, dateFormatter = dateFormatter)
                    }
                }
                
                if (warningBeacons.isNotEmpty()) {
                    item {
                        Text(
                            text = "Warnings (${warningBeacons.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFF57C00), // Orange
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(warningBeacons) { beaconInfo ->
                        BeaconDiagnosticItem(beaconInfo = beaconInfo, dateFormatter = dateFormatter)
                    }
                }
                
                if (goodBeacons.isNotEmpty()) {
                    item {
                        Text(
                            text = "Good Condition (${goodBeacons.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF4CAF50), // Green
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(goodBeacons) { beaconInfo ->
                        BeaconDiagnosticItem(beaconInfo = beaconInfo, dateFormatter = dateFormatter)
                    }
                }
                
                if (unknownBeacons.isNotEmpty()) {
                    item {
                        Text(
                            text = "Unknown Status (${unknownBeacons.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(unknownBeacons) { beaconInfo ->
                        BeaconDiagnosticItem(beaconInfo = beaconInfo, dateFormatter = dateFormatter)
                    }
                }
            }
        )
    }
}

@Composable
fun BeaconDiagnosticItem(
    beaconInfo: BeaconDiagnosticInfo,
    dateFormatter: SimpleDateFormat
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Health status indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            when (beaconInfo.healthStatus) {
                                BeaconHealthStatus.CRITICAL -> Color.Red
                                BeaconHealthStatus.WARNING -> Color(0xFFF57C00) // Orange
                                BeaconHealthStatus.GOOD -> Color(0xFF4CAF50) // Green
                                BeaconHealthStatus.UNKNOWN -> Color.Gray
                            },
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = beaconInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = beaconInfo.healthStatus.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (beaconInfo.healthStatus) {
                        BeaconHealthStatus.CRITICAL -> Color.Red
                        BeaconHealthStatus.WARNING -> Color(0xFFF57C00) // Orange
                        BeaconHealthStatus.GOOD -> Color(0xFF4CAF50) // Green
                        BeaconHealthStatus.UNKNOWN -> Color.Gray
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Signal: ${beaconInfo.signalQuality}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    if (beaconInfo.batteryLevel >= 0) {
                        Text(
                            text = "Battery: ${beaconInfo.batteryLevel}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Last seen: ${
                        if (beaconInfo.lastSeenTimestamp > 0) 
                            dateFormatter.format(Date(beaconInfo.lastSeenTimestamp))
                        else 
                            "Never"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Recommended Actions:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                beaconInfo.recommendedActions.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = action,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Distance: ${String.format("%.2f", beaconInfo.estimatedDistance)}m",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Text(
                            text = "Confidence: ${String.format("%.1f", beaconInfo.distanceConfidence * 100)}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Failed detections: ${beaconInfo.failedDetectionCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Text(
                            text = "MAC: ${beaconInfo.macAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Maintenance Priority: ${beaconInfo.maintenancePriority.name}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = when (beaconInfo.maintenancePriority) {
                        MaintenancePriority.URGENT -> Color.Red
                        MaintenancePriority.HIGH -> Color(0xFFF57C00) // Orange
                        MaintenancePriority.MEDIUM -> Color(0xFFFFB300) // Amber
                        MaintenancePriority.LOW -> Color(0xFF4CAF50) // Green
                    }
                )
            }
        }
    }
}

@Composable
fun MaintenanceScheduleTab(
    viewModel: BeaconMaintenanceViewModel,
    dateFormatter: SimpleDateFormat
) {
    val maintenanceSchedule by viewModel.maintenanceSchedule.collectAsState()
    
    if (maintenanceSchedule.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No maintenance schedule available.\nRun diagnostics to generate a schedule.",
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.generateMaintenanceSchedule() }
                ) {
                    Text("Generate Schedule")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                item {
                    Text(
                        text = "Maintenance Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Group by maintenance interval
                val dailyMaintenance = maintenanceSchedule.filter { it.value == 1 }
                val weeklyMaintenance = maintenanceSchedule.filter { it.value == 7 }
                val monthlyMaintenance = maintenanceSchedule.filter { it.value == 30 }
                val quarterlyMaintenance = maintenanceSchedule.filter { it.value == 90 }
                
                if (dailyMaintenance.isNotEmpty()) {
                    item {
                        MaintenanceScheduleSection(
                            title = "Daily Maintenance (${dailyMaintenance.size})",
                            beacons = dailyMaintenance.keys.toList(),
                            viewModel = viewModel
                        )
                    }
                }
                
                if (weeklyMaintenance.isNotEmpty()) {
                    item {
                        MaintenanceScheduleSection(
                            title = "Weekly Maintenance (${weeklyMaintenance.size})",
                            beacons = weeklyMaintenance.keys.toList(),
                            viewModel = viewModel
                        )
                    }
                }
                
                if (monthlyMaintenance.isNotEmpty()) {
                    item {
                        MaintenanceScheduleSection(
                            title = "Monthly Maintenance (${monthlyMaintenance.size})",
                            beacons = monthlyMaintenance.keys.toList(),
                            viewModel = viewModel
                        )
                    }
                }
                
                if (quarterlyMaintenance.isNotEmpty()) {
                    item {
                        MaintenanceScheduleSection(
                            title = "Quarterly Maintenance (${quarterlyMaintenance.size})",
                            beacons = quarterlyMaintenance.keys.toList(),
                            viewModel = viewModel
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun MaintenanceScheduleSection(
    title: String,
    beacons: List<String>,
    viewModel: BeaconMaintenanceViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                beacons.forEach { beaconId ->
                    val beaconInfo = viewModel.getBeaconInfo(beaconId)
                    
                    if (beaconInfo != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Health status indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        when (beaconInfo.healthStatus) {
                                            BeaconHealthStatus.CRITICAL -> Color.Red
                                            BeaconHealthStatus.WARNING -> Color(0xFFF57C00) // Orange
                                            BeaconHealthStatus.GOOD -> Color(0xFF4CAF50) // Green
                                            BeaconHealthStatus.UNKNOWN -> Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = beaconInfo.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            if (beaconInfo.recommendedActions.isNotEmpty() && 
                                beaconInfo.recommendedActions.first() != "No maintenance required") {
                                Text(
                                    text = "${beaconInfo.recommendedActions.size} action(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "No actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (beaconId != beacons.last()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}