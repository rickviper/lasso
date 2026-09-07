package com.ullas.circlesearch

import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log

/** Phase 2 probe: Quick Settings tile as ACTIVATE trigger. */
class ProbeTile : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        Log.i(GestureService.TAG, "tile added")
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.i(GestureService.TAG, "tile removed")
    }

    override fun onClick() {
        super.onClick()
        ProbeState.tileClicks++
        Log.i(GestureService.TAG, "tile clicked count=${ProbeState.tileClicks}")
        qsTile?.let {
            it.label = "Probe ${ProbeState.tileClicks}"
            it.updateTile()
        }
        startActivityAndCollapse(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
