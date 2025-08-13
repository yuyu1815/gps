package com.example.myapplication.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.service.LowPowerMode

/**
 * A composable that provides controls for the low-power mode.
 * 
 * @param lowPowerMode The LowPowerMode instance to control
 * @param modifier Modifier to be applied to the component
 */
@Composable
fun LowPowerModeToggle(
    lowPowerMode: LowPowerMode,
    modifier: Modifier = Modifier
) {
    // Collect state from LowPowerMode
    val isEnabled by lowPowerMode.lowPowerModeEnabled.collectAsState()
    val autoEnable by lowPowerMode.autoEnableOnLowBattery.collectAsState()
    val threshold by lowPowerMode.lowBatteryThreshold.collectAsState()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = "Low-Power Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_lock_idle_low_battery),
                    contentDescription = "Battery icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Enable Low-Power Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { lowPowerMode.setLowPowerModeEnabled(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto-enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-enable on low battery",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Switch(
                    checked = autoEnable,
                    onCheckedChange = { lowPowerMode.setAutoEnableOnLowBattery(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Threshold slider
            Text(
                text = "Low battery threshold: $threshold%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = threshold.toFloat(),
                onValueChange = { lowPowerMode.setLowBatteryThreshold(it.toInt()) },
                valueRange = 5f..50f,
                steps = 45,
                enabled = autoEnable
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = "Low-power mode reduces battery consumption by decreasing scanning frequency and sensor sampling rate. This may affect positioning accuracy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Preview of the LowPowerModeToggle component.
 */
@Preview(showBackground = true)
@Composable
fun LowPowerModeTogglePreview() {
    // Create a simple preview with dummy data
    val isEnabled = remember { mutableStateOf(false) }
    val autoEnable = remember { mutableStateOf(true) }
    val threshold = remember { mutableStateOf(20) }
    
    // Create a dummy component that mimics the real one for preview
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = "Low-Power Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_lock_idle_low_battery),
                    contentDescription = "Battery icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Enable Low-Power Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Switch(
                    checked = isEnabled.value,
                    onCheckedChange = { isEnabled.value = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto-enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-enable on low battery",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Switch(
                    checked = autoEnable.value,
                    onCheckedChange = { autoEnable.value = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Threshold slider
            Text(
                text = "Low battery threshold: ${threshold.value}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = threshold.value.toFloat(),
                onValueChange = { threshold.value = it.toInt() },
                valueRange = 5f..50f,
                steps = 45,
                enabled = autoEnable.value
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = "Low-power mode reduces battery consumption by decreasing scanning frequency and sensor sampling rate. This may affect positioning accuracy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}