package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.data.repository

import androidx.lifecycle.LiveData
import com.google.android.exoplayer2.offline.Download
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.dao.VideoDao
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities.DownloadedVideo
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.download.DownloadTracker
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.extractMediaId
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val videoDao: VideoDao,
    private val downloadTracker: DownloadTracker
) {
    /**
     * Emits a list of all [DownloadedVideo] records from the database.
     */
    val allDownloadedVideos: Flow<List<DownloadedVideo>> = videoDao.getAllVideos()

    /**
     * LiveData representing the current state of a download process.
     * Observed by the UI to track progress or status changes.
     */
    val downloadState: LiveData<Download?> = downloadTracker.downloadLiveData

    /**
     * Initiates a new media download.
     *
     * @param mediaUrl The source URL of the video.
     * @param drmLicenseUrl The URL of the DRM license server.
     * @param brandGuid Custom header for DRM identification.
     * @param token User authentication token for DRM.
     * @param onComplete Callback invoked when the download initiation finishes.
     */
    fun startDownload(
        mediaUrl: String,
        drmLicenseUrl: String,
        brandGuid: String,
        token: String,
        onComplete: (String?) -> Unit
    ) {
        downloadTracker.startDownload(mediaUrl, drmLicenseUrl, brandGuid, token, onComplete)
    }

    /**
     * Creates and saves a [DownloadedVideo] record into the local database.
     *
     * @param mediaUrl The URL of the video (used to extract the media ID).
     * @param drmLicenseUrl The DRM license server URL used for this download.
     * @param keySetId The offline license key set identifier.
     */
    suspend fun saveVideo(mediaUrl: String, drmLicenseUrl: String, keySetId: ByteArray) {
        val videoToSave = DownloadedVideo(
            videoId = mediaUrl.extractMediaId(),
            mediaUrl = mediaUrl,
            drmLicenseUrl = drmLicenseUrl,
            keySetId = keySetId
        )
        videoDao.insertOrUpdate(videoToSave)
    }

    /**
     * Removes the video from both the [DownloadTracker] and the [VideoDao] .
     *
     * @param video The [DownloadedVideo] object to be deleted.
     */
    suspend fun deleteVideo(video: DownloadedVideo) {
        downloadTracker.removeDownload(video.videoId, video.drmLicenseUrl)
        videoDao.delete(video)
    }

    /**
     * Directly removes a download from the [DownloadTracker].
     */
    fun removeDownload(mediaId: String, drmLicenseUrl: String) =
        downloadTracker.removeDownload(mediaId, drmLicenseUrl)

    /**
     * Searches for a specific video in the database by its unique identifier.
     *
     * @param videoId The unique ID of the video.
     * @return The found [DownloadedVideo] or null if not found.
     */
    suspend fun getVideoById(videoId: String): DownloadedVideo? =
        videoDao.getVideoById(videoId)
}