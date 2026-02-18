package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils

fun String.extractMediaId(): String {
    return try {
        val segments = this.split("/")
        val assetsIndex = segments.indexOf("assets")
        if (assetsIndex != -1 && assetsIndex + 1 < segments.size) {
            segments[assetsIndex + 1]
        } else {
            val fallback = this.substringBeforeLast("/dash/").substringAfterLast("/")
            if (fallback.contains("http") || fallback.isEmpty()) this.hashCode()
                .toString() else fallback
        }
    } catch (e: Exception) {
        this.hashCode().toString()
    }
}