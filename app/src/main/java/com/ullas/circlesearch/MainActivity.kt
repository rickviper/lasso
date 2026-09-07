package com.ullas.circlesearch

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Phase 2 screen: Quick Settings tile activation counts. */
class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusView = findViewById(R.id.probe_status)
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
        val tile = ProbeState.activationCounts[Activation.SOURCE_TILE] ?: 0
        out.appendLine("Activations via tile: $tile")
        val last = ProbeState.lastActivationSource?.let { source ->
            "$source at ${timeFormat.format(Date(ProbeState.lastActivationTimeMillis))}"
        } ?: "(none yet)"
        out.appendLine("Last activation: $last")
        out.appendLine("Last selection: ${ProbeState.lastSelection ?: "(none yet)"}")
        statusView.text = out.toString().trimEnd()
    }
}
