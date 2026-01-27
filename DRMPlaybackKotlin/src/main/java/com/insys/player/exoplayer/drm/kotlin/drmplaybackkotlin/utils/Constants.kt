package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils

object Constants {
    const val CLOUD_DRM_URL = "https://www.drm.cloud/"

    object IntentExtra {
        const val URL = "URL"
        const val DRM_LICENSE_URL = "DRM_LICENSE_URL"
        const val X_DRM_BRAND_GUID = "X_DRM_BRAND_GUID"
        const val X_DRM_USER_TOKEN = "X_DRM_USER_TOKEN"
        const val IS_OFFLINE = "IS_OFFLINE"
    }

    object HttpHeaders {
        const val BRAND_GUID = "x-drm-brandGuid"
        const val USER_TOKEN = "x-drm-userToken"
    }

    object Database {
        const val DATABASE_NAME = "video_database"
        const val DATABASE_TABLE_NAME = "downloaded_videos"
    }
}