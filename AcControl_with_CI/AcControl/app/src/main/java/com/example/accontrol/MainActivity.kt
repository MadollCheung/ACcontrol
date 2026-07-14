package com.example.accontrol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.accontrol.service.FloatingWindowService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateStatus()

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (Settings.canDrawOverlays(this)) startFloating()
            else requestPermission()
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, FloatingWindowService::class.java))
            Toast.makeText(this, "悬浮面板已停止", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        if (Settings.canDrawOverlays(this)) startFloating()
    }

    private fun startFloating() {
        startForegroundService(Intent(this, FloatingWindowService::class.java))
        Toast.makeText(this, "空调悬浮面板已启动", Toast.LENGTH_SHORT).show()
    }

    private fun requestPermission() {
        Toast.makeText(this, "请授予「显示在其他应用上层」权限", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")))
    }

    private fun updateStatus() {
        findViewById<TextView>(R.id.tv_status).text =
            if (Settings.canDrawOverlays(this)) getString(R.string.permission_granted)
            else getString(R.string.permission_denied)
    }
}
