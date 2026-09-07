package com.ullas.circlesearch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.FingerprintGestureController
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 1 fingerprint probe + Phase 2 activation probes (volume keys,
 * accessibility button). All probes observe only and log with tag [TAG].
 */
class GestureService : AccessibilityService() {

    companion object {
        const val TAG = "CircleSearchProbe"
        const val DOUBLE_PRESS_WINDOW_MS = 600L
    }

    private val gestureCallback = object : FingerprintGestureController.FingerprintGestureCallback() {
        override fun onGestureDetectionAvailabilityChanged(available: Boolean) {
            super.onGestureDetectionAvailabilityChanged(available)
            ProbeState.gesturesAvailable = available
            Log.i(TAG, "availability changed: available=$available")
        }

        override fun onGestureDetected(gesture: Int) {
            super.onGestureDetected(gesture)
            val name = gestureName(gesture)
            ProbeState.lastGesture = name
            ProbeState.lastGestureTimeMillis = System.currentTimeMillis()
            ProbeState.gestureCounts[name] = (ProbeState.gestureCounts[name] ?: 0) + 1
            Log.i(TAG, "gesture detected: $name")
        }
    }

    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            super.onClicked(controller)
            ProbeState.a11yButtonClicks++
            Log.i(TAG, "accessibility button clicked count=${ProbeState.a11yButtonClicks}")
        }

        override fun onAvailabilityChanged(controller: AccessibilityButtonController, available: Boolean) {
            super.onAvailabilityChanged(controller, available)
            ProbeState.a11yButtonAvailable = available
            Log.i(TAG, "accessibility button availability: $available")
        }
    }

    private var lastVolumeDownMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        ProbeState.serviceConnected = true
        val controller = fingerprintGestureController
        val available = controller?.isGestureDetectionAvailable ?: false
        ProbeState.gesturesAvailable = available
        Log.i(TAG, "service connected: controllerPresent=${controller != null}, available=$available")
        if (available && controller != null) {
            controller.registerFingerprintGestureCallback(gestureCallback, mainHandler)
            Log.i(TAG, "gesture callback registered")
        }
        val buttonController = accessibilityButtonController
        ProbeState.a11yButtonAvailable = buttonController.isAccessibilityButtonAvailable
        buttonController.registerAccessibilityButtonCallback(buttonCallback, mainHandler)
        Log.i(TAG, "button callback registered, buttonAvailable=${ProbeState.a11yButtonAvailable}")
    }

    /**
     * Phase 2 volume probe: observes volume keys system-wide. Never consumes
     * events (zero interference); detects VOL_DOWN double-press.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val name = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> "VOLUME_DOWN"
            KeyEvent.KEYCODE_VOLUME_UP -> "VOLUME_UP"
            else -> null
        }
        if (name != null) {
            val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
            ProbeState.lastVolumeKey = "$name $action repeat=${event.repeatCount}"
            ProbeState.lastVolumeTimeMillis = System.currentTimeMillis()
            Log.i(TAG, "key event: $name $action repeat=${event.repeatCount}")
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
                event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
            ) {
                val now = System.currentTimeMillis()
                if (now - lastVolumeDownMs <= DOUBLE_PRESS_WINDOW_MS) {
                    ProbeState.volumeDoublePressCount++
                    Log.i(TAG, "volume double-press detected count=${ProbeState.volumeDoublePressCount}")
                }
                lastVolumeDownMs = now
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        ProbeState.serviceConnected = false
        try {
            fingerprintGestureController?.unregisterFingerprintGestureCallback(gestureCallback)
        } catch (e: Exception) {
            Log.w(TAG, "unregister failed: $e")
        }
        try {
            accessibilityButtonController.unregisterAccessibilityButtonCallback(buttonCallback)
        } catch (e: Exception) {
            Log.w(TAG, "button unregister failed: $e")
        }
        Log.i(TAG, "service disconnected")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used: this service exists only for the hardware/gesture probes.
    }

    override fun onInterrupt() {
        // Nothing to interrupt: no event processing or feedback.
    }

    private fun gestureName(gesture: Int): String = when (gesture) {
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT -> "SWIPE_LEFT"
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT -> "SWIPE_RIGHT"
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP -> "SWIPE_UP"
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN -> "SWIPE_DOWN"
        else -> "UNKNOWN($gesture)"
    }
}

/** Latest probe results. Written by probes, read by MainActivity. */
object ProbeState {
    @Volatile var serviceConnected: Boolean = false

    /** Null = service never connected yet, so availability is unknown. */
    @Volatile var gesturesAvailable: Boolean? = null

    @Volatile var lastGesture: String? = null
    @Volatile var lastGestureTimeMillis: Long = 0L
    val gestureCounts: MutableMap<String, Int> = mutableMapOf()

    @Volatile var lastVolumeKey: String? = null
    @Volatile var lastVolumeTimeMillis: Long = 0L
    @Volatile var volumeDoublePressCount: Int = 0

    @Volatile var a11yButtonAvailable: Boolean? = null
    @Volatile var a11yButtonClicks: Int = 0

    @Volatile var tileClicks: Int = 0

    @Volatile var overlayShown: Boolean = false
    @Volatile var overlayTaps: Int = 0
}
