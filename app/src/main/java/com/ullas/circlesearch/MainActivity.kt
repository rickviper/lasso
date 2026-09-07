package com.ullas.circlesearch

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Phase 1 probe screen: shows GestureService status and detected gestures. */
class MainActivity : Activity() {

    private lateinit var statusView: TextView

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
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val enabled = isServiceEnabled()
        val availability = ProbeState.gesturesAvailable
        val availabilityText = when {
            !enabled && availability == null -> "UNKNOWN (enable the service first)"
            availability == null -> "UNKNOWN (service not connected yet)"
            availability -> "AVAILABLE"
            else -> "NOT AVAILABLE on this device"
        }
        val counts = listOf("SWIPE_LEFT", "SWIPE_RIGHT", "SWIPE_UP", "SWIPE_DOWN")
            .joinToString("\n") { name -> "  $name: ${ProbeState.gestureCounts[name] ?: 0}" }
        val last = ProbeState.lastGesture?.let { name ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ProbeState.lastGestureTimeMillis))
            "$name at $time"
        } ?: "(none yet)"
        statusView.text = getString(
            R.string.probe_status_template, enabled, availabilityText, last, counts
        )
    }

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, GestureService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
