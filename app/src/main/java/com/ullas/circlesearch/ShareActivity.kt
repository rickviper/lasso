package com.ullas.circlesearch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

/**
 * Phase 7 single entry point for visual search: fires the system share sheet
 * with the latest crop and finishes. Used by the tap-to-search notification
 * and the dashboard button alike. Cold-start safe: falls back to the newest
 * crop row in MediaStore when in-memory state is gone.
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

    private fun latestCropUri(): String? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("crop_%.png")
            val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args, sort
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()
                    ).toString()
                    ProbeState.lastCropUri = uri
                    uri
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(Activation.TAG, "crop lookup failed: $e")
            null
        }
    }
}
