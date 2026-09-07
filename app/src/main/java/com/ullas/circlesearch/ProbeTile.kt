package com.ullas.circlesearch

import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log

/** Phase 2 trigger: the Quick Settings tile calls [Activation]. */
class ProbeTile : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        Log.i(Activation.TAG, "tile added")
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.i(Activation.TAG, "tile removed")
    }

    override fun onClick() {
        super.onClick()
        Activation.trigger(this, Activation.SOURCE_TILE)
        // Collapse QS into the dashboard so the activation is visible.
        startActivityAndCollapse(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
