package com.i8700nd.depthanything.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.i8700nd.depthanything.viewmodels.MainViewModel
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun DepthImageUI(viewModel: MainViewModel, depthImage: Bitmap) {
    val inferenceTime by viewModel.inferenceTimeState.collectAsState()
    val inputDataType by viewModel.inputDataTypeState.collectAsState()
    val inputResolution by viewModel.inputResolutionState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                text = "Depth Image",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onClick = { viewModel.closeDepthImage() }
            ) { Text(text = "Close") }
        }
        val rotatedDepth = remember(depthImage) {
            val matrix = Matrix()
            Bitmap.createBitmap(
                depthImage, 0, 0,
                depthImage.width, depthImage.height, matrix, true
            )
        }
        Image(
            modifier = Modifier
                .aspectRatio(rotatedDepth.width.toFloat() / rotatedDepth.height.toFloat())
                .zoomable(rememberZoomState()),
            bitmap = rotatedDepth.asImageBitmap(),
            contentDescription = "Depth Image"
        )
        Text(text = "Inference time: $inferenceTime ms")
        Text(text = "Model used: ${viewModel.getModelName()}")
        inputDataType?.let { Text(text = "Type: $it") }
        inputResolution?.let { Text(text = "Resolution: $it") }
    }
}