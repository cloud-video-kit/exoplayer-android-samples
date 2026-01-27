package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.R
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities.DownloadedVideo

class DownloadsAdapter(
    private val onPlayClicked: (DownloadedVideo) -> Unit,
    private val onDeleteClicked: (DownloadedVideo) -> Unit
) : ListAdapter<DownloadedVideo, DownloadsAdapter.VideoViewHolder>(VideoDiffCallback()) {

    /**
     * Inflates the layout for a single video item and creates the ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_downloaded_video, parent, false)
        return VideoViewHolder(view)
    }

    /**
     * Binds the data from a [DownloadedVideo] object to the ViewHolder views.
     */
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video, onPlayClicked, onDeleteClicked)
    }

    /**
     * ViewHolder class that holds references to the UI components of a video item.
     */
    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val idTextView: TextView = itemView.findViewById(R.id.videoIdTextView)
        private val playButton: Button = itemView.findViewById(R.id.playButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        /**
         * Connects the video data to the views and sets up click listeners.
         *
         * @param video The video data object.
         * @param onPlayClicked The function to handle playback.
         * @param onDeleteClicked The function to handle deletion.
         */
        fun bind(
            video: DownloadedVideo,
            onPlayClicked: (DownloadedVideo) -> Unit,
            onDeleteClicked: (DownloadedVideo) -> Unit
        ) {
            idTextView.text = video.videoId
            playButton.setOnClickListener { onPlayClicked(video) }
            deleteButton.setOnClickListener { onDeleteClicked(video) }
        }
    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     */
    class VideoDiffCallback : DiffUtil.ItemCallback<DownloadedVideo>() {
        /**
         * Checks if two objects represent the same item.
         */
        override fun areItemsTheSame(oldItem: DownloadedVideo, newItem: DownloadedVideo): Boolean {
            return oldItem.videoId == newItem.videoId
        }

        /**
         * Checks if the contents of two objects are exactly the same.
         */
        override fun areContentsTheSame(
            oldItem: DownloadedVideo,
            newItem: DownloadedVideo
        ): Boolean {
            return oldItem == newItem
        }
    }
}
