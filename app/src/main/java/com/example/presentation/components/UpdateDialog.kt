package com.example.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.core.update.UpdateState
import com.example.core.update.UpdateInfo
import java.io.File

@Composable
fun InAppUpdateDialog(
    updateState: UpdateState,
    onDownload: (UpdateInfo) -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit
) {
    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            val info = updateState.info
            AlertDialog(
                onDismissRequest = {
                    if (!info.isMandatory) onDismiss()
                },
                properties = DialogProperties(
                    dismissOnBackPress = !info.isMandatory,
                    dismissOnClickOutside = !info.isMandatory
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text("Update Available") },
                text = {
                    Column {
                        Text("Version ${info.versionName} is now available.")
                        if (info.isMandatory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This is a mandatory security update. You must update to continue using the app.",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onDownload(info) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Download Update")
                    }
                },
                dismissButton = {
                    if (!info.isMandatory) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Later")
                        }
                    }
                }
            )
        }
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss while downloading */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = { Text("Downloading Update") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { updateState.progress / 100f },
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${updateState.progress}% Downloaded", fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = { }
            )
        }
        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = { /* Must install */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                icon = {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                title = { Text("Ready to Install") },
                text = { Text("The update has been securely verified and is ready to install.") },
                confirmButton = {
                    Button(
                        onClick = { onInstall(updateState.file) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Install Now")
                    }
                }
            )
        }
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Update Error") },
                text = { Text(updateState.message, color = MaterialTheme.colorScheme.error) },
                confirmButton = {
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            )
        }
        else -> {
            // Idle or Checking - Do not show dialog
        }
    }
}
