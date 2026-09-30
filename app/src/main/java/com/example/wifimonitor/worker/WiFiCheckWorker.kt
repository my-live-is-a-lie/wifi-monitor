package com.example.wifimonitor.worker
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wifimonitor.data.LimitPeriod
import com.example.wifimonitor.data.LimitStore
import com.example.wifimonitor.data.WifiUsageRepository
import com.example.wifimonitor.util.NotificationHelper
import kotlinx.coroutines.flow.first
class WiFiCheckWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val repo = WifiUsageRepository(applicationContext); val store = LimitStore(applicationContext)
        if(!repo.hasUsagePermission() || !store.enabledFlow.first()) return Result.success()
        val period = store.periodFlow.first(); val limitBytes = store.limitFlow.first()*1024*1024*1024
        val usage = repo.getDeviceWifiUsage(); val currentUsage = if(period==LimitPeriod.DAILY) usage.todayBytes else usage.monthBytes
        val progress = currentUsage / limitBytes; val periodName = if(period==LimitPeriod.DAILY) "daily" else "monthly"
        when {
            progress >= 1.0 -> NotificationHelper.show(applicationContext, "WiFi Limit Exceeded!", "You\'ve used ${String.format("%.2f", currentUsage/1024.0/1024/1024)} GB / ${store.limitFlow.first().toInt()} GB $periodName")
            progress >= 0.8 -> NotificationHelper.show(applicationContext, "WiFi Warning: 80% Used", "You\'ve used 80% of your $periodName WiFi limit (${store.limitFlow.first().toInt()} GB)")
        }
        return Result.success()
    }
}