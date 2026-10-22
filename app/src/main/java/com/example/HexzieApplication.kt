package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.HexzieDatabase
import com.example.data.local.DataStoreManager
import com.example.data.local.SecurityVault

class HexzieApplication : Application() {

    lateinit var database: HexzieDatabase
        private set

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var securityVault: SecurityVault
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = HexzieDatabase.getInstance(this)
        dataStoreManager = DataStoreManager(this)
        securityVault = SecurityVault(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE_SERVICE,
                getString(R.string.channel_voice_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_voice_service_desc)
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "HexZie Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts and HIGH-risk confirmation requests."
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(voiceChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val CHANNEL_VOICE_SERVICE = "hexzie_voice_service_channel"
        const val CHANNEL_ALERTS = "hexzie_alerts_channel"

        lateinit var instance: HexzieApplication
            private set
    }
}
