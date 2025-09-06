package com.i8700nd.depthanything.ui.screens


import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.i8700nd.depthanything.R
import com.i8700nd.depthanything.ml.DelegateType
import com.i8700nd.depthanything.viewmodels.MainViewModel

@Composable
fun ImageSelectionUI(
    viewModel: MainViewModel,
    onPermissionDenied: () -> Unit,
    onTakePicture: () -> Unit,
) {
    val context = LocalContext.current
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.processGalleryImage(it) }
    }

    val selectedModel by viewModel.selectedModelState.collectAsState()
    val selectedDelegate by viewModel.selectedDelegateState.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = context.getString(R.string.model_name),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = context.getString(R.string.model_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        var expanded by remember { mutableStateOf(false) }
        val models by remember { mutableStateOf(viewModel.listModelsInAssets()) }

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(selectedModel) }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            viewModel.onModelSelected(model)
                            expanded = false
                        }
                    )
                }
            }
        }

        var delegateExpanded by remember { mutableStateOf(false) }
        val delegates = remember { listOf(DelegateType.CPU, DelegateType.GPU, DelegateType.NNAPI) }

        Box {
            OutlinedButton(
                onClick = { delegateExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(selectedDelegate.name) }
            DropdownMenu(
                expanded = delegateExpanded,
                onDismissRequest = { delegateExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                delegates.forEach { delegate ->
                    DropdownMenuItem(
                        text = { Text(delegate.name) },
                        onClick = {
                            viewModel.onDelegateSelected(delegate)
                            delegateExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                onTakePicture()
            } else {
                onPermissionDenied()
            }
        }) { Text(text = "Take A Picture") }

        Button(onClick = {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }) { Text(text = "Select From Gallery") }

        Button(onClick = {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                viewModel.setCameraXState(true)
            } else {
                onPermissionDenied()
            }
        }) { Text(text = "Use Camera for live inference") }
    }
}