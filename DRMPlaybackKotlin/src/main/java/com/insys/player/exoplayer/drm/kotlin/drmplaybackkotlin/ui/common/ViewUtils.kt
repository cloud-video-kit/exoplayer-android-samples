package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.common

import android.content.Context
import android.widget.Toast

object ViewUtils {
    /**
     * Extension function to display a Toast message easily from any Context.
     */
    fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}