package com.i8700nd.depthanything

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.i8700nd.depthanything.ui.screens.CameraModeUI
import com.i8700nd.depthanything.ui.screens.DepthImageUI
import com.i8700nd.depthanything.ui.screens.ImageSelectionUI
import com.i8700nd.depthanything.ui.screens.PermissionDeniedDialog
import com.i8700nd.depthanything.ui.screens.ProgressDialog
import com.i8700nd.depthanything.ui.theme.DepthAnythingTheme
import com.i8700nd.depthanything.viewmodels.MainViewModel
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var currentPhotoPath: String = ""

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                viewModel.processTakenPicture(currentPhotoPath)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActivityUI()
        }
    }

    @Composable
    private fun ActivityUI() {
        var showPermissionDialog by remember { mutableStateOf(false) }
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            showPermissionDialog = !isGranted
        }

        LaunchedEffect(Unit) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        DepthAnythingTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val depthImage by viewModel.depthImageState.collectAsState()
                val showCameraX by viewModel.cameraXState.collectAsState()

                ProgressDialog(viewModel)

                if (showPermissionDialog) {
                    PermissionDeniedDialog(
                        onDismiss = { showPermissionDialog = false },
                        onOpenSettings = {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                            startActivity(intent)
                        }
                    )
                }

                if (depthImage != null) {
                    DepthImageUI(viewModel = viewModel, depthImage = depthImage!!)
                } else if (showCameraX) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        CameraModeUI(viewModel = viewModel)
                    } else {
                        viewModel.setCameraXState(false)
                        showPermissionDialog = true
                    }
                } else {
                    ImageSelectionUI(
                        viewModel = viewModel,
                        onPermissionDenied = { showPermissionDialog = true },
                        onTakePicture = { dispatchTakePictureIntent() }
                    )
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? =
                try {
                    val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    File.createTempFile("image", ".jpg", imagesDir).apply {
                        currentPhotoPath = absolutePath
                    }
                } catch (ex: IOException) {
                    null
                }
            photoFile?.also {
                val photoURI =
                    FileProvider.getUriForFile(this, "com.i8700nd.depthanything", it)
                takePictureLauncher.launch(photoURI)
            }
        }
    }
}
