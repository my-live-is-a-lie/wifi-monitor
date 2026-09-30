package com.example.wifimonitor
import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wifimonitor.util.NotificationHelper
import com.example.wifimonitor.worker.WiFiCheckWorker
import java.util.concurrent.TimeUnit
class WifiMonitorApp: Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        val request = PeriodicWorkRequestBuilder<WiFiCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("wifi_check", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}