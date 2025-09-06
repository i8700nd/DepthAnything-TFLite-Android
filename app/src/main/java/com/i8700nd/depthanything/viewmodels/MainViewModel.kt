package com.i8700nd.depthanything.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.i8700nd.depthanything.ml.DelegateType
import com.i8700nd.depthanything.ml.DepthAnything
import com.i8700nd.depthanything.ml.DepthAnythingEvent
import com.i8700nd.depthanything.ml.colormapInferno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import androidx.core.graphics.scale
import kotlin.math.roundToInt

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _depthImageState = MutableStateFlow<Bitmap?>(null)
    val depthImageState: StateFlow<Bitmap?> = _depthImageState.asStateFlow()

    private val _inferenceTimeState = MutableStateFlow(0L)
    val inferenceTimeState: StateFlow<Long> = _inferenceTimeState.asStateFlow()

    private val _progressState = MutableStateFlow(false)
    val progressState: StateFlow<Boolean> = _progressState.asStateFlow()

    private val _cameraXState = MutableStateFlow(false)
    val cameraXState: StateFlow<Boolean> = _cameraXState.asStateFlow()

    private val _selectedModelState = MutableStateFlow("Depth-Anything-V2.tflite")
    val selectedModelState: StateFlow<String> = _selectedModelState.asStateFlow()

    private val _selectedDelegateState = MutableStateFlow(DelegateType.CPU)
    val selectedDelegateState: StateFlow<DelegateType> = _selectedDelegateState.asStateFlow()

    private val _inputDataTypeState = MutableStateFlow<DataType?>(null)
    val inputDataTypeState: StateFlow<DataType?> = _inputDataTypeState.asStateFlow()

    private val _inputResolutionState = MutableStateFlow<Int?>(null)
    val inputResolutionState: StateFlow<Int?> = _inputResolutionState.asStateFlow()

    private var depthAnything: DepthAnything

    init {
        depthAnything = DepthAnything(
            application.applicationContext,
            _selectedModelState.value,
            _selectedDelegateState.value
        )
        collectDepthAnythingEvents()
    }

    private fun collectDepthAnythingEvents() {
        viewModelScope.launch {
            depthAnything.events.collect { event ->
                when (event) {
                    is DepthAnythingEvent.DataTypeDetermined -> {
                        _inputDataTypeState.value = event.inputDataType
                        _inputResolutionState.value = event.resolution
                    }
                }
            }
        }
    }

    fun onModelSelected(model: String) {
        _selectedModelState.value = model
        reinitializeDepthAnything()
    }

    fun onDelegateSelected(delegate: DelegateType) {
        _selectedDelegateState.value = delegate
        reinitializeDepthAnything()
    }

    fun processGalleryImage(uri: Uri) {
        _progressState.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val originalBitmap = getFixedBitmap(uri)
            Log.d(TAG, "Original gallery size: ${originalBitmap.width}x${originalBitmap.height}")

            val resizedBitmap = resizeBitmap(originalBitmap, 480)
            Log.d(TAG, "Resized gallery size: ${resizedBitmap.width}x${resizedBitmap.height}")

            val (depthMap, inferenceTime) = depthAnything.predict(resizedBitmap)
            _depthImageState.value = colormapInferno(depthMap)
            _inferenceTimeState.value = inferenceTime
            withContext(Dispatchers.Main) {
                _progressState.value = false
            }
            _depthImageState.value?.let {
                Log.d(TAG, "Depth map Height ${it.height}, Width ${it.width} ")
            }
        }
    }

    fun processTakenPicture(photoPath: String) {
        _progressState.value = true
        viewModelScope.launch(Dispatchers.Default) {
            var originalBitmap = BitmapFactory.decodeFile(photoPath)

            val exifInterface = ExifInterface(photoPath)
            originalBitmap = when (exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                else -> originalBitmap
            }
            Log.d(
                TAG,
                "Original taken picture size: ${originalBitmap.width}x${originalBitmap.height}"
            )

            val resizedBitmap = resizeBitmap(originalBitmap, 480)
            Log.d(TAG, "Resized taken picture size: ${resizedBitmap.width}x${resizedBitmap.height}")

            val (depthMap, inferenceTime) = depthAnything.predict(resizedBitmap)
            _depthImageState.value = colormapInferno(depthMap)
            _inferenceTimeState.value = inferenceTime
            withContext(Dispatchers.Main) {
                _progressState.value = false
            }
            _depthImageState.value?.let {
                Log.d(TAG, "Depth map Height ${it.height}, Width ${it.width} ")
            }
        }
    }

    suspend fun processLiveFrame(bitmap: Bitmap): Bitmap {
        val (depthMap, inferenceTime) = depthAnything.predict(bitmap)

        withContext(Dispatchers.Main) {
            _inferenceTimeState.value = inferenceTime
        }

        val colormapped = colormapInferno(depthMap)
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(
            colormapped, 0, 0,
            colormapped.width, colormapped.height, matrix, true
        )
    }

    fun setCameraXState(enabled: Boolean) {
        _cameraXState.value = enabled
    }

    fun closeDepthImage() {
        _depthImageState.value = null
    }

    fun listModelsInAssets(): List<String> {
        return getApplication<Application>().assets.list("")?.filter { it.endsWith(".tflite") }
            ?: emptyList()
    }

    fun getModelName(): String = depthAnything.modelName


    private fun reinitializeDepthAnything() {
        depthAnything.close()
        depthAnything = DepthAnything(
            getApplication<Application>().applicationContext,
            _selectedModelState.value,
            _selectedDelegateState.value
        )
        collectDepthAnythingEvents()
    }

    private fun resizeBitmap(bitmap: Bitmap, targetMinDim: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        if (originalWidth <= targetMinDim && originalHeight <= targetMinDim) {
            return bitmap
        }

        val scale: Float
        val newWidth: Int
        val newHeight: Int

        if (originalWidth < originalHeight) {
            // Portrait or square: Scale based on width
            scale = targetMinDim.toFloat() / originalWidth
            newWidth = targetMinDim
            newHeight = (originalHeight * scale).roundToInt()
        } else {
            // Landscape or square: Scale based on height
            scale = targetMinDim.toFloat() / originalHeight
            newHeight = targetMinDim
            newWidth = (originalWidth * scale).roundToInt()
        }

        Log.d(TAG, "Resizing from ${originalWidth}x${originalHeight} to ${newWidth}x${newHeight}")
        return bitmap.scale(newWidth, newHeight)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
    }

    private fun getFixedBitmap(imageFileUri: Uri): Bitmap {
        val context = getApplication<Application>().applicationContext
        var imageBitmap =
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageFileUri))
        val exifInterface = ExifInterface(context.contentResolver.openInputStream(imageFileUri)!!)
        imageBitmap = when (exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(imageBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(imageBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(imageBitmap, 270f)
            else -> imageBitmap
        }
        return imageBitmap
    }

    override fun onCleared() {
        super.onCleared()
        depthAnything.close()
    }
}