package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.downloads

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities.DownloadedVideo
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.databinding.ActivityDownloadsBinding
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.PlayerActivity
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.common.ViewUtils.toast
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.DRM_LICENSE_URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.IS_OFFLINE
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.IntentExtra.URL
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.viewmodel.ExoPlayerViewModel
import kotlinx.coroutines.launch

class DownloadsActivity : AppCompatActivity() {
    private lateinit var viewModel: ExoPlayerViewModel
    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var downloadsAdapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ExoPlayerViewModel::class.java]
        setupRecyclerView()
        observeDownloadedVideos()
    }

    private fun setupRecyclerView() {
        downloadsAdapter = DownloadsAdapter(
            onPlayClicked = { video ->
                playVideoOffline(video)
            },
            onDeleteClicked = { video ->
                viewModel.deleteVideo(video)
                toast("Video with id: ${video.videoId} has been deleted.")
            }
        )

        binding.downloadsRecyclerView.apply {
            adapter = downloadsAdapter
            layoutManager = LinearLayoutManager(this@DownloadsActivity)
            setHasFixedSize(true)
        }
    }

    private fun observeDownloadedVideos() {
        lifecycleScope.launch {
            viewModel.allDownloadedVideos.collect { videoList ->
                if (videoList.isNotEmpty()) {
                    binding.tvNoData.visibility = View.GONE
                    binding.downloadsRecyclerView.visibility = View.VISIBLE
                    binding.tvHeader.visibility = View.VISIBLE
                    downloadsAdapter.submitList(videoList)
                } else {
                    binding.tvNoData.visibility = View.VISIBLE
                    binding.downloadsRecyclerView.visibility = View.GONE
                    binding.tvHeader.visibility = View.GONE
                }
            }
        }
    }

    private fun playVideoOffline(video: DownloadedVideo) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(URL, video.mediaUrl)
            putExtra(DRM_LICENSE_URL, video.drmLicenseUrl)
            putExtra(IS_OFFLINE, true)
        }
        startActivity(intent)
    }
}