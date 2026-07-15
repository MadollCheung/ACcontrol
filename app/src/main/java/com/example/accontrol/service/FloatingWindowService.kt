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
        val lp = WindowManager.LayoutParams(dp(280), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = iconLp.x; y = iconLp.y + dp(64)
        }
        panelView = LayoutInflater.from(this).inflate(R.layout.floating_ac_panel, null).also {
            wm.addView(it, lp)
            bindPanel(it)
        }
    }

    private fun closePanel() {
        isPanelOpen = false
        panelView?.let { runCatching { wm.removeView(it) }; panelView = null }
    }

    // ── 面板绑定 ──────────────────────────────────────────────
    private fun bindPanel(v: View) {
        v.findViewById<Button>(R.id.btn_close_panel).setOnClickListener { closePanel() }

        val swPower = v.findViewById<Switch>(R.id.switch_ac_power)
        val tvTemp  = v.findViewById<TextView>(R.id.tv_temperature)
        val seekFan = v.findViewById<SeekBar>(R.id.seekbar_fan_speed)
        val tvFan   = v.findViewById<TextView>(R.id.tv_fan_label)
        val rgBlow  = v.findViewById<RadioGroup>(R.id.rg_blow_mode)
        val swCirc  = v.findViewById<Switch>(R.id.switch_circulation)
        val swCool  = v.findViewById<Switch>(R.id.switch_ac_cooling)

        swPower.isChecked = state.isPowerOn
        tvTemp.text = "${state.temperature}°C"
        seekFan.progress = state.fanSpeed
        tvFan.text = fanLabel(state.fanSpeed)
        when (state.blowMode) { 0 -> rgBlow.check(R.id.rb_face); 1 -> rgBlow.check(R.id.rb_feet); 2 -> rgBlow.check(R.id.rb_defrost) }
        swCirc.isChecked = state.isInnerCirculation
        swCool.isChecked = state.isCoolingOn

        swPower.setOnCheckedChangeListener { _, c -> state.isPowerOn = c; ctrl.setPower(c); updateEnabled(v) }

        v.findViewById<Button>(R.id.btn_temp_up).setOnClickListener {
            if (state.temperature < 32) { state.temperature++; tvTemp.text = "${state.temperature}°C"; ctrl.setTemperature(state.temperature) }
        }
        v.findViewById<Button>(R.id.btn_temp_down).setOnClickListener {
            if (state.temperature > 16) { state.temperature--; tvTemp.text = "${state.temperature}°C"; ctrl.setTemperature(state.temperature) }
        }

        seekFan.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) { state.fanSpeed = p; tvFan.text = fanLabel(p) }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { ctrl.setFanSpeed(state.fanSpeed) }
        })

        rgBlow.setOnCheckedChangeListener { _, id ->
            state.blowMode = when(id){ R.id.rb_feet -> 1; R.id.rb_defrost -> 2; else -> 0 }
            ctrl.setBlowMode(state.blowMode)
        }
        swCirc.setOnCheckedChangeListener { _, c -> state.isInnerCirculation = c; ctrl.setCirculation(c) }
        swCool.setOnCheckedChangeListener { _, c -> state.isCoolingOn = c; ctrl.setCooling(c) }

        updateEnabled(v)
    }

    private fun updateEnabled(v: View) {
        val on = state.isPowerOn
        listOf(R.id.btn_temp_up, R.id.btn_temp_down, R.id.seekbar_fan_speed,
            R.id.rg_blow_mode, R.id.switch_circulation, R.id.switch_ac_cooling)
            .forEach { v.findViewById<View>(it).isEnabled = on }
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
