package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.dao.VideoDao
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.database.entities.DownloadedVideo
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils.Constants.Database.DATABASE_NAME

@Database(entities = [DownloadedVideo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides access to data access objects for downloaded videos.
     * @return The [VideoDao] instance.
     */
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of [AppDatabase].
         *
         * @param context The application context used for the database.
         * @return The existing or newly created [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}