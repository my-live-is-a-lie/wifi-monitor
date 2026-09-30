package com.example.wifimonitor.ui
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifimonitor.data.DailyWifiUsage
import com.example.wifimonitor.data.formatBytes
@Composable
fun WeeklyChartCard(weeklyData: List<DailyWifiUsage>) {
    val maxBytes = (weeklyData.maxOfOrNull { it.bytes } ?: 1L).toFloat()
    val totalWeek = weeklyData.sumOf { it.bytes }
    Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                Column{ Text("Weekly Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Last 7 days • WiFi only", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primaryContainer){ Text(formatBytes(totalWeek), modifier = Modifier.padding(horizontal=12.dp, vertical=6.dp), fontWeight=FontWeight.Bold, fontSize=12.sp) }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                weeklyData.forEach { day ->
                    val fraction = if(maxBytes==0f) 0f else day.bytes/maxBytes
                    val animatedFraction by animateFloatAsState(targetValue=fraction, animationSpec=spring(dampingRatio=0.6f, stiffness=300f), label="")
                    val isMax = day.bytes==weeklyData.maxOf{it.bytes} && day.bytes>0
                    Column(horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.weight(1f)) {
                        Text(if(day.bytes>1024*1024) String.format("%.1f",day.bytes/1024f/1024f) else "0", fontSize=10.sp, color=if(isMax) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight=if(isMax)FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(16.dp), color = if(isMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.width(32.dp).fillMaxHeight(animatedFraction).clip(RoundedCornerShape(16.dp))){}
                        Spacer(Modifier.height(8.dp))
                        Text(day.label, fontSize=11.sp, fontWeight=if(day.isToday) FontWeight.ExtraBold else FontWeight.Medium, color=if(day.isToday) MaterialTheme.colorScheme.primary else Color.Black, textAlign=TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(8.dp)); Text("Highest usage on ${weeklyData.maxByOrNull{it.bytes}?.label}", style=MaterialTheme.typography.labelSmall, color=Color.Gray)
        }
    }
}