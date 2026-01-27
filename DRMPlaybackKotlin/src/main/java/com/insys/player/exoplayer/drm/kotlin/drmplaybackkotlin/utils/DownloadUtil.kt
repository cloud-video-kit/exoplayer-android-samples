package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils

import android.content.Context
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloaderFactory
import com.google.android.exoplayer2.offline.WritableDownloadIndex
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object DownloadUtil {
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var downloadCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    /**
     * Initializes the [DownloadManager] ensuring a single instance is used throughout the application.
     *
     * @param context The application context, required for database and cache initialization.
     * @return The singleton [DownloadManager] instance.
     */
    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        if (downloadManager == null) {
            val downloadIndex: WritableDownloadIndex =
                DefaultDownloadIndex(getDatabaseProvider(context))

            val upstreamDataSourceFactory = DefaultHttpDataSource.Factory()

            val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
                .setCache(getDownloadCache(context))
                .setUpstreamDataSourceFactory(upstreamDataSourceFactory)

            val downloaderFactory: DownloaderFactory = DefaultDownloaderFactory(
                cacheDataSourceFactory
            ) { obj: Runnable -> obj.run() }

            downloadManager = DownloadManager(
                context,
                downloadIndex,
                downloaderFactory
            )
        }
        return downloadManager!!
    }

    /**
     * Initializes a [SimpleCache] used for storing downloaded video content.
     *
     * @param context The application context.
     * @return The singleton [Cache] instance.
     */
    @Synchronized
    fun getDownloadCache(context: Context): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory = File(
                context.getExternalFilesDir(null),
                DOWNLOAD_CONTENT_DIRECTORY
            )
            downloadCache = SimpleCache(
                downloadContentDirectory,
                NoOpCacheEvictor(),
                getDatabaseProvider(context)
            )
        }
        return downloadCache!!
    }

    /**
     * Creates a [StandaloneDatabaseProvider], which is used by both the download cache and the
     * download index to manage their SQLite databases.
     *
     * @param context The application context, required by the database provider.
     * @return The singleton [DatabaseProvider] instance.
     */
    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        }
        return databaseProvider!!
    }
}
