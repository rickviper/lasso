package com.ullas.circlesearch

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast

/**
 * Phase 3 selection overlay: fullscreen dimmed surface above the foreground
 * app. Draw to select, Cancel to dismiss, Confirm to log the selection
 * bounds (capture/search arrive in later phases).
 */
class OverlayActivity : Activity() {

    private lateinit var selectionView: SelectionView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay)
        selectionView = findViewById(R.id.selection_view)
        Log.i(Activation.TAG, "overlay opened")
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            cancel()
        }
        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            confirm()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.i(Activation.TAG, "overlay cancelled via back")
    }

    private fun cancel() {
        selectionView.clear()
        Log.i(Activation.TAG, "overlay cancelled")
        finish()
    }

    private fun confirm() {
        val bounds = selectionView.selectionBounds()
        if (bounds == null) {
            Toast.makeText(this, "Draw a circle first", Toast.LENGTH_SHORT).show()
            return
        }
        val text = "left=${bounds.left.toInt()} top=${bounds.top.toInt()} " +
            "right=${bounds.right.toInt()} bottom=${bounds.bottom.toInt()}"
        ProbeState.lastSelection = text
        Log.i(Activation.TAG, "selection confirmed: $text")
        Toast.makeText(this, "Selection: $text", Toast.LENGTH_LONG).show()
        finish()
    }
}
