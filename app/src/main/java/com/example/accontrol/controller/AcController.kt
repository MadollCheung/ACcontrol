package com.example.accontrol.controller

import android.util.Log

class AcController {
    companion object { private const val TAG = "AcController" }

    fun setPower(on: Boolean) = send("AC_POWER", if (on) "1" else "0")
    fun setTemperature(temp: Int) { require(temp in 16..32); send("AC_TEMP", temp.toString()) }
    fun setFanSpeed(speed: Int) { require(speed in 0..5); send("AC_FAN_SPEED", speed.toString()) }
    fun setBlowMode(mode: Int) = send("AC_BLOW_MODE", when(mode){ 1->"FEET"; 2->"DEFROST"; else->"FACE" })
    fun setCirculation(inner: Boolean) = send("AC_CIRCULATION", if (inner) "INNER" else "OUTER")
    fun setCooling(on: Boolean) = send("AC_COOLING", if (on) "1" else "0")

    private fun send(property: String, value: String) {
        Log.i(TAG, "[VehicleAPI] $property = $value")
        // TODO: 替换为真实车厂 SDK 调用
    }
}
