package com.ullas.circlesearch

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast

/**
 * Phase 3 selection overlay + Phase 4 capture entry: Confirm hides the
 * overlay and takes the system MediaProjection consent; the grant is handed
 * to [CaptureService] (a `mediaProjection` foreground service, as Android
 * requires). Denial restores the overlay.
 */
class OverlayActivity : Activity() {

    companion object {
        private const val REQUEST_CAPTURE = 1001
        private const val RESULT_TIMEOUT_MS = 12_000L
    }

    private lateinit var selectionView: SelectionView
    private lateinit var rootView: View
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var resultDelivered = false
    private val timeoutRunnable = Runnable {
        Log.i(Activation.TAG, "result wait timed out, closing overlay")
        finishQuietly()
    }

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
        // Prevent double-tap spawning duplicate consent + capture flows.
        findViewById<Button>(R.id.btn_confirm).isEnabled = false
        val text = "left=${bounds.left.toInt()} top=${bounds.top.toInt()} " +
            "right=${bounds.right.toInt()} bottom=${bounds.bottom.toInt()}"
        ProbeState.lastSelection = text
        ProbeState.selectionBounds = android.graphics.RectF(bounds)
        Log.i(Activation.TAG, "selection confirmed: $text")
        // Overlay stays visible behind the system consent dialog; hidden
        // only once the grant arrives (cleaner than hide-then-restore).
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
            Log.i(Activation.TAG, "capture consent denied, continuing overlay")
            Toast.makeText(this, "Capture declined", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.btn_confirm).isEnabled = true
            return
        }
        // Hide our UI now so the capture contains only the underlying app.
        rootView.visibility = View.GONE
        val (width, height) = screenSize()
        val service = Intent(this, CaptureService::class.java)
            .putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(CaptureService.EXTRA_DATA, data)
            .putExtra(CaptureService.EXTRA_WIDTH, width)
            .putExtra(CaptureService.EXTRA_HEIGHT, height)
            .putExtra(CaptureService.EXTRA_DENSITY, resources.displayMetrics.densityDpi)
        startForegroundService(service)
        // Stay alive (invisible) until the capture resolves, then auto-open
        // search — no taps needed after Start now. Timeout guards silence
        // (service toasts/notifications still cover feedback).
        waitForResult()
    }

    private fun waitForResult() {
        CaptureService.resultListener = { uri ->
            if (!resultDelivered) {
                resultDelivered = true
                mainHandler.removeCallbacks(timeoutRunnable)
                if (uri != null) {
                    try {
                        ShareHelper.openSearch(this, uri)
                    } catch (e: Exception) {
                        Log.w(Activation.TAG, "auto-open failed: $e")
                    }
                }
                finish()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, RESULT_TIMEOUT_MS)
    }

    private fun finishQuietly() {
        resultDelivered = true
        finish()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(timeoutRunnable)
        if (!resultDelivered) {
            CaptureService.resultListener = null
        }
        super.onDestroy()
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
}
