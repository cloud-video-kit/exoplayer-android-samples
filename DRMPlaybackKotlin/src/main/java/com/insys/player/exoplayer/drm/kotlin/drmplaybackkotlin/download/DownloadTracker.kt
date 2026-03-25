package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.download

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionEventListener
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.drm.OfflineLicenseHelper
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.R
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.HttpHeaders.BRAND_GUID
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.HttpHeaders.USER_TOKEN
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ErrorUtils.findHttpException
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ErrorUtils.getMessageForHttpCode
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.extractMediaId
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.viewmodel.ExoPlayerViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.UnknownHostException

private const val GET_DRM_FORMAT_TIMEOUT = 20_000L

class DownloadTracker(
    private val context: Context,
    private val viewModel: ExoPlayerViewModel,
    private val downloadManager: DownloadManager
) {
    private val tag = "DownloadTracker"
    private val _downloadLiveData = MutableLiveData<Download?>()
    val downloadLiveData: LiveData<Download?> = _downloadLiveData

    init {
        downloadManager.addListener(DownloadManagerListener())
    }

    /**
     * Downloads and saves an offline DRM license for a specific video.
     *
     * @param mediaUrl The URL of the media.
     * @param drmLicenseUrl The URL of the DRM license server.
     * @param xDrmBrandGuid The custom header value for the DRM provider's tenant ID.
     * @param xDrmUserToken The user authentication token for the license request.
     * @return The downloaded key set ID as a [ByteArray] on success, or null on failure.
     */
    private suspend fun downloadLicenseFor(
        mediaUrl: String,
        drmLicenseUrl: String,
        xDrmBrandGuid: String,
        xDrmUserToken: String
    ): ByteArray? {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                mapOf(
                    USER_TOKEN to xDrmUserToken,
                    BRAND_GUID to xDrmBrandGuid
                )
            )

        val format = getFirstVideoFormatWithDrm(context, mediaUrl, httpFactory)
        if (format == null) {
            Log.e(tag, "Cannot download format from DrmInitData.")
            return null
        }

        Log.d(tag, "Format from DrmInitData has been found, continue license downloading...")

        val offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(
            drmLicenseUrl,
            httpFactory,
            DrmSessionEventListener.EventDispatcher()
        )

        return try {
            val keySetId = offlineLicenseHelper.downloadLicense(format)
            val mediaId = mediaUrl.extractMediaId()
            Log.d(tag, "DRM license for $mediaId downloaded and saved.")
            keySetId
        } finally {
            offlineLicenseHelper.release()
        }
    }

    /**
     * Initiates the download process for a given media item, starting with download of the DRM
     * license.
     *
     * @param mediaUrl The URL of the media.
     * @param drmLicenseUrl The URL of the DRM license.
     * @param xDrmBrandGuid The custom header value for the DRM provider's tenant ID.
     * @param xDrmUserToken The user authentication token for the license request.
     */
    fun startDownload(
        mediaUrl: String,
        drmLicenseUrl: String,
        xDrmBrandGuid: String,
        xDrmUserToken: String,
        onComplete: (errorMessage: String?) -> Unit
    ) {
        viewModel.viewModelScope.launch {
            try {
                Log.d(tag, "Downloading DRM license...")
                val keySetId =
                    downloadLicenseFor(mediaUrl, drmLicenseUrl, xDrmBrandGuid, xDrmUserToken)

                if (keySetId != null) {
                    val mediaId = mediaUrl.extractMediaId()
                    Log.d(tag, "License downloaded, started video download for: $mediaId")

                    val downloadRequest = DownloadRequest.Builder(mediaId, mediaUrl.toUri())
                        .setKeySetId(keySetId)
                        .build()

                    DownloadService.sendAddDownload(
                        context,
                        ExoPlayerDownloadService::class.java,
                        downloadRequest,
                        false
                    )
                    withContext(Dispatchers.Main) {
                        onComplete(null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(context.getString(R.string.cannot_find_drm_format))
                    }
                }
            } catch (e: HttpDataSource.InvalidResponseCodeException) {
                withContext(Dispatchers.Main) {
                   onComplete(getMessageForHttpCode(context, e.responseCode))
                }
            } catch (e: UnknownHostException) {
                withContext(Dispatchers.Main) {
                    onComplete(context.getString(R.string.invalid_url_or_no_internet_connection))
                }
            } catch (e: Exception) {
                val httpException = findHttpException(e)
                if (httpException != null) {
                    withContext(Dispatchers.Main) {
                        onComplete(getMessageForHttpCode(context, httpException.responseCode))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(context.getString(R.string.unexpected_error, e.localizedMessage))
                    }
                }
            }
        }
    }

    /**
     * Parses the media manifest to extract the Format containing DrmInitData.
     * It does this by creating a temporary ExoPlayer instance that prepares the MediaSource.
     *
     * @param context Application context.
     * @param mediaUrl The URL to the media.
     * @param dataSourceFactory A data source factory that contains custom headers.
     * @return The [Format] object for the first found video track with DRM, or null if an error or
     * timeout occurs.
     */
    private suspend fun getFirstVideoFormatWithDrm(
        context: Context,
        mediaUrl: String,
        dataSourceFactory: DataSource.Factory
    ): Format? {
        return withTimeoutOrNull(GET_DRM_FORMAT_TIMEOUT) {
            val formatDeferred = CompletableDeferred<Format?>()
            withContext(Dispatchers.Main) {
                val drmCallback = HttpMediaDrmCallback(null, dataSourceFactory)

                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        C.WIDEVINE_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(drmCallback)

                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(dataSourceFactory)
                    .setDrmSessionManagerProvider { drmSessionManager }

                val tempPlayer = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()

                val playerListener = object : Player.Listener {
                    override fun onTracksChanged(tracks: com.google.android.exoplayer2.Tracks) {
                        for (trackGroup in tracks.groups) {
                            if (trackGroup.type == C.TRACK_TYPE_VIDEO && trackGroup.isSupported) {
                                for (i in 0 until trackGroup.length) {
                                    val format = trackGroup.getTrackFormat(i)
                                    if (format.drmInitData != null) {
                                        Log.d(
                                            tag,
                                            "Found video format with DrmInitData: $format"
                                        )
                                        Log.d(
                                            tag,
                                            "DrmInitData: ${format.drmInitData.toString()}"
                                        )
                                        if (formatDeferred.isActive) {
                                            formatDeferred.complete(format)
                                        }
                                        return
                                    }
                                }
                            }
                        }
                        Log.w(tag, "TempPlayer::onTracksChanged done, but DrmInitData not found.")
                        if (formatDeferred.isActive) {
                            formatDeferred.complete(null)
                        }
                    }

                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        if (formatDeferred.isActive) {
                            formatDeferred.complete(null)
                        }
                    }
                }

                formatDeferred.invokeOnCompletion {
                    tempPlayer.removeListener(playerListener)
                    tempPlayer.release()
                    Log.d(tag, "Temp player has been released.")
                }

                tempPlayer.addListener(playerListener)
                val mediaItem = MediaItem.fromUri(mediaUrl.toUri())
                tempPlayer.setMediaItem(mediaItem)
                tempPlayer.prepare()
            }
            formatDeferred.await()
        }
    }


    /**
     * Stops and removes the currently tracked download and its license.
     *
     * @param drmLicenseUrl The URL of the DRM license server, required to release the license.
     */
    fun removeDownload(mediaId: String, drmLicenseUrl: String) {
        DownloadService.sendRemoveDownload(
            context,
            ExoPlayerDownloadService::class.java,
            mediaId,
            false
        )
        releaseLicenseFor(mediaId, drmLicenseUrl)
    }

    /**
     * Releases a previously acquired offline DRM license from the DRM server.
     */
    private fun releaseLicenseFor(mediaId: String, drmLicenseUrl: String) {
        viewModel.viewModelScope.launch {
            try {
                val keySetIdToRelease: ByteArray =
                    viewModel.getDownloadedVideoById(mediaId)?.keySetId
                        ?: return@launch

                val offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(
                    drmLicenseUrl,
                    DefaultHttpDataSource.Factory(),
                    DrmSessionEventListener.EventDispatcher()
                )

                offlineLicenseHelper.releaseLicense(keySetIdToRelease)
                Log.d(tag, "License released successfully.")
            } catch (e: Exception) {
                Log.e(tag, "Error during releasing license.", e)
            }
        }
    }

    private inner class DownloadManagerListener : DownloadManager.Listener {
        /**
         * Called when a download's state changes. It updates the tracked `currentDownload.
         */
        override fun onDownloadChanged(
            manager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            _downloadLiveData.postValue(download)
        }

        /**
         * Called when a download is permanently removed from the [DownloadManager].
         */
        override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
            _downloadLiveData.postValue(null)
        }
    }
}

