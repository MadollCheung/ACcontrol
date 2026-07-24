package com.example.accontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.accontrol.service.FloatingWindowService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btn_start_service)
        val btnStop = findViewById<Button>(R.id.btn_stop_service)
        val btnAccessibility = findViewById<Button>(R.id.btn_enable_accessibility)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        btnStart.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "请先开启辅助功能权限", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            startService(Intent(this, FloatingWindowService::class.java))
            tvStatus.text = "状态：悬浮面板已启动"
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, FloatingWindowService::class.java))
            tvStatus.text = "状态：已停止"
        }

        btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    override fun onResume() {
        super.onResume()
        val tvStatus = findViewById<TextView>(R.id.tv_status)
        val accessible = isAccessibilityEnabled()
        val overlay = Settings.canDrawOverlays(this)
        tvStatus.text = buildString {
            append("辅助功能：${if (accessible) "✅ 已开启" else "❌ 未开启"}\n")
            append("悬浮窗权限：${if (overlay) "✅ 已开启" else "❌ 未开启"}\n")
            append(if (accessible && overlay) "就绪，可启动面板" else "请开启上述权限")
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return services.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "请找到「空调控制辅助服务」并开启", Toast.LENGTH_LONG).show()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivity(intent)
    }
}
