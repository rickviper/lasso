package com.lasso.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Phase 8 single path for opening visual search on a crop URI: try the
 * Google app's share target directly (no chooser friction), fall back to
 * the system chooser when unresolvable. Fully user-visible at every step —
 * no bypass of any kind.
 */
object ShareHelper {

    fun openSearch(activity: Activity, uriString: String) {
        val uri = Uri.parse(uriString)
        directIntent(activity, uri)?.let { direct ->
            try {
                activity.startActivity(direct)
                Log.i(Activation.TAG, "opened direct search: ${direct.component}")
                return
            } catch (e: Exception) {
                Log.w(Activation.TAG, "direct search failed, falling back: $e")
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, "Visual search with"))
    }

    private fun directIntent(context: Context, uri: Uri): Intent? {
        val targets = try {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_SEND).apply { type = "image/png" },
                PackageManager.MATCH_DEFAULT_ONLY
            )
        } catch (e: Exception) {
            Log.w(Activation.TAG, "share-target query failed: $e")
            return null
        }
        val hit = targets.firstOrNull {
            it.activityInfo.packageName.contains("googlequicksearchbox")
        } ?: targets.firstOrNull {
            it.activityInfo.packageName.startsWith("com.google.") &&
                it.activityInfo.name.contains("lens", ignoreCase = true)
        } ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            component = ComponentName(hit.activityInfo.packageName, hit.activityInfo.name)
        }
    }
}