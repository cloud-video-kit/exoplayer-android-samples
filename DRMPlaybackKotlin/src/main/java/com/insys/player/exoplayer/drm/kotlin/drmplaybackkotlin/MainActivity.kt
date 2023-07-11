package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.databinding.ActivityMainBinding
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.MimeTypes

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {
        // Create a data source factory for HTTP requests
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setTransferListener(DefaultBandwidthMeter.Builder(this)
                .setResetOnNetworkTypeChange(false)
                .build())

        // Create a media source factory for DASH (Dynamic Adaptive Streaming over HTTP)
        val mediaSourceFactory = DashMediaSource.Factory(DefaultDashChunkSource.Factory(dataSourceFactory),
            DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT))

        // Create a media item with the necessary details
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(URL))  // Set the media URL
            .setMimeType(MimeTypes.APPLICATION_MPD)  // Set the MIME type of the media
            .setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(drmSchemeUuid)
                    .setLicenseUri(DRM_LICENSE_URL)  // Set the DRM license server URL
                    .setLicenseRequestHeaders(httpHeaders)  // Set custom DRM request headers
                    .build()
            )
            .build()

        // Create an ExoPlayer instance using the ExoPlayer.Builder
        ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000)  // Set the seek forward increment duration
            .setSeekBackIncrementMs(10000)  // Set the seek backward increment duration
            .build().also { exoPlayer = it }

        // Set the media source, playWhenReady, and prepare the ExoPlayer
        exoPlayer?.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        exoPlayer?.playWhenReady = true
        exoPlayer?.prepare()

        // Set the ExoPlayer instance to the player view
        binding.playerView.player = exoPlayer
    }

    companion object {
        private const val URL =
            "https://dtkya1w875897.cloudfront.net/da6dc30a-e52f-4af2-9751-000b89416a4e/assets/357577a1-3b61-43ae-9af5-82b9727e2f22/videokit-720p-dash-hls-drm/dash/index.mpd"
        private const val DRM_LICENSE_URL =
            "https://insys-marketing.la.drm.cloud/acquire-license/widevine"
        private const val USER_AGENT = "ExoPlayer-Drm"
        private val drmSchemeUuid = C.WIDEVINE_UUID // DRM Type
        private val httpHeaders: Map<String, String> = mutableMapOf(
            "x-drm-brandGuid" to "da6dc30a-e52f-4af2-9751-000b89416a4e",
            "x-drm-userToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE4OTM0NTYwMDAsImRybVRva2VuSW5mbyI6eyJleHAiOiIyMDMwLTAxLTAxVDAwOjAwOjAwKzAwOjAwIiwia2lkIjpbIjFmODNhZTdmLTMwYzgtNGFkMC04MTcxLTI5NjZhMDFiNjU0NyJdLCJwIjp7InBlcnMiOmZhbHNlfX19.hElVqrfK-iLeV_ZleJJO8i-Mf1D2yYVXdtgBE0ja9R4",
        )
    }
}