package com.i8700nd.depthanything.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.Interpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.File
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.flex.FlexDelegate

enum class DelegateType { CPU, GPU, NNAPI }
private const val TAG = "DepthAnythingHelper"

class DepthAnything(context: Context, val modelName: String, delegateType: DelegateType) {

    private val tflite: Interpreter
    private val inputDim: Int
    private val outputDim: Int
    private var inputDataType: DataType
    private var outputDataType: DataType

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _events = MutableSharedFlow<DepthAnythingEvent>(replay = 1)
    val events = _events.asSharedFlow()

    init {
        val model = loadModelFile(context, modelName)
        val options = Interpreter.Options()
        options.numThreads = 4
        options.addDelegate(FlexDelegate())
        when (delegateType) {
            DelegateType.GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    val gpuOptions = GpuDelegate.Options()
                    gpuOptions.isPrecisionLossAllowed = true
                    gpuOptions.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                    options.addDelegate(GpuDelegate(gpuOptions))
                    Log.d(TAG, "GpuDelegate Added with FP16 enabled")
                } else {
                    Log.e(TAG, "GPU Not supported")
                }
            }

            DelegateType.NNAPI -> {
                options.setUseNNAPI(true)
                options.setUseXNNPACK(true)
                val nnOptions = NnApiDelegate.Options().also {
                    it.executionPreference =
                        NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED
                    it.allowFp16 = true
                }
                options.addDelegate(NnApiDelegate(nnOptions))
                Log.d(TAG, "NnApiDelegate Added")
            }

            DelegateType.CPU -> {}
        }
        Log.d(TAG, "create: Model Delegates: " + options.delegates + " useNNAPI " + options.useNNAPI)
        tflite = Interpreter(model, options)

        // Assuming NHWC format: [1, height, width, channels]
        inputDim = tflite.getInputTensor(0).shape()[1]
        outputDim = tflite.getOutputTensor(0).shape()[1]

        inputDataType = tflite.getInputTensor(0).dataType()
        outputDataType = tflite.getOutputTensor(0).dataType()

        coroutineScope.launch {
            _events.emit(DepthAnythingEvent.DataTypeDetermined(inputDataType, inputDim))
        }

        Log.d(TAG, "Input dim: $inputDim, Output dim: $outputDim")
        Log.d(TAG, "Input type: $inputDataType, Output type: $outputDataType")
    }

    @Throws(Exception::class)
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private val rotateTransform = Matrix().apply { postRotate(0f) }

    suspend fun predict(inputImage: Bitmap): Pair<Bitmap, Long> =
        withContext(Dispatchers.Default) {
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputDim, inputDim, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            var tensorImage = TensorImage(inputDataType)
            tensorImage.load(inputImage)
            tensorImage = imageProcessor.process(tensorImage)

            // Prepare output
            val outputDataType = tflite.getOutputTensor(0).dataType()
            val outputShape = intArrayOf(1, outputDim, outputDim, 1)
            val outputSize = outputShape[1] * outputShape[2] * when (outputDataType) {
                DataType.FLOAT32 -> 4
                DataType.UINT8 -> 1
                else -> 4
            }

            val outputBuffer = ByteBuffer.allocateDirect(outputSize).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            val t1 = System.currentTimeMillis()
            tflite.run(tensorImage.buffer, outputBuffer)
            val inferenceTime = System.currentTimeMillis() - t1

            // Process output
            outputBuffer.rewind()
            val depthMap = processOutput(outputBuffer, outputDataType, outputDim)
            val scaledDepthMap = depthMap.scale(inputImage.width, inputImage.height)

            Pair(scaledDepthMap, inferenceTime)
        }

    private fun processOutput(outputBuffer: ByteBuffer, dataType: DataType, dim: Int): Bitmap {
        return when (dataType) {
            DataType.FLOAT32 -> processFloatOutput(outputBuffer, dim)
            DataType.UINT8 -> processQuantizedOutput(outputBuffer, dim)
            else -> processFloatOutput(outputBuffer, dim)
        }
    }

    private fun processFloatOutput(outputBuffer: ByteBuffer, dim: Int): Bitmap {
        val floatBuffer = outputBuffer.asFloatBuffer()
        val pixels = FloatArray(dim * dim)
        floatBuffer.get(pixels)

        val min = pixels.minOrNull() ?: 0f
        val max = pixels.maxOrNull() ?: 1f
        Log.d(TAG, "Float output range: min=$min, max=$max")

        val bitmap = createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
        val range = max - min

        for (y in 0..<dim) {
            for (x in 0..<dim) {
                val value = pixels[y * dim + x]
                val normalizedValue = if (range > 0) ((value - min) / range * 255).toInt() else 0
                val clampedValue = normalizedValue.coerceIn(0, 255)

                val color = Color.argb(255, clampedValue, clampedValue, clampedValue)
                bitmap[x, y] = color
            }
        }

        return Bitmap.createBitmap(bitmap, 0, 0, dim, dim, rotateTransform, false)
    }

    private fun processQuantizedOutput(outputBuffer: ByteBuffer, dim: Int): Bitmap {
        val pixels = ByteArray(dim * dim)
        outputBuffer.get(pixels)

        val min = pixels.minOrNull() ?: 0f
        val max = pixels.maxOrNull() ?: 1f
        Log.d(TAG, "Int output range: min=$min, max=$max")

        val bitmap = createBitmap(dim, dim)

        for (y in 0..<dim) {
            for (x in 0..<dim) {
                val value = pixels[y * dim + x].toInt() and 0xFF
                val invertedValue = 255 - value
                val color = Color.argb(255, invertedValue, invertedValue, invertedValue)
                bitmap[x, y] = color
            }
        }

        return Bitmap.createBitmap(bitmap, 0, 0, dim, dim, rotateTransform, false)
    }

    fun close() {
        tflite.close()
        coroutineScope.cancel()
    }
}