package com.example.namazstreak

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Forest = Color(0xFF103B2D)
private val Gold = Color(0xFFD8B45C)
private val prayers = listOf("Fajr", "Zuhr", "Asr", "Maghrib", "Isha")

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { NamazApp(this) }
    }
}

@Composable
private fun NamazApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("namaz_streak", Context.MODE_PRIVATE) }
    var refresh by remember { mutableIntStateOf(0) }
    var reminders by remember { mutableStateOf(prefs.getBoolean("reminders", true)) }
    var showTimes by remember { mutableStateOf(false) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val key = "day_$today"
    val checked = (0..4).map { prefs.getBoolean("${key}_$it", false) }
    val count = checked.count { it }
    val currentStreak = calculateStreak(prefs)
    val longest = prefs.getInt("longest_streak", 0).coerceAtLeast(currentStreak)

    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Gold, background = Color(0xFFF4F7F1))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF4F7F1)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(24.dp)).padding(22.dp)) {
                        Text("☾ NAMAZ STREAK", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Har namaz, ek qadam", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), color = Color(0xFFDDE8DF))
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("🔥 Streak", "$currentStreak days", Modifier.weight(1f))
                            StatCard("🕌 Aaj", "$count / 5", Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aaj ki namazein", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${count * 20}%", color = Forest, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { count / 5f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Forest, trackColor = Color(0xFFDCE6DC))
                }
                items(5) { i ->
                    val isChecked = checked[i]
                    Row(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).clickable {
                            prefs.edit().putBoolean("${key}_$i", !isChecked).apply()
                            val updated = (0..4).count { j -> if (j == i) !isChecked else prefs.getBoolean("${key}_$j", false) }
                            val newStreak = calculateStreak(prefs)
                            prefs.edit().putInt("longest_streak", maxOf(prefs.getInt("longest_streak", 0), newStreak)).apply()
                            refresh++
                        }.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(listOf("🌅", "☀️", "🌤️", "🌇", "🌙")[i], fontSize = 25.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(prayers[i], fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text(if (isChecked) "Marked complete" else "Tap when completed", color = Color(0xFF6C7A72), fontSize = 12.sp)
                        }
                        Checkbox(checked = isChecked, onCheckedChange = {
                            prefs.edit().putBoolean("${key}_$i", it).apply()
                            val s = calculateStreak(prefs)
                            prefs.edit().putInt("longest_streak", maxOf(prefs.getInt("longest_streak", 0), s)).apply()
                            refresh++
                        }, colors = CheckboxDefaults.colors(checkedColor = Forest))
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp)) {
                        Text("Your progress", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Longest streak: $longest days")
                        Text("Milestones: ${if (currentStreak >= 7) "🌱 7 days ✓" else "🌱 7 days"}  ·  ${if (currentStreak >= 30) "🌟 30 days ✓" else "🌟 30 days"}  ·  ${if (currentStreak >= 100) "🏆 100 days ✓" else "🏆 100 days"}", color = Forest)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Forest)
                            Spacer(Modifier.width(8.dp))
                            Text("Daily reminders", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(checked = reminders, onCheckedChange = {
                                reminders = it
                                prefs.edit().putBoolean("reminders", it).apply()
                                if (it) scheduleDefaultReminders(context) else cancelReminders(context)
                            })
                        }
                        Text("Default reminders: 5:30 AM, 1:30 PM, 4:30 PM, 6:00 PM, 8:00 PM. These are editable in the project; verify local prayer times before relying on them.", fontSize = 12.sp, color = Color(0xFF6C7A72))
                        if (reminders) {
                            Button(onClick = { scheduleDefaultReminders(context) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                                Text("Schedule reminders")
                            }
                        }
                    }
                }
                item { Text("Your data stays on this phone. Reminder times are sample defaults, not verified Karachi prayer times.", color = Color(0xFF6C7A72), fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0xFF245541), RoundedCornerShape(14.dp)).padding(13.dp)) {
        Text(label, color = Color(0xFFDDE8DF), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    }
}

fun calculateStreak(prefs: android.content.SharedPreferences): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    val today = sdf.format(cal.time)
    val todayDone = (0..4).all { prefs.getBoolean("day_${today}_$it", false) }
    if (!todayDone) cal.add(Calendar.DAY_OF_YEAR, -1)
    var streak = if (todayDone) 1 else 0
    while (true) {
        val date = sdf.format(cal.time)
        if ((0..4).all { prefs.getBoolean("day_${date}_$it", false) }) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else break
    }
    return streak
}
