package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities.DownloadedVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    /**
     * Inserts a new video or updates an existing one if the videoId already exists.
     *
     * @param video The [DownloadedVideo] object to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(video: DownloadedVideo)


    /**
     * Finds a specific video by its unique ID.
     *
     * @param videoId The ID of the video to retrieve.
     * @return The [DownloadedVideo] if found, otherwise null.
     */
    @Query("SELECT * FROM downloaded_videos WHERE videoId = :videoId")
    suspend fun getVideoById(videoId: String): DownloadedVideo?

    /**
     * Retrieves all downloaded videos from the database.
     *
     * @return A flow emitting a list of [DownloadedVideo] objects.
     */
    @Query("SELECT * FROM downloaded_videos")
    fun getAllVideos(): Flow<List<DownloadedVideo>>

    /**
     * Deletes a specific video record from the database.
     *
     * @param video The [DownloadedVideo] object to remove.
     */
    @Delete
    suspend fun delete(video: DownloadedVideo)
}