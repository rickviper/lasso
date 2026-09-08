package com.ullas.circlesearch

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.ceil

/**
 * Phase 4 capture worker. Android requires `getMediaProjection` (targetSdk
 * 29+) to run inside a foreground service of type `mediaProjection`, so the
 * overlay hands the consent result here for a single one-frame capture.
 */
class CaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_DENSITY = "density"
        private const val CHANNEL_ID = "capture_service"
        private const val NOTIF_ID = 42
        private const val CHANNEL_SEARCH_ID = "search_ready"
        private const val SEARCH_NOTIF_ID = 43
        private const val FRAME_WAIT_MS = 4000L
        private const val SETTLE_MS = 600L
        private const val SKIP_FRAMES = 2
        /** Padding added on each side of the selection bounds (configurable). */
        private const val PADDING_DP = 32
        private const val MIN_CROP_PX = 16
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val captureLock = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!captureLock.compareAndSet(false, true)) {
            Log.w(Activation.TAG, "capture already in progress, ignoring duplicate start")
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        Thread {
            try {
                capture(intent)
            } catch (e: Exception) {
                Log.w(Activation.TAG, "capture failed: $e")
                ProbeState.lastCapture = "FAILED (error)"
                toast("Capture failed", long = false)
            } finally {
                captureLock.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Screen capture", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Circle Search capture")
            .setContentText("Capturing screen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun capture(intent: Intent?) {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        val width = intent?.getIntExtra(EXTRA_WIDTH, 0) ?: 0
        val height = intent?.getIntExtra(EXTRA_HEIGHT, 0) ?: 0
        val density = intent?.getIntExtra(EXTRA_DENSITY, 0) ?: 0
        if (resultCode != Activity.RESULT_OK || data == null || width <= 0 || height <= 0) {
            Log.w(Activation.TAG, "capture called with invalid consent result")
            return
        }
        // Ground truth: the physical display's current size/rotation. The
        // mirror buffer MUST match it; overlay-window metrics can disagree
        // (rotation, decor), which yields partial/slivered frames.
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val physical = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        val rotation = physical?.rotation
        val (capW, capH) = physicalSize(physical) ?: (width to height)
        Log.i(Activation.TAG, "display rotation=$rotation real=${capW}x$capH requested=${width}x$height")
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val reader = ImageReader.newInstance(capW, capH, PixelFormat.RGBA_8888, 2)
        val projection = manager.getMediaProjection(resultCode, data)
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(Activation.TAG, "projection stopped")
            }
        }, mainHandler)
        val display = projection.createVirtualDisplay(
            "CircleSearchCapture", capW, capH, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )
        // Let the consent-dialog exit + task transitions settle: grabbing the
        // very first frames risks transition/placeholder (black) buffers.
        Thread.sleep(SETTLE_MS)
        try {
            val saved = grabFrame(reader, capW, capH, density)
            if (saved != null) {
                val galleryUri = savePublicCopy(saved)
                ProbeState.lastCaptureUri = galleryUri
                val info = "${saved.name} ${capW}x$capH ${saved.length() / 1024}KB" +
                    if (galleryUri != null) " + gallery" else ""
                ProbeState.lastCapture = info
                Log.i(Activation.TAG, "captured: $info")
                toast("Captured: $info", long = true)
            } else {
                ProbeState.lastCapture = "FAILED (no frame)"
                ProbeState.lastCrop = "(no capture)"
                toast("Capture failed (no frame)", long = false)
            }
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
            try {
                reader.close()
            } catch (e: Exception) {
                Log.w(Activation.TAG, "reader close failed: $e")
            }
        }
    }

    private fun grabFrame(reader: ImageReader, width: Int, height: Int, densityDpi: Int): File? {
        val deadline = SystemClock.uptimeMillis() + FRAME_WAIT_MS
        var skipped = 0
        var seen = 0
        var best: Bitmap? = null
        var bestBlank = true
        while (SystemClock.uptimeMillis() < deadline && bestBlank) {
            val image = reader.acquireLatestImage()
            if (image == null) {
                Thread.sleep(100)
                continue
            }
            seen++
            val bmp = image.use { img -> imageToBitmap(img, width, height) }
            if (bmp == null) continue
            if (skipped < SKIP_FRAMES) {
                skipped++
                bmp.recycle()
                continue
            }
            if (!isBlank(bmp)) {
                best?.recycle()
                best = bmp
                bestBlank = false
            } else {
                // Blank frame: keep as fallback, keep waiting for a real one.
                best?.recycle()
                best = bmp
                Thread.sleep(100)
            }
        }
        Log.i(Activation.TAG, "frames seen=$seen skipped=$skipped usedBlank=$bestBlank")
        val bmp = best ?: return null
        cropSelection(bmp, densityDpi)
        val dir = File(filesDir, "captures").apply { mkdirs() }
        val out = File(dir, "capture_${System.currentTimeMillis()}.png")
        FileOutputStream(out).use { stream -> bmp.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        bmp.recycle()
        return out
    }

    private fun imageToBitmap(
        image: android.media.Image,
        width: Int,
        height: Int
    ): Bitmap? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowPadding = plane.rowStride - plane.pixelStride * width
        if (rowPadding < 0) return null
        val paddedWidth = width + rowPadding / plane.pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
        padded.recycle()
        return cropped
    }

    /** True if every sampled pixel is pure black (placeholder/fence frame). */
    private fun isBlank(bmp: Bitmap): Boolean {
        val stepX = maxOf(1, bmp.width / 8)
        val stepY = maxOf(1, bmp.height / 8)
        var y = 0
        while (y < bmp.height) {
            var x = 0
            while (x < bmp.width) {
                if ((bmp.getPixel(x, y) and 0x00FFFFFF) != 0) return false
                x += stepX
            }
            y += stepY
        }
        return true
    }

    /**
     * Phase 5: crop the selection (+ padding, clamped to the frame) and retain
     * it in memory for the next phase. Never throws: invalid or tiny regions
     * are skipped with a log. Debug copies land next to the captures.
     */
    private fun cropSelection(full: Bitmap, densityDpi: Int) {
        val bounds = ProbeState.selectionBounds
        if (bounds == null || bounds.isEmpty) {
            Log.i(Activation.TAG, "crop skipped: no selection bounds")
            ProbeState.lastCrop = "skipped (no bounds)"
            CropHolder.replace(null)
            return
        }
        try {
            val padPx = PADDING_DP * densityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()
            val left = (bounds.left - padPx).toInt().coerceIn(0, full.width)
            val top = (bounds.top - padPx).toInt().coerceIn(0, full.height)
            val right = ceil(bounds.right + padPx).toInt().coerceIn(0, full.width)
            val bottom = ceil(bounds.bottom + padPx).toInt().coerceIn(0, full.height)
            val cw = right - left
            val ch = bottom - top
            if (cw < MIN_CROP_PX || ch < MIN_CROP_PX) {
                Log.i(Activation.TAG, "crop skipped: clamped region too small ${cw}x$ch")
                ProbeState.lastCrop = "skipped (too small ${cw}x$ch)"
                CropHolder.replace(null)
                return
            }
            val cropped = Bitmap.createBitmap(full, left, top, cw, ch)
            CropHolder.replace(cropped)
            val dir = File(filesDir, "captures").apply { mkdirs() }
            val out = File(dir, "crop_${System.currentTimeMillis()}.png")
            FileOutputStream(out).use { stream ->
                cropped.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val galleryUri = savePublicCopy(out)
            ProbeState.lastCropUri = galleryUri
            val info = "${out.name} ${cw}x$ch" + if (galleryUri != null) " + gallery" else ""
            ProbeState.lastCrop = info
            Log.i(Activation.TAG, "cropped: $info from bounds $bounds")
            if (galleryUri != null) notifySearch()
        } catch (e: Exception) {
            Log.w(Activation.TAG, "crop failed: $e")
            ProbeState.lastCrop = "skipped (error)"
            CropHolder.replace(null)
        }
    }

    /**
     * Physical display size in the CURRENT rotation, or null if unavailable
     * (caller falls back to the overlay's requested size).
     */
    private fun physicalSize(display: android.view.Display?): Pair<Int, Int>? {
        if (display == null) return null
        return try {
            @Suppress("DEPRECATION")
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
            if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                metrics.widthPixels to metrics.heightPixels
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(Activation.TAG, "physical size failed: $e")
            null
        }
    }

    /**
     * Public copy in Pictures/CircleSearch so the capture is visible in the
     * gallery and file managers, and shareable via MediaStore URI (no
     * FileProvider/extra dependency needed). API 29+: no permission needed
     * for our own files. Returns the content URI string, or null on failure.
     * The app-private PNG remains the next phase's source of truth.
     */
    private fun savePublicCopy(source: File): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(Activation.TAG, "public copy skipped below API 29")
            return null
        }
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, source.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CircleSearch")
            }
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null
            contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(source).use { input -> input.copyTo(out) }
            } ?: return null
            Log.i(Activation.TAG, "gallery copy saved: $uri")
            uri.toString()
        } catch (e: Exception) {
            Log.w(Activation.TAG, "gallery copy failed: $e")
            null
        }
    }

    /**
     * Phase 7 tap-to-search: posted while still foreground (no extra
     * permission path needed). Tap opens [ShareActivity]; auto-cancels.
     */
    private fun notifySearch() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SEARCH_ID, "Visual search", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val search = PendingIntent.getActivity(
            this, 0, Intent(this, ShareActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_SEARCH_ID)
            .setContentTitle("Circle Search ready")
            .setContentText("Tap to search the circled image")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(search)
            .setAutoCancel(true)
            .build()
        manager.notify(SEARCH_NOTIF_ID, notification)
        Log.i(Activation.TAG, "search notification posted")
    }

    private fun toast(text: String, long: Boolean) {
        mainHandler.post {
            try {
                Toast.makeText(
                    this, text,
                    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.w(Activation.TAG, "toast failed: $e")
            }
        }
    }
}
