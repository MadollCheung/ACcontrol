package com.example.accontrol.controller

import android.content.Context
import android.util.Log
import com.example.accontrol.accessibility.HvacAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 空调控制器 —— 委托给 HvacAccessibilityService 操作车机原生控件
 */
class AcController(private val context: Context) {

    companion object {
        private const val TAG = "AcController"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun svc(): HvacAccessibilityService? =
        HvacAccessibilityService.instance.also {
            if (it == null) Log.w(TAG, "辅助功能服务未连接，请先在系统设置中开启")
        }

    private fun run(block: () -> Unit) = scope.launch { block() }

    /** 电源开关 */
    fun setPower(on: Boolean) = run {
        Log.d(TAG, "电源 → $on")
        svc()?.clickPower()
    }

    /** 主驾温度 */
    fun setDriverTemp(temp: Int) = run {
        Log.d(TAG, "主驾温度 → $temp°C")
        svc()?.setTemperature(temp.toFloat())
    }

    /** 副驾温度 */
    fun setPassengerTemp(temp: Int) = run {
        Log.d(TAG, "副驾温度 → $temp°C")
        // H6 副驾温度通常需要先切到副驾区域再调节
        // 如果是双区模式，zone 按钮切换后再设温度
        svc()?.setTemperature(temp.toFloat())
    }

    /** AUTO 自动 */
    fun setAutoTemp(auto: Boolean) = run {
        Log.d(TAG, "AUTO → $auto")
        svc()?.clickAuto()
    }

    /** 风速 0~5 */
    fun setFanSpeed(level: Int) = run {
        Log.d(TAG, "风速 → $level")
        svc()?.setFanSpeed(level)
    }

    /**
     * 出风模式：0=吹脸 1=吹脚 2=除霜
     * 通过循环点击 iv_hvac_blower_mode 切换
     */
    fun setBlowMode(mode: Int) = run {
        Log.d(TAG, "出风模式 → $mode")
        svc()?.clickBlowerMode()
    }

    /**
     * 循环模式：true=内循环 false=外循环
     */
    fun setCirculation(isInternal: Boolean) = run {
        Log.d(TAG, "循环 → ${if (isInternal) "内" else "外"}")
        svc()?.clickCycleMode()
    }

    /**
     * 双区模式开关
     */
    fun setDualZone(dual: Boolean) = run {
        Log.d(TAG, "双区 → $dual")
        svc()?.clickZone()
    }
}
