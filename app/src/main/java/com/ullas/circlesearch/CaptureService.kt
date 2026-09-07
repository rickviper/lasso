package com.ullas.circlesearch

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import android.util.Log
import android.widget.Toast
import java.io.File
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
        private const val FRAME_WAIT_MS = 2000L
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
        try {
            val saved = grabFrame(reader, width, height)
            if (saved != null) {
                val info = "${saved.name} ${width}x$height ${saved.length() / 1024}KB"
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
