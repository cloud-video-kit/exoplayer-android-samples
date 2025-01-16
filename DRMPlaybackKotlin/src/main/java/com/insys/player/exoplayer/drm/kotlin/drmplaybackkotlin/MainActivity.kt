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
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaDrm
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback

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

        // Create a DRM session manager
        val mediaDrm = FrameworkMediaDrm.newInstance(drmSchemeUuid)
        val drmCallback = HttpMediaDrmCallback(DRM_LICENSE_URL, dataSourceFactory)
        httpHeaders.forEach { (key, value) ->
            drmCallback.setKeyRequestProperty(key, value)
        }
        val drmSessionManager = DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(drmSchemeUuid) { mediaDrm }
            .setMultiSession(true)
            .build(drmCallback)

        // Create a media source factory for DASH (Dynamic Adaptive Streaming over HTTP)
        val mediaSourceFactory = DashMediaSource.Factory(DefaultDashChunkSource.Factory(dataSourceFactory),
            DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT))
            .setDrmSessionManagerProvider { drmSessionManager }

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
             "https://dh7xcm7fqixq8.cloudfront.net/cf06bdcb-2db5-429f-8e7e-7b3e5d2742d7/assets/a63aa0d4-9f66-44e4-8130-94f9d5175367/videokit-576p-dash-hls-drm/dash/index.mpd"
         private const val DRM_LICENSE_URL =
             "https://qa-lab-tenant1.la-drm.lab.cloud.insysvt.com/acquire-license/widevine"
         private const val USER_AGENT = "ExoPlayer-Drm"
         private val drmSchemeUuid = C.WIDEVINE_UUID // DRM Type
         private val httpHeaders: Map<String, String> = mutableMapOf(
             "x-drm-brandGuid" to "cf06bdcb-2db5-429f-8e7e-7b3e5d2742d7",
             "x-drm-userToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NDU2NjI5NjcsImRybVRva2VuSW5mbyI6eyJleHAiOiIyMDI2LTAxLTA4VDA5OjQ0OjUwLjAwMDI0MCIsImtpZCI6WyIqIl0sInAiOnsicGVycyI6dHJ1ZSwiZXhjIjp7IldpZGV2aW5lQ2FuUmVuZXciOnRydWUsIldpZGV2aW5lTGljZW5zZUR1cmF0aW9uU2Vjb25kcyI6NjAsIldpZGV2aW5lUmVudGFsRHVyYXRpb25TZWNvbmRzIjo2MCwiV2lkZXZpbmVQbGF5YmFja0R1cmF0aW9uU2Vjb25kcyI6NjAsIldpZGV2aW5lUmVuZXdhbERlbGF5U2Vjb25kcyI6MTB9fX19.W-Em0Xq35JO5p0yrMxrTGfceQFCAMzjIfRLarpsqFaw"
         )
     }
}