package com.example.wifimonitor.data
import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.getSystemService
data class WifiUsage(val todayBytes: Long=0, val weekBytes: Long=0, val monthBytes: Long=0, val todayRx: Long=0, val todayTx: Long=0)
data class DailyWifiUsage(val label: String, val bytes: Long, val isToday: Boolean=false)
class WifiUsageRepository(private val context: Context) {
    private val networkStatsManager = context.getSystemService<NetworkStatsManager>()!!
    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService<AppOpsManager>()!!
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
    fun openUsageSettings() { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    private fun getWifiBytes(startTime: Long, endTime: Long): Long {
        return try {
            val bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) { 0L }
    }
    private fun getWifiRxTx(startTime: Long, endTime: Long): Pair<Long, Long> {
        return try {
            val bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            Pair(bucket.rxBytes, bucket.txBytes)
        } catch (e: Exception) { Pair(0L, 0L) }
    }
    fun getDeviceWifiUsage(): WifiUsage {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay(now)
        val startOfWeek = now - 7 * 24 * 60 * 60 * 1000L
        val startOfMonth = getStartOfMonth(now)
        val (rx, tx) = getWifiRxTx(startOfDay, now)
        return WifiUsage(todayBytes = rx+tx, weekBytes = getWifiBytes(startOfWeek, now), monthBytes = getWifiBytes(startOfMonth, now), todayRx=rx, todayTx=tx)
    }
    fun getLast7DaysUsage(): List<DailyWifiUsage> {
        val cal = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val list = mutableListOf<DailyWifiUsage>()
        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0)
            val start = cal.timeInMillis; cal.add(java.util.Calendar.DAY_OF_YEAR, 1); val end = cal.timeInMillis
            val bytes = getWifiBytes(start, end)
            val label = if (i==0) "Today" else dateFormat.format(java.util.Date(start))
            list.add(DailyWifiUsage(label, bytes, i==0))
        }
        return list
    }
    fun getStartOfDay(time: Long): Long {
        val cal = java.util.Calendar.getInstance(); cal.timeInMillis=time; cal.set(java.util.Calendar.HOUR_OF_DAY,0); cal.set(java.util.Calendar.MINUTE,0); cal.set(java.util.Calendar.SECOND,0); return cal.timeInMillis
    }
    fun getStartOfMonth(time: Long): Long {
        val cal = java.util.Calendar.getInstance(); cal.timeInMillis=time; cal.set(java.util.Calendar.DAY_OF_MONTH,1); cal.set(java.util.Calendar.HOUR_OF_DAY,0); cal.set(java.util.Calendar.MINUTE,0); cal.set(java.util.Calendar.SECOND,0); return cal.timeInMillis
    }
    fun getTimeUntilReset(period: LimitPeriod): Long {
        val cal = java.util.Calendar.getInstance()
        if (period == LimitPeriod.DAILY) { cal.add(java.util.Calendar.DAY_OF_YEAR,1); cal.set(java.util.Calendar.HOUR_OF_DAY,0); cal.set(java.util.Calendar.MINUTE,0); cal.set(java.util.Calendar.SECOND,0) }
        else { cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)); cal.set(java.util.Calendar.HOUR_OF_DAY,23); cal.set(java.util.Calendar.MINUTE,59); cal.set(java.util.Calendar.SECOND,59) }
        return cal.timeInMillis - System.currentTimeMillis()
    }
}
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes/1024.0; if(kb<1024) return String.format("%.1f KB",kb)
    val mb = kb/1024.0; if(mb<1024) return String.format("%.2f MB",mb)
    val gb = mb/1024.0; return String.format("%.2f GB",gb)
}