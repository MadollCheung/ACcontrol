package com.example.accontrol.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * GWM 哈弗 H6 车机 HVAC 辅助功能服务
 * 包名：com.gwm.dynamiclauncher
 * 通过查找原生控件 ID 模拟点击来控制空调
 */
class HvacAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "HvacAccessibility"
        const val GWM_PACKAGE = "com.gwm.dynamiclauncher"

        // 原生 HVAC 按钮资源名
        const val ID_BTN_AUTO       = "btn_hvac_auto"
        const val ID_BTN_CYCLE_MODE = "btn_hvac_cycle_mode"
        const val ID_BTN_ZONE       = "btn_hvac_zone"
        const val ID_BTN_AC         = "btn_hvac_ac"
        const val ID_IV_POWER       = "iv_hvac_power"
        const val ID_TV_TEMPERATURE = "hvac_tv_temperature"
        const val ID_FAN_SEEKBAR    = "hvac_fan_speed_seek_bar"

        // 温度增减按钮
        const val ID_VIEW_TEMP_ADD  = "hvac_view_add"
        const val ID_VIEW_TEMP_SUB  = "hvac_view_subtract"

        @Volatile
        var instance: HvacAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "HvacAccessibilityService 已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d(TAG, "HvacAccessibilityService 中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // ==================== 对外接口 ====================

    /** 点击 AUTO 按钮 */
    fun clickAuto(): Boolean = clickById(ID_BTN_AUTO)

    /** 点击 内/外循环 切换 */
    fun clickCycleMode(): Boolean = clickById(ID_BTN_CYCLE_MODE)

    /** 点击 双区 按钮 */
    fun clickZone(): Boolean = clickById(ID_BTN_ZONE)

    /** 点击 AC 按钮 */
    fun clickAc(): Boolean = clickById(ID_BTN_AC)

    /** 升温（点 + 按钮） */
    fun tempUp(): Boolean = clickById(ID_VIEW_TEMP_ADD)

    /** 降温（点 - 按钮） */
    fun tempDown(): Boolean = clickById(ID_VIEW_TEMP_SUB)

    /**
     * 设置温度到目标值
     * 先读当前温度 TextView，再多次点击加减按钮
     */
    fun setTemperature(target: Float): Boolean {
        val current = getCurrentTemp() ?: run {
            Log.w(TAG, "无法读取当前温度，改用相对调节")
            return false
        }
        Log.d(TAG, "当前温度: $current，目标: $target")
        val diff = target - current
        val steps = Math.abs(diff * 2).toInt() // 0.5°C/步
        if (steps == 0) return true
        val btnId = if (diff > 0) ID_VIEW_TEMP_ADD else ID_VIEW_TEMP_SUB
        repeat(steps) {
            clickById(btnId)
            Thread.sleep(80)
        }
        return true
    }

    /**
     * 设置风速（通过 SeekBar 拖拽）
     * level: 0~7
     */
    fun setFanSpeed(level: Int): Boolean {
        val node = findNodeById(ID_FAN_SEEKBAR) ?: run {
            Log.w(TAG, "找不到风速 SeekBar")
            return false
        }
        val bundle = Bundle().apply {
            putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, level.toFloat())
        }
        putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, level.toFloat())
        node.recycle()
        Log.d(TAG, "设置风速 $level: $result")
        return result
    }

    // ==================== 内部工具 ====================

    private fun clickById(resName: String): Boolean {
        val node = findNodeById(resName) ?: run {
            Log.w(TAG, "找不到控件: $resName")
            return false
        }
        val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        node.recycle()
        Log.d(TAG, "点击 $resName: $result")
        return result
    }

    private fun findNodeById(resName: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val fullId = "$GWM_PACKAGE:id/$resName"
        val nodes = root.findAccessibilityNodeInfosByViewId(fullId)
        return if (nodes.isNullOrEmpty()) null else nodes[0]
    }

    private fun getCurrentTemp(): Float? {
        val node = findNodeById(ID_TV_TEMPERATURE) ?: return null
        val text = node.text?.toString() ?: return null
        node.recycle()
        return text.replace("°C", "").replace("°", "").trim().toFloatOrNull()
    }
}
