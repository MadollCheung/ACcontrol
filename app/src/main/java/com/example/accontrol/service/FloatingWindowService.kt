package com.example.accontrol.service

import android.app.*
import android.content.Intent
import android.content.res.ColorStateList
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

    // 温度范围：16~32°C，SeekBar max=16，progress 0=32°C，16=16°C
    private fun progressToTemp(p: Int) = 32 - p
    private fun tempToProgress(t: Int) = 32 - t

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

    private fun startFg() {
        val ch = "ac_float"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NotificationManager::class.java))
                .createNotificationChannel(
                    NotificationChannel(ch, "AC Control", NotificationManager.IMPORTANCE_LOW)
                )
        }
        startForeground(
            1, NotificationCompat.Builder(this, ch)
                .setContentTitle("AC Control")
                .setContentText("Floating panel running")
                .setSmallIcon(R.drawable.ic_ac_launcher)
                .build()
        )
    }

    private fun addIcon() {
        val lp = iconParams()
        iconView = LayoutInflater.from(this).inflate(R.layout.floating_icon, null)
        wm.addView(iconView, lp)
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var drag = false
        iconView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; drag = false; true
                }
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

    private fun toggle(iconLp: WindowManager.LayoutParams) {
        if (isPanelOpen) closePanel() else openPanel(iconLp)
    }

    private fun openPanel(iconLp: WindowManager.LayoutParams) {
        isPanelOpen = true
        val lp = WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
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

    private fun bindPanel(v: View) {

        // ── 关闭按钮 ──────────────────────────────────────────
        v.findViewById<Button>(R.id.btn_close_panel).setOnClickListener { closePanel() }

        // ── 标题栏拖动 ────────────────────────────────────────
        val dragHandle = v.findViewById<View>(R.id.panel_drag_handle)
        var psx = 0; var psy = 0; var ptx = 0f; var pty = 0f
        dragHandle.setOnTouchListener { _, e ->
            val lp = panelLp ?: return@setOnTouchListener false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    psx = lp.x; psy = lp.y; ptx = e.rawX; pty = e.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = psx + (e.rawX - ptx).toInt()
                    lp.y = psy + (e.rawY - pty).toInt()
                    panelView?.let { wm.updateViewLayout(it, lp) }; true
                }
                else -> false
            }
        }

        // ── 电源开关 ──────────────────────────────────────────
        val swPower = v.findViewById<Switch>(R.id.switch_ac_power)
        swPower.isChecked = state.isPowerOn
        swPower.setOnCheckedChangeListener { _, c ->
            state.isPowerOn = c
            ctrl.setPower(c)
            updateEnabled(v)
        }

        // ── 主驾温度 SeekBar（竖向，rotation=270） ───────────
        val tvDriver = v.findViewById<TextView>(R.id.tv_driver_temp)
        val sbDriver = v.findViewById<SeekBar>(R.id.seekbar_driver_temp)
        tvDriver.text = formatTemp(state.driverTemp, state.isAutoTemp)
        sbDriver.progress = tempToProgress(state.driverTemp)
        sbDriver.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, byUser: Boolean) {
                if (!byUser || state.isAutoTemp) return
                state.driverTemp = progressToTemp(p)
                tvDriver.text = "${state.driverTemp}°C"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                if (!state.isAutoTemp) ctrl.setDriverTemp(state.driverTemp)
            }
        })

        // ── 副驾温度 SeekBar ──────────────────────────────────
        val tvPass = v.findViewById<TextView>(R.id.tv_pass_temp)
        val sbPass = v.findViewById<SeekBar>(R.id.seekbar_pass_temp)
        tvPass.text = formatTemp(state.passengerTemp, state.isAutoTemp)
        sbPass.progress = tempToProgress(state.passengerTemp)
        sbPass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, byUser: Boolean) {
                if (!byUser || state.isAutoTemp) return
                state.passengerTemp = progressToTemp(p)
                tvPass.text = "${state.passengerTemp}°C"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                if (!state.isAutoTemp) ctrl.setPassengerTemp(state.passengerTemp)
            }
        })

        // ── AUTO 按钮 ─────────────────────────────────────────
        val btnAuto = v.findViewById<Button>(R.id.btn_auto_temp)
        updateAutoBtn(btnAuto, tvDriver, tvPass, sbDriver, sbPass)
        btnAuto.setOnClickListener {
            state.isAutoTemp = !state.isAutoTemp
            ctrl.setAutoTemp(state.isAutoTemp)
            updateAutoBtn(btnAuto, tvDriver, tvPass, sbDriver, sbPass)
            updateEnabled(v)
        }

        // ── 风速 SeekBar ──────────────────────────────────────
        val seekFan = v.findViewById<SeekBar>(R.id.seekbar_fan_speed)
        val tvFan   = v.findViewById<TextView>(R.id.tv_fan_label)
        seekFan.progress = state.fanSpeed
        tvFan.text = fanLabel(state.fanSpeed)
        seekFan.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                state.fanSpeed = p
                tvFan.text = fanLabel(p)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { ctrl.setFanSpeed(state.fanSpeed) }
        })

        // ── 出风模式 ──────────────────────────────────────────
        val rgBlow = v.findViewById<RadioGroup>(R.id.rg_blow_mode)
        when (state.blowMode) {
            0 -> rgBlow.check(R.id.rb_face)
            1 -> rgBlow.check(R.id.rb_feet)
            2 -> rgBlow.check(R.id.rb_defrost)
        }
        rgBlow.setOnCheckedChangeListener { _, id ->
            state.blowMode = when (id) { R.id.rb_feet -> 1; R.id.rb_defrost -> 2; else -> 0 }
            ctrl.setBlowMode(state.blowMode)
        }

        // ── 内循环切换按钮 ────────────────────────────────────
        val btnCirc = v.findViewById<Button>(R.id.btn_circulation)
        updateCircBtn(btnCirc)
        btnCirc.setOnClickListener {
            state.isInnerCirculation = !state.isInnerCirculation
            ctrl.setCirculation(state.isInnerCirculation)
            updateCircBtn(btnCirc)
        }

        updateEnabled(v)
    }

    // 内循环按钮颜色/文字随状态切换
    private fun updateCircBtn(btn: Button) {
        if (state.isInnerCirculation) {
            btn.text = "内循环"
            btn.backgroundTintList = ColorStateList.valueOf(0xFF2196F3.toInt())
            btn.setTextColor(0xFFFFFFFF.toInt())
        } else {
            btn.text = "外循环"
            btn.backgroundTintList = ColorStateList.valueOf(0xFFCCCCCC.toInt())
            btn.setTextColor(0xFF333333.toInt())
        }
    }

    private fun updateAutoBtn(
        btn: Button, tvD: TextView, tvP: TextView,
        sbD: SeekBar, sbP: SeekBar
    ) {
        if (state.isAutoTemp) {
            btn.backgroundTintList = ColorStateList.valueOf(0xFF2196F3.toInt())
            btn.setTextColor(0xFFFFFFFF.toInt())
            tvD.text = "AUTO"; tvP.text = "AUTO"
            sbD.isEnabled = false; sbP.isEnabled = false
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(0xFFCCCCCC.toInt())
            btn.setTextColor(0xFF333333.toInt())
            tvD.text = "${state.driverTemp}°C"
            tvP.text = "${state.passengerTemp}°C"
            sbD.isEnabled = true; sbP.isEnabled = true
        }
    }

    private fun formatTemp(t: Int, auto: Boolean) = if (auto) "AUTO" else "${t}°C"

    // 风速：0 显示 "自动"，否则显示数字
    private fun fanLabel(n: Int) = if (n == 0) "自动" else n.toString()

    private fun updateEnabled(v: View) {
        val on   = state.isPowerOn
        val auto = state.isAutoTemp
        listOf(R.id.seekbar_fan_speed, R.id.rg_blow_mode, R.id.btn_circulation, R.id.btn_auto_temp)
            .forEach { id -> v.findViewById<View>(id).isEnabled = on }
        listOf(R.id.seekbar_driver_temp, R.id.seekbar_pass_temp)
            .forEach { id -> v.findViewById<View>(id).isEnabled = on && !auto }
    }

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun iconParams() = WindowManager.LayoutParams(
        dp(56), dp(56), overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START; x = 20; y = 200
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
