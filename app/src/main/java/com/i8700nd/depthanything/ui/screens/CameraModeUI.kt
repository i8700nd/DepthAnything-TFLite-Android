package com.i8700nd.depthanything.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.i8700nd.depthanything.CameraPreview
import com.i8700nd.depthanything.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CameraModeUI"

@Composable
fun CameraModeUI(viewModel: MainViewModel) {
    var depthBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val inferenceTime by viewModel.inferenceTimeState.collectAsState()
    val inputDataType by viewModel.inputDataTypeState.collectAsState()
    val inputResolution by viewModel.inputResolutionState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RectangleShape)
        ) {
            CameraPreview(
                onFrameCaptured = { bitmap ->
                    if (!isProcessing) {
                        isProcessing = true
                        coroutineScope.launch(Dispatchers.Default) {
                            val finalDisplayBitmap = viewModel.processLiveFrame(bitmap)

                            withContext(Dispatchers.Main) {
                                depthBitmap = finalDisplayBitmap
                                isProcessing = false
                                Log.d(TAG, "Depth map Height ${depthBitmap!!.height}, Width ${depthBitmap!!.width} ")
                            }
                        }
                    }
                },
                isProcessing = isProcessing,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RectangleShape)
        ) {
            if (depthBitmap != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = depthBitmap!!.asImageBitmap(),
                    contentDescription = "Depth Map",
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Processing depth...")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { viewModel.setCameraXState(false) }) { Text("Back") }
            Column {
                inputDataType?.let { Text(text = "Type: $it") }
                inputResolution?.let { Text(text = "Resolution: $it") }
                Text(
                    text = "Inference Time: $inferenceTime ms"
                )
            }
        }
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = "Model: ${viewModel.getModelName()}"
        )
    }
}