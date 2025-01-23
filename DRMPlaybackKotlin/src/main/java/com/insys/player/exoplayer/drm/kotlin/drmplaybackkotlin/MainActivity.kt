package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin

// Dodaj te importy
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.MimeTypes
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null

    // Cache and database variables
    private lateinit var cache: SimpleCache
    private lateinit var databaseProvider: DatabaseProvider
    private lateinit var downloadManager: DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCache()
        setupDownloadManager()

        // Check and handle media download
        checkAndDownloadMedia()
    }

    private fun setupCache() {
        databaseProvider = ExoDatabaseProvider(this)
        val cacheDir = File(this.cacheDir, "media")
        cache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(100L * 1024 * 1024), // 100MB cache size
            databaseProvider
        )
    }

    private fun setupDownloadManager() {
        downloadManager = DownloadManager(
            this,
            databaseProvider,
            cache,
            DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT)
        )
    }

    private fun checkAndDownloadMedia() {
        val uri = Uri.parse(URL)

        // Check if media is cached
        if (isMediaCached(cache, uri)) {
            initializePlayer()
        } else {
            // Media not cached, initiate download
            showMessage("Media not cached. Starting download.")
            downloadMedia(uri)
        }
    }

    private fun isMediaCached(simpleCache: SimpleCache, uri: Uri): Boolean {
//        val cacheKey = CacheKeyFactory.DEFAULT.buildCacheKey(DataSpec(uri))
        val cacheKey = CacheKeyFactory.DEFAULT.buildCacheKey(DataSpec(uri))
        val cachedSpans = simpleCache.getCachedSpans(cacheKey)

        var cachedLength = 0L
        for (span in cachedSpans) {
            cachedLength += span.length
        }

        val contentLength = simpleCache.getContentMetadata(cacheKey)
            .get(ContentMetadata.KEY_CONTENT_LENGTH, C.LENGTH_UNSET.toLong())

        return  cachedLength > 0
//        return contentLength > 0 && cachedLength == contentLength
    }

    private fun downloadMedia(uri: Uri) {
        val uri2 = Uri.parse(URL)
        if (uri2.scheme == null || uri2.host == null) {
            throw IllegalArgumentException("Invalid URI: $uri2")
        }

        val mediaId = uri2.lastPathSegment ?: uri2.toString()

        val downloadRequest = DownloadRequest.Builder(mediaId, uri2).build()
        downloadManager.addDownload(downloadRequest)
        downloadManager.resumeDownloads()

        val handler = Handler(Looper.getMainLooper())
        val updateProgressRunnable = object : Runnable {
            override fun run() {
                val downloads = downloadManager.currentDownloads
                for (download in downloads) {
                    if (download.request.id == mediaId && download.state == Download.STATE_DOWNLOADING) {
                        val percentDownloaded = download.percentDownloaded
                        if (percentDownloaded != C.PERCENTAGE_UNSET.toFloat()) {
                            showMessage("Download progress: ${percentDownloaded.toInt()}%")
                        }
                    }
                }
                handler.postDelayed(this, 5000) // Update every 5 seconds
            }
        }
        handler.post(updateProgressRunnable)

        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                when (download.state) {
                    Download.STATE_COMPLETED -> {
                        if (download.request.id == mediaId) {
                            showMessage("Download progress: 100%")
                            showMessage("Download completed for media: $mediaId")
                            initializePlayer()
                            handler.removeCallbacks(updateProgressRunnable) // Stop updates
                        }
                    }
                    Download.STATE_FAILED -> {
                        if (download.request.id == mediaId) {
                            showError("Download failed for media: $mediaId")
                            handler.removeCallbacks(updateProgressRunnable) // Stop updates
                        }
                    }
                }
            }
        })
    }

    private fun initializePlayer() {
        val uri = Uri.parse(URL)

        if (!isMediaCached(cache, uri)) {
            showError("Media not available offline.")
            return
        }

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT))
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))

        ///// Offline POC
        val drmSessionManager: DefaultDrmSessionManager
        val uuid = C.WIDEVINE_UUID
        val mediaDrm = FrameworkMediaDrm.DEFAULT_PROVIDER //.newInstance(uuid)
        val drmCallback = HttpMediaDrmCallback(DRM_LICENSE_URL, DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT))
        Log.e("dsds","dsds")
        drmSessionManager = DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(uuid, mediaDrm)
            .build(drmCallback)

        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_DOWNLOAD, null)
        /////

        val mediaSource: MediaSource = DashMediaSource.Factory(
            DefaultDashChunkSource.Factory(cacheDataSourceFactory),
            cacheDataSourceFactory
        )
            .setDrmSessionManagerProvider { drmSessionManager } // offline POC
            .createMediaSource(MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(drmSchemeUuid)
                    .setLicenseUri(DRM_LICENSE_URL)
                    .setLicenseRequestHeaders(httpHeaders)
//                    .setKeySetId()
                    .build()
            )
            .build()
        )

        exoPlayer = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.playWhenReady = true
        exoPlayer?.prepare()

        binding.playerView.player = exoPlayer
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onStart() {
        super.onStart()
        val uri = Uri.parse(URL)
        if (isMediaCached(cache, uri)) {
            initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val URL =
            "https://dtkya1w875897.cloudfront.net/da6dc30a-e52f-4af2-9751-000b89416a4e/assets/357577a1-3b61-43ae-9af5-82b9727e2f22/videokit-720p-dash-hls-drm/dash/index.mpd"
        private const val DRM_LICENSE_URL =
            "https://insys-marketing.la.drm.cloud/acquire-license/widevine?brandguid=da6dc30a-e52f-4af2-9751-000b89416a4e&usertoken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE4OTM0NTYwMDAsImRybVRva2VuSW5mbyI6eyJleHAiOiIyMDMwLTAxLTAxVDAwOjAwOjAwKzAwOjAwIiwia2lkIjpbIjFmODNhZTdmLTMwYzgtNGFkMC04MTcxLTI5NjZhMDFiNjU0NyJdLCJwIjp7InBlcnMiOmZhbHNlfX19.hElVqrfK-iLeV_ZleJJO8i-Mf1D2yYVXdtgBE0ja9R4"
        private const val USER_AGENT = "ExoPlayer-Drm"
        private val drmSchemeUuid = C.WIDEVINE_UUID // DRM Type
        private val httpHeaders: Map<String, String> = mutableMapOf(
            "x-drm-brandGuid" to "da6dc30a-e52f-4af2-9751-000b89416a4e",
            "x-drm-userToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE4OTM0NTYwMDAsImRybVRva2VuSW5mbyI6eyJleHAiOiIyMDMwLTAxLTAxVDAwOjAwOjAwKzAwOjAwIiwia2lkIjpbIjFmODNhZTdmLTMwYzgtNGFkMC04MTcxLTI5NjZhMDFiNjU0NyJdLCJwIjp7InBlcnMiOmZhbHNlfX19.hElVqrfK-iLeV_ZleJJO8i-Mf1D2yYVXdtgBE0ja9R4",
        )
    }
}