package com.ullas.circlesearch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView

/** Phase 2 probe: floating overlay dot as ACTIVATE trigger. */
class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "probe_overlay"
        const val NOTIF_ID = 41
    }

    private var dot: ImageView? = null
    private var windowManager: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            Log.w(GestureService.TAG, "overlay permission missing, stopping")
            stopSelf()
            return
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Probe trigger", NotificationManager.IMPORTANCE_MIN)
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Circle Search probe")
            .setContentText("Floating trigger active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openApp)
            .build()
        startForeground(NOTIF_ID, notification)

        val size = (64 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_trigger_dot)
            contentDescription = getString(R.string.overlay_dot_description)
        }
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 240
        }
        view.setOnClickListener {
            ProbeState.overlayTaps++
            Log.i(GestureService.TAG, "overlay tapped count=${ProbeState.overlayTaps}")
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(view, params)
        dot = view
        ProbeState.overlayShown = true
        Log.i(GestureService.TAG, "overlay shown")
    }

    override fun onDestroy() {
        dot?.let { windowManager?.removeView(it) }
        dot = null
        ProbeState.overlayShown = false
        Log.i(GestureService.TAG, "overlay hidden")
        super.onDestroy()
    }
}
