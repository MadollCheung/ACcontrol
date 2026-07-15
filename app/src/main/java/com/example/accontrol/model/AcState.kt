package com.example.accontrol.model

data class AcState(
    var isPowerOn: Boolean = false,
    var driverTemp: Int = 24,       // 主驾温度
    var passengerTemp: Int = 24,    // 副驾温度
    var isAutoTemp: Boolean = false, // AUTO 自动调温
    var fanSpeed: Int = 2,
    var blowMode: Int = 0,
    var isInnerCirculation: Boolean = true
)