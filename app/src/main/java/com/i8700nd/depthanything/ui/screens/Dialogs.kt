package com.i8700nd.depthanything.ui.screens



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.i8700nd.depthanything.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Permission Required") },
        text = { Text("Camera permission is required to use the camera feature. Please grant permission in app settings.") },
        confirmButton = { Button(onClick = onOpenSettings) { Text("Open Settings") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Dismiss") } }
    )
}

@Composable
fun ProgressDialog(viewModel: MainViewModel) {
    val isShowingProgress by viewModel.progressState.collectAsState()
    if (isShowingProgress) {
        Dialog(onDismissRequest = { /* Not cancellable */ }) {
            Surface(color = Color.White) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text(text = "Processing image ...")
                }
            }
        }
    }
}