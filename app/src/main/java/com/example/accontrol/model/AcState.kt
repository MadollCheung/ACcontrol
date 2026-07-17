package com.example.accontrol.model

data class AcState(
    var isPowerOn: Boolean = false,
    var driverTemp: Int = 25,
    var passengerTemp: Int = 25,
    var isAutoTemp: Boolean = false,
    var fanSpeed: Int = 2,
    var blowMode: Int = 0,
    var isInnerCirculation: Boolean = true,
    var isDualZone: Boolean = false
)
