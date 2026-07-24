package com.example.accontrol.controller

import android.content.Context
import android.util.Log
import com.example.accontrol.accessibility.HvacAccessibilityService
import com.example.accontrol.model.AcState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 空调控制器
 * 通过 HvacAccessibilityService 操控 GWM 哈弗 H6 原生 HVAC 界面
 */
class AcController(private val context: Context) {

    companion object {
        private const val TAG = "AcController"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun svc(): HvacAccessibilityService? {
        val s = HvacAccessibilityService.instance
        if (s == null) Log.w(TAG, "辅助功能服务未连接，请在系统设置中开启「空调控制」辅助功能")
        return s
    }

    private fun run(block: () -> Unit) {
        scope.launch { block() }
    }

    fun setDriverTemp(temp: Float) = run {
        Log.d(TAG, "设置主驾温度: $temp")
        svc()?.setTemperature(temp)
    }

    fun setPassengerTemp(temp: Float) = run {
        Log.d(TAG, "设置副驾温度: $temp")
        svc()?.setTemperature(temp)
    }

    fun setAutoTemp(auto: Boolean) = run {
        Log.d(TAG, "AUTO: $auto")
        svc()?.clickAuto()
    }

    fun setFanSpeed(level: Int) = run {
        Log.d(TAG, "设置风速: $level")
        svc()?.setFanSpeed(level)
    }

    fun setCycleMode(isInternal: Boolean) = run {
        Log.d(TAG, "循环模式: ${if (isInternal) "内循环" else "外循环"}")
        svc()?.clickCycleMode()
    }

    fun setDualZone(dual: Boolean) = run {
        Log.d(TAG, "双区: $dual")
        svc()?.clickZone()
    }

    fun setBlowerMode(mode: Int) = run {
        Log.d(TAG, "出风模式: $mode")
        val s = svc() ?: return@run
        val root = s.rootInActiveWindow ?: return@run
        val nodes = root.findAccessibilityNodeInfosByViewId(
            "${HvacAccessibilityService.GWM_PACKAGE}:id/iv_hvac_blower_mode"
        )
        if (!nodes.isNullOrEmpty()) {
            nodes[0].performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            nodes[0].recycle()
        }
    }
}
