package com.ullas.circlesearch

import android.app.Activity
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
     * Phase 6 PoC: hand the crop to the system share sheet so the tester can
     * open it in Lens (or any visual-search target). User-driven, no API,
     * no auth, no bypass. Production path (Phase 7) wraps the same intent.
     */
    private fun shareCrop() {
        val uriString = ProbeState.lastCropUri
        if (uriString == null) {
            android.widget.Toast.makeText(this, "No crop with gallery copy yet", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = android.net.Uri.parse(uriString)
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        android.util.Log.i(Activation.TAG, "sharing crop: $uriString")
        startActivity(android.content.Intent.createChooser(send, "Visual search with"))
    }
}
