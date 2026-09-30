package com.example.wifimonitor.util
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
object NotificationHelper {
    const val CHANNEL_ID = "wifi_alerts"
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "WiFi Alerts", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    fun show(context: Context, title: String, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_sys_wifi).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).build()
        nm.notify(1, notif)
    }
}