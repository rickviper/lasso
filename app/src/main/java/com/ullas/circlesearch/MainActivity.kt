package com.ullas.circlesearch

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Phase 2 probe screen: shows all activation-probe states. */
class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusView = findViewById(R.id.probe_status)
        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            refresh()
        }
        findViewById<Button>(R.id.btn_show_dot).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, OverlayService::class.java))
            } else {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val out = StringBuilder()
        val enabled = isServiceEnabled()
        out.appendLine("Service enabled: $enabled")

        val availability = ProbeState.gesturesAvailable
        out.appendLine(
            "Fingerprint gestures: " + when {
                !enabled && availability == null -> "UNKNOWN (enable the service first)"
                availability == null -> "UNKNOWN (service not connected yet)"
                availability -> "AVAILABLE"
                else -> "NOT AVAILABLE on this device"
            }
        )

        val lastVol = ProbeState.lastVolumeKey?.let { key ->
            "$key at ${timeFormat.format(Date(ProbeState.lastVolumeTimeMillis))}"
        } ?: "(none yet)"
        out.appendLine("Volume key: $lastVol")
        out.appendLine("VOL_DOWN double-press: ${ProbeState.volumeDoublePressCount}")

        val btnAvail = when (ProbeState.a11yButtonAvailable) {
            null -> "UNKNOWN"
            true -> "AVAILABLE"
            false -> "NOT AVAILABLE"
        }
        out.appendLine("A11y button: $btnAvail, clicks=${ProbeState.a11yButtonClicks}")
        out.appendLine("QS tile taps: ${ProbeState.tileClicks}")
        out.appendLine(
            "Overlay: permitted=${Settings.canDrawOverlays(this)}, " +
                "shown=${ProbeState.overlayShown}, taps=${ProbeState.overlayTaps}"
        )
        statusView.text = out.toString().trimEnd()
    }

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, GestureService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
