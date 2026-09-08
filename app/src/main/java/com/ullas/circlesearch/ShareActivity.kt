package com.ullas.circlesearch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * Phase 7 single entry point for visual search: fires the system share sheet
 * with the latest crop and finishes. Used by the tap-to-search notification
 * and the dashboard button alike. Cold-start safe: falls back to the newest
 * crop file in private storage when in-memory state is gone.
 */
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriString = ProbeState.lastCropUri ?: latestCropUri()
        if (uriString == null) {
            Toast.makeText(this, "Nothing to search yet", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uriString))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Log.i(Activation.TAG, "sharing crop: $uriString")
        ShareHelper.openSearch(this, uriString)
        finish()
    }

    /**
     * Cold-start fallback: newest app-private crop, served through our own
     * provider (never MediaStore — Gallery must stay clean).
     */
    private fun latestCropUri(): String? {
        return try {
            val dir = File(filesDir, "captures")
            val latest = dir.listFiles()
                ?.filter { it.name.startsWith("crop_") && it.isFile }
                ?.maxByOrNull { it.lastModified() } ?: return null
            val uri = CropProvider.uriFor(latest.name)
            ProbeState.lastCropUri = uri
            uri
        } catch (e: Exception) {
            Log.w(Activation.TAG, "crop lookup failed: $e")
            null
        }
    }
}
