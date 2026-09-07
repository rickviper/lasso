package com.ullas.circlesearch

import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * Phase 2 centralized activation function. Both triggers — the
 * accessibility shortcut (primary) and the Quick Settings tile
 * (secondary) — must call [trigger] and nothing else activates.
 *
 * Phase 2 scope: log + count + a minimal Toast so activation is visible
 * even when another app is in the foreground. No overlay, capture,
 * cropping, or search here.
 */
object Activation {

    const val SOURCE_SHORTCUT = "accessibility-shortcut"
    const val SOURCE_TILE = "quick-settings-tile"

    fun trigger(context: Context, source: String) {
        ProbeState.activationCounts[source] = (ProbeState.activationCounts[source] ?: 0) + 1
        ProbeState.lastActivationSource = source
        ProbeState.lastActivationTimeMillis = System.currentTimeMillis()
        Log.i(GestureService.TAG, "activated via $source count=${ProbeState.activationCounts[source]}")
        // Toast is best-effort feedback only: on some MIUI builds showing a
        // toast from a background process throws and would kill the shared
        // app process (taking the accessibility service down with it).
        // Counts + log are the ground truth; never let feedback crash us.
        try {
            Toast.makeText(context, "Circle Search activated ($source)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w(GestureService.TAG, "toast failed (suppressed on this build): $e")
        }
    }
}
