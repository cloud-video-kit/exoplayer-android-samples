package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils

import android.content.Context
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.R

object ErrorUtils {
    fun findHttpException(error: Throwable?): HttpDataSource.InvalidResponseCodeException? {
        var cause = error
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause
            }
            cause = cause.cause
        }
        return null
    }

    fun getMessageForHttpCode(
        context: Context,
        code: Int
    ): String {
        return when (code) {
            403 -> context.getString(R.string.drm_error_403)
            401 -> context.getString(R.string.drm_error_401)
            404 -> context.getString(R.string.drm_error_404)
            else -> context.getString(R.string.server_error, code)
        }
    }
}