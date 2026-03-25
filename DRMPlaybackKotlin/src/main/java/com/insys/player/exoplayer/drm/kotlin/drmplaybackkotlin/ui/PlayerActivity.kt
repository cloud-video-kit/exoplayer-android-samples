package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.R
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.databinding.ActivityPlayerBinding
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.common.ViewUtils.toast
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.HttpHeaders.BRAND_GUID
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.HttpHeaders.USER_TOKEN
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.DRM_LICENSE_URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.IS_OFFLINE
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.X_DRM_BRAND_GUID
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.X_DRM_USER_TOKEN
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.DownloadUtil
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ErrorUtils
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ErrorUtils.findHttpException
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.extractMediaId
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.viewmodel.ExoPlayerViewModel
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    private val tag = "PlayerActivity"
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var viewModel: ExoPlayerViewModel
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        viewModel = ViewModelProvider(this)[ExoPlayerViewModel::class.java]
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (exoPlayer == null) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (exoPlayer == null) {
            initializePlayer()
        } else {
            exoPlayer?.play()
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {
        val isOffline = intent.getBooleanExtra(IS_OFFLINE, false)
        val mediaUrl = intent.getStringExtra(URL)
        val drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL)

        if (mediaUrl == null || drmLicenseUrl == null) {
            Log.e(tag, "MediaUrl or drmLicenseUrl is null.")
            finish()
            return
        }

        lifecycleScope.launch {
            val mediaId = mediaUrl.extractMediaId()
            val videoFromDb = viewModel.getDownloadedVideoById(mediaId)
            val keySetId = videoFromDb?.keySetId

            if (isOffline && keySetId == null) {
                toast("Error: No license.")
                finish()
                return@launch
            }

            setupPlayer(isOffline, mediaUrl, drmLicenseUrl, keySetId)
        }
    }

    private fun setupPlayer(
        isOffline: Boolean,
        mediaUrl: String,
        drmLicenseUrl: String?,
        keySetId: ByteArray?
    ) {
        val drmSessionManager: DefaultDrmSessionManager
        val drmHttpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val httpMediaDrmCallback = HttpMediaDrmCallback(drmLicenseUrl, drmHttpDataSourceFactory)

        if (isOffline) {
            Log.d(tag, "Playback initialization in offline mode.")
            drmSessionManager = DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(httpMediaDrmCallback)
            drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, keySetId)
        } else {
            Log.d(tag, "Playback initialization in online mode.")
            val xDrmBrandGuid = intent.getStringExtra(X_DRM_BRAND_GUID)
            val xDrmUserToken = intent.getStringExtra(X_DRM_USER_TOKEN)

            if (xDrmBrandGuid != null && xDrmUserToken != null) {
                drmHttpDataSourceFactory.setDefaultRequestProperties(
                    mapOf(
                        BRAND_GUID to xDrmBrandGuid,
                        USER_TOKEN to xDrmUserToken
                    )
                )
            }

            drmSessionManager = DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(httpMediaDrmCallback)
        }

        val cache = DownloadUtil.getDownloadCache(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)
            .setDrmSessionManagerProvider { drmSessionManager }

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    STATE_BUFFERING -> {
                        binding.loadingContainer.visibility = android.view.View.VISIBLE
                    }
                    STATE_READY -> {
                        binding.loadingContainer.visibility = android.view.View.GONE
                    }
                    STATE_ENDED -> {
                        binding.loadingContainer.visibility = android.view.View.GONE
                    }
                    else -> {
                        binding.loadingContainer.visibility = android.view.View.GONE
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                val message = when {
                    error.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                        val rootCause = findHttpException(error)
                        if (rootCause != null) {
                            ErrorUtils.getMessageForHttpCode(
                                this@PlayerActivity,
                                rootCause.responseCode
                            )
                        } else {
                            getString(R.string.drm_error, error.localizedMessage)
                        }
                    }

                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED-> {
                        getString(R.string.invalid_url_or_no_internet_connection)
                    }

                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                            error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ||
                            error.cause is UnrecognizedInputFormatException -> {
                        getString(R.string.format_error)
                    }

                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                            error.cause is HttpDataSource.InvalidResponseCodeException -> {
                        val exception = findHttpException(error)
                        getString(R.string.video_url_error, exception?.responseCode)
                    }

                    else -> getString(R.string.playback_error, error.errorCodeName)
                }
                this@PlayerActivity.toast(message)

                binding.loadingContainer.visibility = android.view.View.GONE
                releasePlayer()
                finish()
            }
        })

        binding.playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(mediaUrl)

        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.playWhenReady = true
        exoPlayer?.prepare()
    }
}