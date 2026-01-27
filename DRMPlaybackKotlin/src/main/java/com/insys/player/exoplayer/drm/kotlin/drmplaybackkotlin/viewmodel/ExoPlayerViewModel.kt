package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.offline.Download
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.data.repository.VideoRepository
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.AppDatabase
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities.DownloadedVideo
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.download.DownloadTracker
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.DownloadUtil
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.extractMediaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExoPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ExoPlayerViewModel"
    private val downloadManager = DownloadUtil.getDownloadManager(application)
    private val downloadTracker = DownloadTracker(application, this, downloadManager)
    private val videoDao = AppDatabase.getDatabase(application).videoDao()

    private val repository = VideoRepository(videoDao, downloadTracker)

    /**
     * Observable download state for the UI.
     */
    val downloadState: LiveData<Download?> = repository.downloadState

    /**
     * Flow of downloaded videos to be displayed in the UI list.
     */
    val allDownloadedVideos: Flow<List<DownloadedVideo>> = repository.allDownloadedVideos

    /**
     * Starts the download process for a DRM-protected stream.
     */
    fun startDownloadForMedia(
        mediaUrl: String,
        drmLicenseUrl: String,
        xDrmBrandGuid: String,
        xDrmUserToken: String,
        onComplete: (success: Boolean) -> Unit
    ) {
        repository.startDownload(mediaUrl, drmLicenseUrl, xDrmBrandGuid, xDrmUserToken, onComplete)
    }

    /**
     * Deletes a video record and its physical files.
     */
    fun deleteVideo(video: DownloadedVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVideo(video)
            Log.d(tag, "Video with ID: ${video.videoId} has been removed.")
        }
    }

    /**
     * Deletes download from the DownloadManager.
     */
    fun removeDownload(mediaId: String, drmLicenseUrl: String) =
        repository.removeDownload(mediaId, drmLicenseUrl)

    /**
     * Saves information about a successful download to the database.
     */
    fun saveDownloadedVideo(
        mediaUrl: String,
        drmLicenseUrl: String,
        keySetId: ByteArray
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveVideo(mediaUrl, drmLicenseUrl, keySetId)
            Log.d(
                tag,
                "Video with id: ${mediaUrl.extractMediaId()} has been saved in database."
            )
        }
    }

    /**
     * Helper method to fetch a single video's data from the repository.
     */
    suspend fun getDownloadedVideoById(videoId: String): DownloadedVideo? =
        repository.getVideoById(videoId)
}
