package com.example.accontrol.controller

import android.util.Log

class AcController {
    companion object { private const val TAG = "AcController" }

    fun setPower(on: Boolean) = send("AC_POWER", if (on) "1" else "0")
    fun setDriverTemp(temp: Int) { require(temp in 17..32); send("AC_DRIVER_TEMP", temp.toString()) }
    fun setPassengerTemp(temp: Int) { require(temp in 17..32); send("AC_PASSENGER_TEMP", temp.toString()) }
    fun setAutoTemp(on: Boolean) = send("AC_AUTO_TEMP", if (on) "1" else "0")
    fun setFanSpeed(speed: Int) { require(speed in 0..5); send("AC_FAN_SPEED", speed.toString()) }
    fun setBlowMode(mode: Int) = send("AC_BLOW_MODE", when (mode) { 1 -> "FEET"; 2 -> "DEFROST"; else -> "FACE" })
    fun setDualZone(dual: Boolean) = send("AC_DUAL_ZONE", if (dual) "1" else "0")
    fun setCirculation(inner: Boolean) = send("AC_CIRCULATION", if (inner) "INNER" else "OUTER")

    private fun send(property: String, value: String) {
        Log.i(TAG, "[VehicleAPI] $property = $value")
        // TODO: replace with real vehicle SDK call
    }
}
