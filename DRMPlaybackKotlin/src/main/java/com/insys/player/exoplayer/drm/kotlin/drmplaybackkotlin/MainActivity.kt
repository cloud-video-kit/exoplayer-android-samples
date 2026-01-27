package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.offline.Download
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.databinding.ActivityMainBinding
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.PlayerActivity
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.common.ViewUtils.toast
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.downloads.DownloadsActivity
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.CLOUD_DRM_URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.DRM_LICENSE_URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.IS_OFFLINE
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.X_DRM_BRAND_GUID
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.X_DRM_USER_TOKEN
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.PermissionManager
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ValidationUtils.validateUrl
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ValidationUtils.validateXDrmBrandGuid
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.ValidationUtils.validateXDrmUserToken
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.extractMediaId
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.viewmodel.ExoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ExoPlayerViewModel
    private lateinit var permissionManager: PermissionManager
    private lateinit var progressBar: ProgressBar
    private lateinit var downloadButton: Button
    private lateinit var downloadStatusContainer: View
    private lateinit var statusTextView: TextView
    private var currentDownload: Download? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        downloadButton = binding.btnDownload
        progressBar = binding.progressBar
        downloadStatusContainer = binding.downloadStatusContainer
        statusTextView = binding.tvDownloadStatus

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ExoPlayerViewModel::class.java]
        permissionManager = PermissionManager(this)

        fillFormWithDefaultData()
        setupUI()
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModel.downloadState.observe(this) { download ->
            currentDownload = download
            updateUI(download)
        }
    }

    private fun setupUI() {
        binding.btnPlay.setOnClickListener {
            if (!isOnline(this)) {
                toast("No internet connection.")
            } else {
                if (validateForm(this)) {
                    startPlayerActivity()
                }
            }
        }

        binding.btnGoToDownloads.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
        }

        downloadButton.setOnClickListener {
            if (!isOnline(this)) {
                toast("No internet connection.")
            } else {
                permissionManager.checkNotificationPermission(
                    onGranted = {
                        if (validateForm(this)) {
                            startDownload()
                        }
                    },
                    onDenied = {
                        toast("Download progress will not be shown in notifications.")
                        if (validateForm(this)) {
                            startDownload()
                        }
                    }
                )

            }
        }

        binding.btnCloudDrmLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = CLOUD_DRM_URL.toUri()
            startActivity(intent)
        }
    }

    private fun startDownload() {
        val mediaUrl = binding.etUrl.text.toString()
        val mediaId = mediaUrl.extractMediaId()
        if (mediaId.isEmpty()) {
            toast("Cannot start download - empty MediaId.")
            return
        }

        lifecycleScope.launch {
            if (viewModel.getDownloadedVideoById(mediaId) != null) {
                toast("Video with this id has already been downloaded.")
            } else {
                val downloadState = currentDownload?.state
                when (downloadState) {
                    null, Download.STATE_FAILED, Download.STATE_COMPLETED -> {
                        viewModel.startDownloadForMedia(
                            mediaUrl,
                            binding.etDrmLicenseUrl.text.toString(),
                            binding.etXDrmBrandGuid.text.toString(),
                            binding.etXDrmUserToken.text.toString(),
                            onComplete = { success ->
                                if (success) {
                                    toast("License has been downloaded. Starting video download...")
                                } else {
                                    toast("Error while downloading license.")
                                }
                            }
                        )
                    }

                    Download.STATE_DOWNLOADING -> {
                        viewModel.removeDownload(
                            mediaUrl.extractMediaId(),
                            binding.etDrmLicenseUrl.text.toString()
                        )
                        toast("Current download cancelled.")
                    }

                    else -> {
                        toast("Tried to download video, but cannot.")
                    }
                }
            }
        }
    }

    private fun validateForm(context: Context): Boolean {
        val isMediaUrlValid = validateUrl(context, binding.etUrl.text.toString(), binding.etUrl)
        val isDrmLicenseUrlValid =
            validateUrl(context, binding.etDrmLicenseUrl.text.toString(), binding.etDrmLicenseUrl)
        val isXBrandGuidValid = validateXDrmBrandGuid(
            context,
            binding.etXDrmBrandGuid.text.toString(),
            binding.etXDrmBrandGuid
        )
        val isXDrmUserTokenValid = validateXDrmUserToken(
            context,
            binding.etXDrmUserToken.text.toString(),
            binding.etXDrmUserToken
        )

        return isMediaUrlValid && isDrmLicenseUrlValid && isXBrandGuidValid && isXDrmUserTokenValid
    }

    private fun updateUI(download: Download?) {
        if (download == null) {
            binding.btnDownload.text = getString(R.string.download)
            binding.btnDownload.isEnabled = true
            binding.btnPlay.isEnabled = true
            downloadStatusContainer.visibility = View.GONE
            setInputsEnabled(true)
            return
        }

        when (download.state) {
            Download.STATE_REMOVING -> {
                binding.btnDownload.text = getString(R.string.download)
                binding.btnDownload.isEnabled = false
                binding.btnPlay.isEnabled = false
                downloadStatusContainer.visibility = View.VISIBLE
                statusTextView.text = getString(R.string.removing)
                progressBar.visibility = View.GONE
                setInputsEnabled(false)
            }

            Download.STATE_DOWNLOADING -> {
                binding.btnDownload.text = getString(R.string.cancel)
                binding.btnDownload.isEnabled = true
                binding.btnPlay.isEnabled = false
                downloadStatusContainer.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                progressBar.isIndeterminate = true
                statusTextView.text = getString(R.string.downloading)
                setInputsEnabled(false)
            }

            Download.STATE_COMPLETED -> {
                binding.btnDownload.text = getString(R.string.download)
                binding.btnDownload.isEnabled = true
                binding.btnPlay.isEnabled = true
                statusTextView.text = getString(R.string.download_completed)

                lifecycleScope.launch {
                    // To let the user see the download completed message for a while.
                    delay(2000)
                    downloadStatusContainer.visibility = View.GONE
                    progressBar.visibility = View.GONE

                    val mediaId = download.request.id
                    val mediaUrl = download.request.uri.toString()
                    val keySetId = download.request.keySetId
                    val drmLicenseUrl = binding.etDrmLicenseUrl.text.toString()
                    if (keySetId != null) {
                        viewModel.saveDownloadedVideo(mediaUrl, drmLicenseUrl, keySetId)
                        toast("Download completed. Video with id: $mediaId has been saved.")
                    } else {
                        viewModel.removeDownload(mediaUrl.extractMediaId(), drmLicenseUrl)
                        toast("Download completed. But video cannot be saved, because keySetId is null.")
                    }
                    progressBar.isIndeterminate = false
                    setInputsEnabled(true)
                }
            }

            Download.STATE_QUEUED,
            Download.STATE_RESTARTING -> {
                binding.btnDownload.text = getString(R.string.download)
                binding.btnDownload.isEnabled = true
                binding.btnPlay.isEnabled = true
                downloadStatusContainer.visibility = View.VISIBLE
                statusTextView.text = getString(R.string.waiting)
                progressBar.isIndeterminate = true
                setInputsEnabled(false)
            }

            Download.STATE_STOPPED -> {
                binding.btnDownload.text = getString(R.string.download)
                binding.btnDownload.isEnabled = true
                binding.btnPlay.isEnabled = true
                downloadStatusContainer.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                statusTextView.text = getString(R.string.download_stop)
                setInputsEnabled(true)
            }

            Download.STATE_FAILED -> {
                binding.btnDownload.text = getString(R.string.download)
                binding.btnDownload.isEnabled = true
                binding.btnPlay.isEnabled = true
                downloadStatusContainer.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                statusTextView.text = getString(R.string.download_error)
                setInputsEnabled(true)
            }
        }
    }

    private fun startPlayerActivity() {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(URL, binding.etUrl.text.toString())
        intent.putExtra(DRM_LICENSE_URL, binding.etDrmLicenseUrl.text.toString())
        intent.putExtra(X_DRM_BRAND_GUID, binding.etXDrmBrandGuid.text.toString())
        intent.putExtra(X_DRM_USER_TOKEN, binding.etXDrmUserToken.text.toString())
        intent.putExtra(IS_OFFLINE, false)
        startActivity(intent)
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etUrl.isEnabled = enabled
        binding.etXDrmUserToken.isEnabled = enabled
        binding.etDrmLicenseUrl.isEnabled = enabled
        binding.etXDrmBrandGuid.isEnabled = enabled
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun fillFormWithDefaultData() {
        binding.etUrl.setText("https://dtkya1w875897.cloudfront.net/da6dc30a-e52f-4af2-9751-000b89416a4e/assets/357577a1-3b61-43ae-9af5-82b9727e2f22/videokit-720p-dash-hls-drm/dash/index.mpd")
        binding.etDrmLicenseUrl.setText("https://insys-marketing.la.drm.cloud/acquire-license/widevine")
        binding.etXDrmBrandGuid.setText("da6dc30a-e52f-4af2-9751-000b89416a4e")
        // Set token that expired in 5 years to be able to test sample app.
        binding.etXDrmUserToken.setText("eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE5MjY1NzIzMDUsImtpZCI6WyIqIl0sImR1cmF0aW9uIjo4NjQwMCwid2lkZXZpbmUiOnsicGVyc2lzdGVudCI6dHJ1ZX19.sJ6fIK9pq2HdkkFIOsvxHmsYG0Hf1xEc4PHk7ab4m1c")
    }
}