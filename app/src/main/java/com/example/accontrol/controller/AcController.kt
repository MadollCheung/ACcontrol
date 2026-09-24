package com.example.accontrol.controller

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.adayo.proxy.setting.hvac.controller.HvacManager

/**
 * 空调控制器 —— 直接调用 HvacManager 系统 API
 *
 * 参考自已验证可用的 com.hy.acplugin（RealAcController.java）
 *
 * 出风模式映射（UI → 车机信号）：
 *   UI 0=吹脸 → CAR 1
 *   UI 1=吹脚 → CAR 3
 *   UI 2=吹脸+脚 → CAR 2
 *   UI 3=吹脚+除霜 → CAR 4
 *
 * 写操作后需 kill com.adayo.app.hvac 防止状态被覆盖
 */
class AcController(private val context: Context) {

    companion object {
        private const val TAG = "AcController"
        private const val HVAC_APP = "com.adayo.app.hvac"
        private const val SUPPRESS_MS = 1500L

        // UI blowMode → 车机信号值
        private val BLOW_UI_TO_CAR = intArrayOf(1, 3, 2, 4)
        // 车机信号值 → UI blowMode（车机返回 0~3）
        private val BLOW_SIGNAL_TO_UI = intArrayOf(0, 2, 1, 3)

        const val AREA_DRIVER    = HvacManager.AREA_DRIVER     // 1
        const val AREA_PASSENGER = HvacManager.AREA_PASSENGER  // 4

        const val FAN_MIN  = 1
        const val FAN_MAX  = 7
        const val TEMP_MIN = 16.0f
        const val TEMP_MAX = 32.0f
        const val TEMP_STEP = 0.5f
    }

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var suppressUntil = 0L

    private fun hvac(): HvacManager = HvacManager.getInstance()

    /**
     * 执行写操作：设置抑制时间窗口，写完 kill HVAC app 防止状态被覆盖
     */
    private fun write(block: () -> Unit) {
        suppressUntil = SystemClock.uptimeMillis() + SUPPRESS_MS
        try {
            block()
        } catch (e: Throwable) {
            Log.e(TAG, "空调控制失败", e)
        }
        killHvacApp()
    }

