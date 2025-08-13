package com.example.myapplication.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.service.VersionChecker

/**
 * A dialog that notifies the user about an available update.
 *
 * @param versionInfo Information about the current and latest versions
 * @param onDismiss Callback when the user dismisses the dialog
 * @param onUpdateLater Callback when the user chooses to update later
 */
@Composable
fun UpdateNotificationDialog(
    versionInfo: VersionChecker.VersionCheckResult.UpdateAvailable,
    onDismiss: () -> Unit,
    onUpdateLater: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = { 
            if (!versionInfo.latestVersion.forceUpdate) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "A new version of the app is available!",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Current version: ${versionInfo.currentVersion.versionName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "New version: ${versionInfo.latestVersion.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                versionInfo.latestVersion.releaseNotes?.let { notes ->
                    Text(
                        text = "What's new:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (versionInfo.latestVersion.forceUpdate) {
                    Text(
                        text = "This update is required to continue using the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Open the browser with the update URL
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.latestVersion.updateUrl))
                    context.startActivity(intent)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!versionInfo.latestVersion.forceUpdate) {
                OutlinedButton(
                    onClick = {
                        onUpdateLater()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Later")
                }
            }
        }
    )
}