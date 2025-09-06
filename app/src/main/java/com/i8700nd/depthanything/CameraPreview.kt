package com.i8700nd.depthanything

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    onFrameCaptured: (Bitmap) -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera provider", e)
        }
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                analysisExecutor.shutdown()
                Log.d(TAG, "Camera shut down gracefully")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing camera", e)
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        LaunchedEffect(previewView, cameraProvider) {
            val provider = cameraProvider ?: return@LaunchedEffect
            try {
                provider.unbindAll()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(640, 480))
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (!isProcessing) {
                        try {
                            val bitmap = imageProxy.toBitmap()
                            onFrameCaptured(bitmap)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing frame", e)
                        } finally {
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera use cases", e)
            }
        }
    }
}