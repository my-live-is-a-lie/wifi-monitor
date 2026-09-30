package com.example.wifimonitor
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifimonitor.data.DailyWifiUsage
import com.example.wifimonitor.data.LimitPeriod
import com.example.wifimonitor.data.LimitStore
import com.example.wifimonitor.data.WifiUsageRepository
import com.example.wifimonitor.data.formatBytes
import com.example.wifimonitor.ui.WeeklyChartCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary=Color(0xFF2856FF), secondaryContainer=Color(0xFFE2E8FF), surfaceContainer=Color(0xFFF2F4FF))) { WifiMonitorScreen() } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiMonitorScreen() {
    val context = LocalContext.current; val repo = remember{WifiUsageRepository(context)}; val store = remember{LimitStore(context)}; val scope = rememberCoroutineScope()
    var hasPermission by remember{mutableStateOf(repo.hasUsagePermission())}
    var usage by remember{mutableStateOf(repo.getDeviceWifiUsage())}
    var weeklyData by remember{mutableStateOf(emptyList<DailyWifiUsage>())}
    var limitGB by remember{mutableStateOf(5.0)}; var isEnabled by remember{mutableStateOf(true)}; var period by remember{mutableStateOf(LimitPeriod.DAILY)}
    var showDialog by remember{mutableStateOf(false)}; var sliderValue by remember{mutableStateOf(5f)}; var resetText by remember{mutableStateOf("")}
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    LaunchedEffect(Unit){
        if(Build.VERSION.SDK_INT>=33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        limitGB=store.limitFlow.first(); isEnabled=store.enabledFlow.first(); period=store.periodFlow.first(); sliderValue=limitGB.toFloat()
        usage=repo.getDeviceWifiUsage(); weeklyData=repo.getLast7DaysUsage()
    }
    LaunchedEffect(period){
        while(true){
            if(hasPermission){ usage=repo.getDeviceWifiUsage(); weeklyData=repo.getLast7DaysUsage() }
            val millis=repo.getTimeUntilReset(period); val days=millis/(24*60*60*1000); val hours=(millis%(24*60*60*1000))/(60*60*1000); val mins=(millis%(60*60*1000))/(60*1000)
            resetText=if(period==LimitPeriod.DAILY) "${hours}h ${mins}m left" else "${days}d ${hours}h left"
            delay(60000)
        }
    }
    Scaffold(topBar={CenterAlignedTopAppBar(title={Text("WiFi Monitor", fontWeight=FontWeight.Bold)}, navigationIcon={Icon(Icons.Default.Wifi,null,tint=MaterialTheme.colorScheme.primary)})}){ padding ->
        if(!hasPermission){
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment=Alignment.Center){
                Column(horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.padding(32.dp)){
                    Icon(Icons.Default.Wifi,null,Modifier.size(64.dp), tint=Color(0xFF2856FF)); Spacer(Modifier.height(16.dp))
                    Text("Usage Access Required", fontWeight=FontWeight.Bold); Text("Enable to see WiFi stats", color=Color.Gray)
                    Spacer(Modifier.height(16.dp)); Button(onClick={repo.openUsageSettings()}, shape=RoundedCornerShape(50.dp)){Text("Grant Permission")}
                    Spacer(Modifier.height(8.dp)); TextButton(onClick={hasPermission=repo.hasUsagePermission()}){Text("I have enabled - Refresh")}
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(16.dp)){
                item{
                    val currentBytes=if(period==LimitPeriod.DAILY) usage.todayBytes else usage.monthBytes
                    val progress=(currentBytes/(limitGB*1024*1024*1024)).toFloat().coerceIn(0f,1f)
                    val isWarning=progress>=0.8f; val isExceeded=progress>=1f
                    Card(shape=RoundedCornerShape(32.dp), colors=CardDefaults.cardColors(containerColor=when{!isEnabled->MaterialTheme.colorScheme.secondaryContainer; isExceeded->MaterialTheme.colorScheme.errorContainer; isWarning->Color(0xFFFFF0CC); else->MaterialTheme.colorScheme.primary}), modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally){
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                                Text(if(period==LimitPeriod.DAILY) "Daily Limit" else "Monthly Limit", fontWeight=FontWeight.Bold, color=if(isEnabled && !isWarning && !isExceeded) Color.White else Color.Black)
                                Switch(checked=isEnabled, onCheckedChange={isEnabled=it; scope.launch{store.setEnabled(it)}})
                            }
                            Spacer(Modifier.height(12.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){
                                SegmentedButton(selected=period==LimitPeriod.DAILY, onClick={period=LimitPeriod.DAILY; scope.launch{store.setPeriod(period)}}, shape=RoundedCornerShape(50.dp)){Text("Daily")}
                                SegmentedButton(selected=period==LimitPeriod.MONTHLY, onClick={period=LimitPeriod.MONTHLY; scope.launch{store.setPeriod(period)}}, shape=RoundedCornerShape(50.dp)){Text("Monthly")}
                            }
                            Spacer(Modifier.height(20.dp))
                            Box(contentAlignment=Alignment.Center, modifier=Modifier.size(160.dp)){
                                CircularProgressIndicator(progress={progress}, strokeWidth=12.dp, modifier=Modifier.fillMaxSize(), color=when{isExceeded->Color(0xFFBA1A1A); isWarning->Color(0xFFFF8C00); else->Color.White}, trackColor=Color.White.copy(alpha=0.2f))
                                Column(horizontalAlignment=Alignment.CenterHorizontally){ Text("${(progress*100).toInt()}%", fontSize=32.sp, fontWeight=FontWeight.ExtraBold, color=if(isEnabled && !isWarning && !isExceeded) Color.White else Color.Black); Text("${formatBytes(currentBytes)} / ${limitGB.toInt()} GB", fontSize=12.sp, color=if(isEnabled && !isWarning && !isExceeded) Color.White.copy(0.8f) else Color.Gray) }
                            }
                            Spacer(Modifier.height(12.dp))
                            AssistChip(onClick={}, label={Text("Resets in $resetText", fontWeight=FontWeight.Medium)}, leadingIcon={Icon(Icons.Default.Timer,null,Modifier.size(16.dp))}, colors=AssistChipDefaults.assistChipColors(containerColor=Color.White))
                            Spacer(Modifier.height(12.dp))
                            Button(onClick={showDialog=true}, shape=RoundedCornerShape(50.dp), colors=ButtonDefaults.buttonColors(containerColor=Color.White, contentColor=Color.Black)){ Icon(Icons.Default.Settings,null,Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Set Limit") }
                        }
                    }
                }
                item{ WeeklyChartCard(weeklyData=weeklyData) }
                item{
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp), modifier=Modifier.fillMaxWidth()){
                        StatCard("Today", formatBytes(usage.todayBytes), Icons.Default.Today, Modifier.weight(1f))
                        StatCard("Month", formatBytes(usage.monthBytes), Icons.Default.CalendarMonth, Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if(showDialog){
        AlertDialog(onDismissRequest={showDialog=false}, title={Text("Set ${period.name.lowercase().replaceFirstChar{it.uppercase()}} Limit")},
            text={Column{Text("Limit: ${sliderValue.toInt()} GB", fontWeight=FontWeight.Bold); Slider(value=sliderValue, onValueChange={sliderValue=it}, valueRange=if(period==LimitPeriod.DAILY) 1f..30f else 10f..500f)}},
            confirmButton={TextButton(onClick={limitGB=sliderValue.toDouble(); scope.launch{store.setLimit(limitGB)}; showDialog=false}){Text("Save")}},
            dismissButton={TextButton(onClick={showDialog=false}){Text("Cancel")}})
    }
}
@Composable fun StatCard(title:String, value:String, icon:androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier){
    Card(modifier, shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){
        Column(Modifier.padding(16.dp)){ Icon(icon,null, tint=MaterialTheme.colorScheme.primary); Text(title, style=MaterialTheme.typography.labelLarge); Text(value, fontWeight=FontWeight.Bold, fontSize=16.sp) }
    }
}