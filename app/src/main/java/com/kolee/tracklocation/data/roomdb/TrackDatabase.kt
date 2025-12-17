package com.kolee.tracklocation.data.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class], version = 1, exportSchema = false)
abstract class TrackDatabase: RoomDatabase() {

    abstract val trackDao: TrackDao

    companion object {
        @Volatile
        var INSTANCE: TrackDatabase? = null

        fun getDatabase(context: Context): TrackDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context,
                    TrackDatabase::class.java,
                    "track_db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}