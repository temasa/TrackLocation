package com.kolee.tracklocation

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.kolee.tracklocation.data.roomdb.TrackDatabase
import com.kolee.tracklocation.utils.CHANNEL_ID

class TrackApp: Application() {

    val databaseDao by lazy {
        TrackDatabase.getDatabase(this).trackDao
    }
    val locationDao by lazy {
        TrackDatabase.getDatabase(this).locationDao
    }
    val sessionDao by lazy {
        TrackDatabase.getDatabase(this).sessionDao
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
