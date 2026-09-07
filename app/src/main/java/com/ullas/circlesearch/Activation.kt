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
        Toast.makeText(context, "Circle Search activated ($source)", Toast.LENGTH_SHORT).show()
    }
}
