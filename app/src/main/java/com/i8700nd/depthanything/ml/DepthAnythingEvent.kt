package com.i8700nd.depthanything.ml

import org.tensorflow.lite.DataType

sealed class DepthAnythingEvent {
    data class DataTypeDetermined(val inputDataType: DataType, val resolution: Int) : DepthAnythingEvent()
}