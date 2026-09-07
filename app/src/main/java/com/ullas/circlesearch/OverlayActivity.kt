package com.ullas.circlesearch

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * Phase 3 selection overlay + Phase 4 screen capture: Confirm hides the
 * overlay, asks the system MediaProjection consent, captures one frame into
 * app-private storage for the next phase. Consent denial restores the overlay.
 */
class OverlayActivity : Activity() {

    companion object {
        private const val REQUEST_CAPTURE = 1001
        private const val FRAME_WAIT_MS = 2000L
    }

    private lateinit var selectionView: SelectionView
    private lateinit var rootView: View
    private var projection: MediaProjection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay)
        rootView = findViewById(R.id.overlay_root)
        selectionView = findViewById(R.id.selection_view)
        Log.i(Activation.TAG, "overlay opened")
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            cancel()
        }
        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            confirm()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.i(Activation.TAG, "overlay cancelled via back")
    }

    override fun onDestroy() {
        try {
            projection?.stop()
        } catch (e: Exception) {
            Log.w(Activation.TAG, "projection stop failed: $e")
        }
        projection = null
        super.onDestroy()
    }

    private fun cancel() {
        selectionView.clear()
        Log.i(Activation.TAG, "overlay cancelled")
        finish()
    }

    private fun confirm() {
        val bounds = selectionView.selectionBounds()
        if (bounds == null) {
            Toast.makeText(this, "Draw a circle first", Toast.LENGTH_SHORT).show()
            return
        }
        val text = "left=${bounds.left.toInt()} top=${bounds.top.toInt()} " +
            "right=${bounds.right.toInt()} bottom=${bounds.bottom.toInt()}"
        ProbeState.lastSelection = text
        Log.i(Activation.TAG, "selection confirmed: $text")
        // Hide our UI so the capture contains only the underlying app.
        rootView.visibility = View.GONE
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Required: no activity-result library in zero-dependency app")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE) return
        if (resultCode != RESULT_OK || data == null) {
            Log.i(Activation.TAG, "capture consent denied, overlay restored")
            Toast.makeText(this, "Capture declined", Toast.LENGTH_SHORT).show()
            rootView.visibility = View.VISIBLE
            return
        }
        captureOnce(resultCode, data)
    }

    private fun screenSize(): Pair<Int, Int> {
        val manager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            manager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun captureOnce(resultCode: Int, data: Intent) {
        val (width, height) = screenSize()
        val density = resources.displayMetrics.densityDpi
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val reader: ImageReader
        val projection: MediaProjection
        val display: VirtualDisplay
        try {
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            projection = manager.getMediaProjection(resultCode, data)
            this.projection = projection
            display = projection.createVirtualDisplay(
                "CircleSearchCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface, null, null
            )
        } catch (e: Exception) {
            Log.w(Activation.TAG, "capture setup failed: $e")
            Toast.makeText(this, "Capture failed: $e", Toast.LENGTH_LONG).show()
            rootView.visibility = View.VISIBLE
            return
        }
        Thread {
            var saved: File? = null
            try {
                saved = grabFrame(reader, width, height)
            } catch (e: Exception) {
                Log.w(Activation.TAG, "capture failed: $e")
            } finally {
                try {
                    display.release()
                } catch (e: Exception) {
                    Log.w(Activation.TAG, "display release failed: $e")
                }
                try {
                    projection.stop()
                } catch (e: Exception) {
                    Log.w(Activation.TAG, "projection stop failed: $e")
                }
                this.projection = null
                try {
                    reader.close()
                } catch (e: Exception) {
                    Log.w(Activation.TAG, "reader close failed: $e")
                }
            }
            val result = saved
            runOnUiThread {
                if (result != null) {
                    val info = "${result.name} ${width}x$height ${result.length() / 1024}KB"
                    ProbeState.lastCapture = info
                    Log.i(Activation.TAG, "captured: $info")
                    Toast.makeText(this, "Captured: $info", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Capture failed", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }.start()
    }

    private fun grabFrame(reader: ImageReader, width: Int, height: Int): File? {
        val deadline = SystemClock.uptimeMillis() + FRAME_WAIT_MS
        var bitmap: Bitmap? = null
        while (SystemClock.uptimeMillis() < deadline && bitmap == null) {
            val image = reader.acquireLatestImage()
            if (image == null) {
                Thread.sleep(50)
                continue
            }
            image.use { img ->
                val plane = img.planes[0]
                val buffer = plane.buffer
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val paddedWidth = width + rowPadding / plane.pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(buffer)
                bitmap = Bitmap.createBitmap(padded, 0, 0, width, height)
                padded.recycle()
            }
        }
        val bmp = bitmap ?: return null
        val dir = File(filesDir, "captures").apply { mkdirs() }
        val out = File(dir, "capture_${System.currentTimeMillis()}.png")
        FileOutputStream(out).use { stream -> bmp.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        bmp.recycle()
        return out
    }
}
