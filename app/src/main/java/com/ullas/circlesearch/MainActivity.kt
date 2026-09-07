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

/** Phase 2 screen: accessibility-service state and centralized activations. */
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
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val out = StringBuilder()
        out.appendLine("Service enabled: ${isServiceEnabled()}")
        val shortcut = ProbeState.activationCounts[Activation.SOURCE_SHORTCUT] ?: 0
        val tile = ProbeState.activationCounts[Activation.SOURCE_TILE] ?: 0
        out.appendLine("Activations via shortcut: $shortcut")
        out.appendLine("Activations via tile: $tile")
        val last = ProbeState.lastActivationSource?.let { source ->
            "$source at ${timeFormat.format(Date(ProbeState.lastActivationTimeMillis))}"
        } ?: "(none yet)"
        out.appendLine("Last activation: $last")
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
