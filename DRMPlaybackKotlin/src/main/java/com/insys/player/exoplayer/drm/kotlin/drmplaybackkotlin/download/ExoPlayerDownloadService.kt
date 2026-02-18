package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.download

import android.app.Notification
import android.app.PendingIntent
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.R
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.DownloadUtil

class ExoPlayerDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_notification_channel_name,
    R.string.download_notification_channel_description
) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }

    /**
     * Returns the singleton [DownloadManager] instance used to manage all download operations.
     */
    override fun getDownloadManager(): DownloadManager = DownloadUtil.getDownloadManager(this)

    /**
     * Provides a [Scheduler] to define constraints for when downloads can run.
     *
     * Returning `null` disables the scheduler, meaning downloads will start as soon as
     * network connectivity is available.
     *
     * @return A [Scheduler] instance or `null` to disable scheduling.
     */
    override fun getScheduler(): Scheduler? = null

    /**
     * Creates and returns the notification displayed while the service is in the foreground.
     *
     * @param downloads A list of the current downloads being processed.
     * @param notMetRequirements Requirements that are not met for scheduled downloads.
     * @return The [Notification] to be displayed for the foreground service.
     */
    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationHelper = DownloadNotificationHelper(this, CHANNEL_ID)
        return notificationHelper.buildProgressNotification(
            this,
            R.drawable.ic_download,
            pendingIntent,
            null,
            downloads,
            notMetRequirements
        )
    }
}