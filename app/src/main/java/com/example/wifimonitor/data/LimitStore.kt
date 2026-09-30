package com.example.wifimonitor.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
enum class LimitPeriod { DAILY, MONTHLY }
val Context.dataStore by preferencesDataStore("wifi_limit")
class LimitStore(private val context: Context) {
    companion object {
        val LIMIT_GB = doublePreferencesKey("limit_gb")
        val ENABLED = booleanPreferencesKey("limit_enabled")
        val PERIOD = stringPreferencesKey("limit_period")
    }
    val limitFlow = context.dataStore.data.map { it[LIMIT_GB] ?: 5.0 }
    val enabledFlow = context.dataStore.data.map { it[ENABLED] ?: true }
    val periodFlow = context.dataStore.data.map { try { LimitPeriod.valueOf(it[PERIOD] ?: "DAILY") } catch(e: Exception){ LimitPeriod.DAILY } }
    suspend fun setLimit(gb: Double) { context.dataStore.edit { it[LIMIT_GB] = gb } }
    suspend fun setEnabled(enabled: Boolean) { context.dataStore.edit { it[ENABLED] = enabled } }
    suspend fun setPeriod(period: LimitPeriod) { context.dataStore.edit { it[PERIOD] = period.name } }
}