    private fun killHvacApp() {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.killBackgroundProcesses(HVAC_APP)
        } catch (_: Throwable) {}
    }

    // ── 温度 ──────────────────────────────────────────────────────

    /** 设置主驾温度（16.0~32.0，步进0.5） */
    fun setDriverTemp(temp: Int) {
        val t = temp.toFloat().coerceIn(TEMP_MIN, TEMP_MAX)
        Log.d(TAG, "主驾温度 → $t")
        write { hvac().setHvacTemperatureSet(AREA_DRIVER, t) }
    }

    /** 设置副驾温度 */
    fun setPassengerTemp(temp: Int) {
        val t = temp.toFloat().coerceIn(TEMP_MIN, TEMP_MAX)
        Log.d(TAG, "副驾温度 → $t")
        write { hvac().setHvacTemperatureSet(AREA_PASSENGER, t) }
    }

    /** 主驾温度 +0.5°C */
    fun tempUp() {
        val cur = hvac().getHvacTemperatureSet(AREA_DRIVER)
        write { hvac().setHvacTemperatureSet(AREA_DRIVER, (cur + TEMP_STEP).coerceAtMost(TEMP_MAX)) }
    }

    /** 主驾温度 -0.5°C */
    fun tempDown() {
        val cur = hvac().getHvacTemperatureSet(AREA_DRIVER)
        write { hvac().setHvacTemperatureSet(AREA_DRIVER, (cur - TEMP_STEP).coerceAtLeast(TEMP_MIN)) }
    }

    // ── 风速 ──────────────────────────────────────────────────────

    /** 设置风速（1~7） */
    fun setFanSpeed(level: Int) {
        val l = level.coerceIn(FAN_MIN, FAN_MAX)
        Log.d(TAG, "风速 → $l")
        write { hvac().setHvacFanSpeed(l) }
    }

    fun fanUp()   { setFanSpeed(hvac().getHvacFanSpeed() + 1) }
    fun fanDown() { setFanSpeed(hvac().getHvacFanSpeed() - 1) }

    // ── A/C 压缩机 ────────────────────────────────────────────────

    fun setPower(on: Boolean) {
        Log.d(TAG, "电源 → $on")
        // 参考插件：setHvacPowerOn(POWER_ON/POWER_OFF)
        write { hvac().setHvacPowerOn(if (on) HvacManager.POWER_ON else HvacManager.POWER_OFF) }
    }

    fun setAcOn(on: Boolean) {
        Log.d(TAG, "A/C → $on")
        write { hvac().setHvacAcOn(HvacManager.SW_ON) }  // 切换，与参考插件一致
    }

    fun toggleAc() = write { hvac().setHvacAcOn(HvacManager.SW_ON) }

    // ── AUTO ──────────────────────────────────────────────────────

    fun setAutoTemp(on: Boolean) {
        Log.d(TAG, "AUTO → $on")
        write { hvac().setHvacAutoOn(HvacManager.SW_ON) }
    }

    fun toggleAuto() = write { hvac().setHvacAutoOn(HvacManager.SW_ON) }

    // ── 内/外循环 ─────────────────────────────────────────────────

    /**
     * 切换循环模式。
     * 注意：参考插件读取当前状态取反，避免系统卡死。
     * getHvacRecircOn()==0 表示内循环开启（state.recircOn=true）
     */
    fun setCirculation(isInternal: Boolean) {
        Log.d(TAG, "循环 → ${if (isInternal) "内" else "外"}")
        write { hvac().setHvacRecircOn(HvacManager.SW_ON) }
    }

    fun toggleRecirc() = write { hvac().setHvacRecircOn(HvacManager.SW_ON) }

    // ── 出风模式 ──────────────────────────────────────────────────

    /**
     * 设置出风模式
     * @param mode UI值：0=吹脸 1=吹脚 2=吹脸+脚 3=吹脚+除霜
     */
    fun setBlowMode(mode: Int) {
        val carVal = BLOW_UI_TO_CAR[mode.coerceIn(0, 3)]
        Log.d(TAG, "出风模式 UI=$mode → CAR=$carVal")
        write { hvac().setHvacFanDerection(carVal) }
    }

    // ── 双区 ──────────────────────────────────────────────────────

    fun setDualZone(dual: Boolean) {
        Log.d(TAG, "双区 → $dual")
        write { hvac().setHvacDualOn(dual) }
    }

    // ── 读取当前状态（供 UI 刷新） ────────────────────────────────

    fun readState(): AcSnapshot? {
        if (SystemClock.uptimeMillis() < suppressUntil) return null  // 写操作抑制期内不读
        return try {
            val h = hvac()
            val fanDir = h.getHvacFanDerection()
            AcSnapshot(
                isPowerOn     = h.getHvacPowerOn() == HvacManager.POWER_ON,
                isAcOn        = h.getHvacAcOn() == HvacManager.SW_ON,
                isAutoOn      = h.getHvacAutoOn() == HvacManager.SW_ON,
                isRecircOn    = h.getHvacRecircOn() == 0,  // 0=内循环开，与参考插件一致
                isDualOn      = h.getHvacDualOn() == HvacManager.SW_ON,
                driverTemp    = h.getHvacTemperatureSet(AREA_DRIVER),
                passengerTemp = h.getHvacTemperatureSet(AREA_PASSENGER),
                fanSpeed      = h.getHvacFanSpeed().coerceIn(FAN_MIN, FAN_MAX),
                blowMode      = if (fanDir in 0..3) BLOW_SIGNAL_TO_UI[fanDir] else 0
            )
        } catch (e: Throwable) {
            Log.e(TAG, "读取空调状态失败", e)
            null
        }
    }

    /** 空调状态快照 */
    data class AcSnapshot(
        val isPowerOn: Boolean,
        val isAcOn: Boolean,
        val isAutoOn: Boolean,
        val isRecircOn: Boolean,
        val isDualOn: Boolean,
        val driverTemp: Float,
        val passengerTemp: Float,
        val fanSpeed: Int,
        val blowMode: Int   // 0=吹脸 1=吹脚 2=吹脸+脚 3=吹脚+除霜
    )
}
