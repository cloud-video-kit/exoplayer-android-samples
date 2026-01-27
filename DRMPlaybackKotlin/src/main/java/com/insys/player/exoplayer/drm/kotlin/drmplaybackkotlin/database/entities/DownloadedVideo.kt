package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.Database.DATABASE_TABLE_NAME

/**
 * Represents a video that has been downloaded and stored locally.
 *
 * @property videoId Unique identifier for the video.
 * @property mediaUrl The source URL of the downloaded video file.
 * @property drmLicenseUrl The URL of the license server used to acquire the DRM keys.
 * @property keySetId The stored offline license keys.
 */
@Entity(tableName = DATABASE_TABLE_NAME)
data class DownloadedVideo(
    @PrimaryKey val videoId: String,
    val mediaUrl: String,
    val drmLicenseUrl: String,
    val keySetId: ByteArray
)