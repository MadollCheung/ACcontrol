package com.example.accontrol.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * HVAC 无障碍服务 - 基于 R.java 真实 ID
 *
 * 目标包：com.gwm.dynamiclauncher（灵控球）
 *
 * 关键控件 ID（来自车机 R.java R.id 类）：
 *   iv_hvac_power        = 2131362829  ← 电源
 *   iv_hvac_auto         = 2131362824  ← AUTO
 *   iv_hvac_ac           = 2131362823  ← A/C
 *   iv_hvac_cycle_mode   = 2131362826  ← 内/外循环
 *   iv_hvac_zone         = 2131362830  ← 单/双区
 *   iv_hvac_blower_mode  = 2131362825  ← 出风模式
 *   iv_hvac_fan_speed    = 2131362827  ← 风速图标
 *   hvac_view_add        = 2131362660  ← 温度+
 *   hvac_view_subtract   = 2131362662  ← 温度-
 *   hvac_fan_speed_seek_bar = 2131362658  ← 风速滑条
 *   hvac_tv_temperature  = 2131362659  ← 温度文本
 *   btn_hvac_ac          = 2131231266  ← 备用布局 A/C
 *   btn_hvac_auto        = 2131231267  ← 备用布局 AUTO
 *   btn_hvac_cycle_mode  = 2131231268  ← 备用布局循环
 *   btn_hvac_zone        = 2131231270  ← 备用布局区域
 */
class HvacAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "HvacSvc"
        const val PKG = "com.gwm.dynamiclauncher"

        // iv_hvac_* 系列（主悬浮窗）
        private const val ID_POWER       = "$PKG:id/iv_hvac_power"
        private const val ID_AUTO        = "$PKG:id/iv_hvac_auto"
        private const val ID_AC          = "$PKG:id/iv_hvac_ac"
        private const val ID_CYCLE_MODE  = "$PKG:id/iv_hvac_cycle_mode"
        private const val ID_ZONE        = "$PKG:id/iv_hvac_zone"
        private const val ID_BLOWER_MODE = "$PKG:id/iv_hvac_blower_mode"
        private const val ID_FAN_IV      = "$PKG:id/iv_hvac_fan_speed"

        // 温度加减按钮
        private const val ID_TEMP_ADD    = "$PKG:id/hvac_view_add"
        private const val ID_TEMP_SUB    = "$PKG:id/hvac_view_subtract"

        // 风速 SeekBar + 温度文本
        private const val ID_FAN_SEEKBAR = "$PKG:id/hvac_fan_speed_seek_bar"
        private const val ID_TV_TEMP     = "$PKG:id/hvac_tv_temperature"

        // btn_hvac_* 系列（备用/展开面板）
        private const val ID_BTN_AC      = "$PKG:id/btn_hvac_ac"
        private const val ID_BTN_AUTO    = "$PKG:id/btn_hvac_auto"
        private const val ID_BTN_CYCLE   = "$PKG:id/btn_hvac_cycle_mode"
        private const val ID_BTN_ZONE    = "$PKG:id/btn_hvac_zone"

        @Volatile
        var instance: HvacAccessibilityService? = null
    }

    // ── 生命周期 ────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "HVAC 辅助服务已连接")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "HVAC 辅助服务已断开")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // ── 公开操作 API ────────────────────────────────────────────────

    /** 电源开关 */
    fun clickPower() = performClick(ID_POWER)

    /** AUTO 自动模式（主布局 + 备用布局各试一次） */
    fun clickAuto() = performClick(ID_AUTO).also { if (!it) performClick(ID_BTN_AUTO) }

    /** A/C 压缩机开关 */
    fun clickAc() = performClick(ID_AC).also { if (!it) performClick(ID_BTN_AC) }

    /** 内/外循环切换 */
    fun clickCycleMode() = performClick(ID_CYCLE_MODE).also { if (!it) performClick(ID_BTN_CYCLE) }

    /** 单/双区切换 */
    fun clickZone() = performClick(ID_ZONE).also { if (!it) performClick(ID_BTN_ZONE) }

    /** 出风模式循环切换 */
    fun clickBlowerMode() = performClick(ID_BLOWER_MODE)

    /** 温度 +0.5°C */
    fun tempUp() = performClick(ID_TEMP_ADD)

    /** 温度 -0.5°C */
    fun tempDown() = performClick(ID_TEMP_SUB)

    /**
     * 设置目标温度（读取当前温度后反复加减）
     * @param target 目标温度 17.0 ~ 32.0（支持 0.5 步进）
     */
    fun setTemperature(target: Float) {
        val current = readCurrentTemp()
        if (current == null) {
            Log.w(TAG, "无法读取当前温度，跳过精确调节")
            return
        }
        val diff = target - current
        val steps = Math.round(Math.abs(diff) / 0.5f)
        Log.d(TAG, "温度: 当前=$current 目标=$target 步数=$steps")
        if (diff > 0) {
            repeat(steps) { performClick(ID_TEMP_ADD); Thread.sleep(150) }
        } else if (diff < 0) {
            repeat(steps) { performClick(ID_TEMP_SUB); Thread.sleep(150) }
        }
    }

    /**
     * 设置风速（通过 SeekBar ACTION_SET_PROGRESS）
     * @param level 0(自动/最低) ~ 5(最高)
     */
 fun setFanSpeed(level: Int) {
    val node = findNodeById(ID_FAN_SEEKBAR)
    if (node == null) {
        Log.w(TAG, "未找到风速 SeekBar，尝试点击风速图标")
        performClick(ID_FAN_IV)
        return
    }
    
    // Use scroll actions to adjust SeekBar
    val currentProgress = node.rangeInfo?.current?.toInt() ?: 0
    val targetProgress = level
    val diff = targetProgress - currentProgress
    
    val ok = if (diff > 0) {
        repeat(diff) { node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) }
        true
    } else if (diff < 0) {
        repeat(Math.abs(diff)) { node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) }
        true
    } else {
        true
    }
    
    node.recycle()
    Log.d(TAG, "风速 level=$level 设置结果=$ok")
}

    /**
     * 通用点击：传入短名（如 "iv_hvac_power"）或完整 resource-id
     * 供 AcController 调用
     */
    fun clickById(shortName: String): Boolean {
        val fullId = if (shortName.contains(":")) shortName else "$PKG:id/$shortName"
        return performClick(fullId)
    }

    // ── 内部工具 ─────────────────────────────────────────────────────

    /** 查找节点并点击 */
    private fun performClick(resId: String): Boolean {
        val node = findNodeById(resId) ?: run {
            Log.v(TAG, "未找到节点: $resId")
            return false
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        node.recycle()
        Log.d(TAG, "点击 $resId → $ok")
        return ok
    }

    /** 查找节点：先用系统 API，失败则递归遍历 */
    private fun findNodeById(resId: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(resId)
        if (!nodes.isNullOrEmpty()) return nodes[0]
        return traverseTree(root, resId)
    }

    /** 递归遍历无障碍树 */
    private fun traverseTree(node: AccessibilityNodeInfo, resId: String): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == resId) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = traverseTree(child, resId)
            if (found != null) return found
            if (found == null) child.recycle()
        }
        return null
    }

    /** 读取当前温度（从 hvac_tv_temperature 文本，如 "25°C" → 25.0f） */
    private fun readCurrentTemp(): Float? {
        val node = findNodeById(ID_TV_TEMP) ?: return null
        val raw = node.text?.toString()
        node.recycle()
        return raw?.replace("°C", "")?.replace("°", "")?.trim()?.toFloatOrNull()
    }
}
