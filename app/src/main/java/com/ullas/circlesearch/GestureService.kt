package com.ullas.circlesearch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.FingerprintGestureController
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 2 trigger service: keeps the fingerprint-availability readout (Phase 1
 * verdict) and routes accessibility-shortcut taps to [Activation].
 * Processes no accessibility events and observes no hardware keys.
 */
class GestureService : AccessibilityService() {

    companion object {
        const val TAG = "CircleSearchProbe"
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
            Activation.trigger(this@GestureService, Activation.SOURCE_SHORTCUT)
        }

        override fun onAvailabilityChanged(controller: AccessibilityButtonController, available: Boolean) {
            super.onAvailabilityChanged(controller, available)
            // Informational only: on this MIUI build clicks arrive even when
            // the API reports unavailable, so clicks are the ground truth.
            Log.i(TAG, "accessibility button availability: $available")
        }
    }

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
        buttonController.registerAccessibilityButtonCallback(buttonCallback, mainHandler)
        Log.i(TAG, "button callback registered")
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

    /** Centralized activation results, written only by [Activation]. */
    val activationCounts: MutableMap<String, Int> = mutableMapOf()
    @Volatile var lastActivationSource: String? = null
    @Volatile var lastActivationTimeMillis: Long = 0L
}
