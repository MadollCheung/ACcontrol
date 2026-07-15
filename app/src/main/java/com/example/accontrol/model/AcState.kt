package com.example.accontrol.model

data class AcState(
    var isPowerOn: Boolean = false,
    var temperature: Int = 24,
    var fanSpeed: Int = 2,
    var blowMode: Int = 0,
    var isInnerCirculation: Boolean = true,
    var isCoolingOn: Boolean = true
)
