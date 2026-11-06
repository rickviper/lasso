package com.lasso.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.io.File
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
        findViewById<Button>(R.id.btn_share).setOnClickListener {
            shareCrop()
        }
        requestNotificationPermission()
    }

    /**
     * Phase 7: tap-to-search is a content notification, which on Android 13+
     * requires the runtime POST_NOTIFICATIONS grant (the FGS exemption covers
     * only the service's own notice). Without it the notification silently
     * never appears. Dashboard sharing works regardless.
     */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            android.util.Log.i(Activation.TAG, "notification permission granted=$granted")
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
        out.appendLine("Last capture: ${ProbeState.lastCapture ?: "(none yet)"}")
        out.appendLine("Last crop: ${ProbeState.lastCrop ?: "(none yet)"}")
        // On-disk truth: survives process death, unlike in-memory counters.
        val capturesDir = File(filesDir, "captures").apply { mkdirs() }
        val latest = capturesDir.listFiles()?.maxByOrNull { it.lastModified() }
        out.appendLine("Files on disk: ${latest?.let { "${it.name} ${it.length() / 1024}KB" } ?: "(none)"}")
        statusView.text = out.toString().trimEnd()
    }

    /**
     * Phase 7: dashboard path to the same [ShareActivity] the tap-to-search
     * notification uses. Single chooser-firing path, no duplication.
     */
    private fun shareCrop() {
        startActivity(Intent(this, ShareActivity::class.java))
    }
}