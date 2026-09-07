package com.ullas.circlesearch

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
        private const val FRAME_WAIT_MS = 4000L
        private const val SETTLE_MS = 600L
        private const val SKIP_FRAMES = 2
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        Thread {
            try {
                capture(intent)
            } catch (e: Exception) {
                Log.w(Activation.TAG, "capture failed: $e")
                toast("Capture failed", long = false)
            } finally {
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
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val projection = manager.getMediaProjection(resultCode, data)
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(Activation.TAG, "projection stopped")
            }
        }, mainHandler)
        val display = projection.createVirtualDisplay(
            "CircleSearchCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )
        // Let the consent-dialog exit + task transitions settle: grabbing the
        // very first frames risks transition/placeholder (black) buffers.
        Thread.sleep(SETTLE_MS)
        try {
            val saved = grabFrame(reader, width, height)
            if (saved != null) {
                val gallery = savePublicCopy(saved)
                val info = "${saved.name} ${width}x$height ${saved.length() / 1024}KB" +
                    if (gallery) " + gallery" else ""
                ProbeState.lastCapture = info
                Log.i(Activation.TAG, "captured: $info")
                toast("Captured: $info", long = true)
            } else {
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

    private fun grabFrame(reader: ImageReader, width: Int, height: Int): File? {
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
     * Public copy in Pictures/CircleSearch so the capture is visible in the
     * gallery and file managers. API 29+: no permission needed for our own
     * files. The app-private PNG remains the next phase's source of truth.
     */
    private fun savePublicCopy(source: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(Activation.TAG, "public copy skipped below API 29")
            return false
        }
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, source.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CircleSearch")
            }
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return false
            contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(source).use { input -> input.copyTo(out) }
            } ?: return false
            Log.i(Activation.TAG, "gallery copy saved: $uri")
            true
        } catch (e: Exception) {
            Log.w(Activation.TAG, "gallery copy failed: $e")
            false
        }
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
