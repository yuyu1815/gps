package com.example.myapplication.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.myapplication.service.LogFileManager
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitLogsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val logManager = remember(context) { LogFileManager(context) }
    val scope = rememberCoroutineScope()

    var files by remember { mutableStateOf(emptyList<LogFileManager.LogFileInfo>()) }
    var selectedIndex by remember { mutableStateOf(0) }
    var fullText by remember { mutableStateOf("") }
    var onlyInit by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun loadFileText(ctx: Context, fileInfo: LogFileManager.LogFileInfo) {
        isLoading = true
        val text = withContext(Dispatchers.IO) {
            try {
                fileInfo.file.readText()
            } catch (e: Exception) {
                "読み込みに失敗しました: ${e.message}"
            }
        }
        fullText = text
        isLoading = false
    }

    LaunchedEffect(Unit) {
        files = logManager.getLogFiles(LogFileManager.LogType.APP)
        if (files.isNotEmpty()) {
            selectedIndex = 0
            loadFileText(context, files[0])
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.init_logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(getDisplayText(fullText, onlyInit)))
                    }, enabled = fullText.isNotEmpty()) {
                        Text(stringResource(id = R.string.copy))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // File selector
            if (files.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        value = files[selectedIndex].file.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.debug_log_files)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        files.forEachIndexed { idx, info ->
                            DropdownMenuItem(
                                text = { Text(info.file.name) },
                                onClick = {
                                    expanded = false
                                    selectedIndex = idx
                                    // loading will be triggered by LaunchedEffect(selectedIndex)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = onlyInit, onCheckedChange = { onlyInit = it })
                    Text(stringResource(id = R.string.filter_init_only))
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        val sel = files.getOrNull(selectedIndex)
                        if (sel != null) {
                            scope.launch { loadFileText(context, sel) }
                        }
                    }) { Text(stringResource(id = R.string.refresh)) }
                }
            } else {
                Text(stringResource(id = R.string.no_log_files_found))
            }

            Spacer(Modifier.height(12.dp))

            val displayText = getDisplayText(fullText, onlyInit)
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(displayText)
                        }
                    }
                }
            }
        }
    }

    // Handle file change loading
    LaunchedEffect(selectedIndex) {
        val sel = files.getOrNull(selectedIndex)
        if (sel != null) loadFileText(context, sel)
    }
}

private fun getDisplayText(full: String, onlyInit: Boolean): String {
    if (!onlyInit) return full
    val keywords = listOf(
        "[INIT]", "Init", "init", "initialize", "Startup", "Version", "LanguageManager",
        "ArCore", "SensorService", "SensorMonitor", "PdrTracker", "FusionCoordinator"
    )
    return full.lines().filter { line ->
        keywords.any { kw -> line.contains(kw, ignoreCase = true) }
    }.joinToString("\n")
}
