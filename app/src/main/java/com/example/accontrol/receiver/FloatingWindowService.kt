package com.example.accontrol.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.example.accontrol.R
import com.example.accontrol.controller.AcController
import com.example.accontrol.model.AcState

class FloatingWindowService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var iconView: View
    private var panelView: View? = null
    private var panelLp: WindowManager.LayoutParams? = null
    private var isPanelOpen = false
    private val state = AcState()
    private val ctrl = AcController()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startFg()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addIcon()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::iconView.isInitialized) runCatching { wm.removeView(iconView) }
        panelView?.let { runCatching { wm.removeView(it) } }
    }

    // ── 前台服务 ──────────────────────────────────────────────
    private fun startFg() {
        val ch = "ac_float"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NotificationManager::class.java))
                .createNotificationChannel(NotificationChannel(ch, "空调控制", NotificationManager.IMPORTANCE_LOW))
        }
        startForeground(1, NotificationCompat.Builder(this, ch)
            .setContentTitle("空调控制").setContentText("悬浮面板运行中")
            .setSmallIcon(R.drawable.ic_ac_launcher).build())
    }

    // ── 悬浮图标 ──────────────────────────────────────────────
    private fun addIcon() {
        val lp = iconParams()
        iconView = LayoutInflater.from(this).inflate(R.layout.floating_icon, null)
        wm.addView(iconView, lp)

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var drag = false
        iconView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; drag = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (dx * dx + dy * dy > 25) drag = true
                    lp.x = sx + dx; lp.y = sy + dy
                    wm.updateViewLayout(iconView, lp); true
                }
                MotionEvent.ACTION_UP -> { if (!drag) toggle(lp); true }
                else -> false
            }
        }
    }

    // ── 面板展开/收起 ─────────────────────────────────────────
    private fun toggle(iconLp: WindowManager.LayoutParams) {
        if (isPanelOpen) { closePanel() } else { openPanel(iconLp) }
    }

    private fun openPanel(iconLp: WindowManager.LayoutParams) {
        isPanelOpen = true
        val lp = WindowManager.LayoutParams(dp(320), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = iconLp.x; y = iconLp.y + dp(64)
        }
        panelLp = lp
        panelView = LayoutInflater.from(this).inflate(R.layout.floating_ac_panel, null).also {
            wm.addView(it, lp)
            bindPanel(it)
        }
    }

    private fun closePanel() {
        isPanelOpen = false
        panelView?.let { runCatching { wm.removeView(it) }; panelView = null }
        panelLp = null
    }

    // ── 面板绑定 ──────────────────────────────────────────────
    private fun bindPanel(v: View) {
        v.findViewById<Button>(R.id.btn_close_panel).setOnClickListener { closePanel() }

        // ---- 面板拖动 ----
        val dragHandle = v.findViewById<View>(R.id.panel_drag_handle)
        var psx = 0; var psy = 0; var ptx = 0f; var pty = 0f
        dragHandle.setOnTouchListener { _, e ->
            val lp = panelLp ?: return@setOnTouchListener false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { psx = lp.x; psy = lp.y; ptx = e.rawX; pty = e.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = psx + (e.rawX - ptx).toInt()
                    lp.y = psy + (e.rawY - pty).toInt()
                    panelView?.let { wm.updateViewLayout(it, lp) }; true
                }
                else -> false
            }
        }

        // ---- 电源 ----
        val swPower = v.findViewById<Switch>(R.id.switch_ac_power)
        swPower.isChecked = state.isPowerOn
        swPower.setOnCheckedChangeListener { _, c -> state.isPowerOn = c; ctrl.setPower(c); updateEnabled(v) }

        // ---- 主驾温度 ----
        val tvDriver = v.findViewById<TextView>(R.id.tv_driver_temp)
        tvDriver.text = formatTemp(state.driverTemp, state.isAutoTemp)

        v.findViewById<Button>(R.id.btn_driver_temp_up).setOnClickListener {
            if (state.isAutoTemp) return@setOnClickListener
            if (state.driverTemp < 32) { state.driverTemp++; tvDriver.text = "${state.driverTemp}°C"; ctrl.setDriverTemp(state.driverTemp) }
        }
        v.findViewById<Button>(R.id.btn_driver_temp_down).setOnClickListener {
            if (state.isAutoTemp) return@setOnClickListener
            if (state.driverTemp > 16) { state.driverTemp--; tvDriver.text = "${state.driverTemp}°C"; ctrl.setDriverTemp(state.driverTemp) }
        }

        // ---- 副驾温度 ----
        val tvPass = v.findViewById<TextView>(R.id.tv_pass_temp)
        tvPass.text = formatTemp(state.passengerTemp, state.isAutoTemp)

        v.findViewById<Button>(R.id.btn_pass_temp_up).setOnClickListener {
            if (state.isAutoTemp) return@setOnClickListener
            if (state.passengerTemp < 32) { state.passengerTemp++; tvPass.text = "${state.passengerTemp}°C"; ctrl.setPassengerTemp(state.passengerTemp) }
        }
        v.findViewById<Button>(R.id.btn_pass_temp_down).setOnClickListener {
            if (state.isAutoTemp) return@setOnClickListener
            if (state.passengerTemp > 16) { state.passengerTemp--; tvPass.text = "${state.passengerTemp}°C"; ctrl.setPassengerTemp(state.passengerTemp) }
        }

        // ---- AUTO ----
        val btnAuto = v.findViewById<Button>(R.id.btn_auto_temp)
        updateAutoButton(btnAuto, tvDriver, tvPass)
        btnAuto.setOnClickListener {
            state.isAutoTemp = !state.isAutoTemp
            ctrl.setAutoTemp(state.isAutoTemp)
            updateAutoButton(btnAuto, tvDriver, tvPass)
            updateEnabled(v)
        }

        // ---- 风速 ----
        val seekFan = v.findViewById<SeekBar>(R.id.seekbar_fan_speed)
        val tvFan   = v.findViewById<TextView>(R.id.tv_fan_label)
        seekFan.progress = state.fanSpeed
        tvFan.text = fanLabel(state.fanSpeed)
        seekFan.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) { state.fanSpeed = p; tvFan.text = fanLabel(p) }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { ctrl.setFanSpeed(state.fanSpeed) }
        })

        // ---- 出风模式 ----
        val rgBlow = v.findViewById<RadioGroup>(R.id.rg_blow_mode)
        when (state.blowMode) { 0 -> rgBlow.check(R.id.rb_face); 1 -> rgBlow.check(R.id.rb_feet); 2 -> rgBlow.check(R.id.rb_defrost) }
        rgBlow.setOnCheckedChangeListener { _, id ->
            state.blowMode = when(id){ R.id.rb_feet -> 1; R.id.rb_defrost -> 2; else -> 0 }
            ctrl.setBlowMode(state.blowMode)
        }

        // ---- 内循环 ----
        val swCirc = v.findViewById<Switch>(R.id.switch_circulation)
        swCirc.isChecked = state.isInnerCirculation
        swCirc.setOnCheckedChangeListener { _, c -> state.isInnerCirculation = c; ctrl.setCirculation(c) }

        updateEnabled(v)
    }

    private fun updateAutoButton(btnAuto: Button, tvDriver: TextView, tvPass: TextView) {
        if (state.isAutoTemp) {
            btnAuto.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            btnAuto.setTextColor(0xFFFFFFFF.toInt())
            tvDriver.text = "AUTO"
            tvPass.text = "AUTO"
        } else {
            btnAuto.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFCCCCCC.toInt())
            btnAuto.setTextColor(0xFF333333.toInt())
            tvDriver.text = "${state.driverTemp}°C"
            tvPass.text = "${state.passengerTemp}°C"
        }
    }

    private fun formatTemp(temp: Int, isAuto: Boolean) = if (isAuto) "AUTO" else "${temp}°C"

    private fun updateEnabled(v: View) {
        val on = state.isPowerOn
        val autoOn = state.isAutoTemp
        listOf(R.id.seekbar_fan_speed, R.id.rg_blow_mode, R.id.switch_circulation)
            .forEach { v.findViewById<View>(it).isEnabled = on }
        // 温度加减在 AUTO 模式下禁用
        listOf(R.id.btn_driver_temp_up, R.id.btn_driver_temp_down,
               R.id.btn_pass_temp_up, R.id.btn_pass_temp_down)
            .forEach { v.findViewById<View>(it).isEnabled = on && !autoOn }
        v.findViewById<View>(R.id.btn_auto_temp).isEnabled = on
    }

    // ── 工具 ──────────────────────────────────────────────────
    private fun overlayType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    private fun iconParams() = WindowManager.LayoutParams(dp(56), dp(56), overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.START; x = 20; y = 200
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun fanLabel(n: Int) = when(n){ 0->"自动"; else->"$n 档" }
}