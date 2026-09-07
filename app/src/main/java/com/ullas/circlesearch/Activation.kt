package com.ullas.circlesearch

import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * Phase 2 centralized activation function. The Quick Settings tile is the
 * sole trigger (accessibility shortcut was dropped: MIUI does not rebind
 * the service after the app process is killed).
 *
 * Phase 2 scope: log + count + a best-effort Toast. No overlay, capture,
 * cropping, or search here.
 */
object Activation {

    const val TAG = "CircleSearchProbe"
    const val SOURCE_TILE = "quick-settings-tile"

    fun trigger(context: Context, source: String) {
        ProbeState.activationCounts[source] = (ProbeState.activationCounts[source] ?: 0) + 1
        ProbeState.lastActivationSource = source
        ProbeState.lastActivationTimeMillis = System.currentTimeMillis()
        Log.i(TAG, "activated via $source count=${ProbeState.activationCounts[source]}")
        // Toast is best-effort feedback only: on some MIUI builds showing a
        // toast from a background process throws. Counts + log are the
        // ground truth; never let feedback crash us.
        try {
            Toast.makeText(context, "Circle Search activated ($source)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w(TAG, "toast failed (suppressed on this build): $e")
        }
    }
}

/** Latest activation results. Written only by [Activation]. */
object ProbeState {
    val activationCounts: MutableMap<String, Int> = mutableMapOf()
    @Volatile var lastActivationSource: String? = null
    @Volatile var lastActivationTimeMillis: Long = 0L
    @Volatile var lastSelection: String? = null
    @Volatile var selectionBounds: android.graphics.RectF? = null
    @Volatile var lastCapture: String? = null
    @Volatile var lastCrop: String? = null
}

/**
 * Phase 5 in-memory crop for the next phase. Single slot: replacing the
 * crop recycles the previous bitmap. Survives nothing — process death
 * drops it (files on disk remain).
 */
object CropHolder {
    @Volatile var cropped: android.graphics.Bitmap? = null
        private set

    @Synchronized
    fun replace(next: android.graphics.Bitmap?) {
        cropped?.recycle()
        cropped = next
    }
}
