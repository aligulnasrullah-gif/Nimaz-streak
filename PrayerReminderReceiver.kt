package com.example.namazstreak

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

const val CHANNEL_ID = "namaz_reminders"

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(CHANNEL_ID, "Namaz reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Gentle reminders to check your prayer tracker"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val name = intent?.getStringExtra("prayer") ?: "Namaz"
        val open = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 900, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Namaz reminder")
            .setContentText("It's time to check in for $name.")
            .setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        context.getSystemService(NotificationManager::class.java).notify(name.hashCode(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED &&
            context.getSharedPreferences("namaz_streak", Context.MODE_PRIVATE).getBoolean("reminders", true)) {
            scheduleDefaultReminders(context)
        }
    }
}

fun scheduleDefaultReminders(context: Context) {
    val prefs = context.getSharedPreferences("namaz_streak", Context.MODE_PRIVATE)
    val times = listOf(Triple("Fajr", 5, 30), Triple("Zuhr", 13, 30), Triple("Asr", 16, 30), Triple("Maghrib", 18, 0), Triple("Isha", 20, 0))
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    times.forEachIndexed { index, t ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, t.second); set(Calendar.MINUTE, t.third); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, PrayerReminderReceiver::class.java).putExtra("prayer", t.first)
        val pi = PendingIntent.getBroadcast(context, 100 + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
        else manager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
        // Schedule tomorrow's repeat from receiver-independent daily scheduling is intentionally left simple in this starter.
    }
    prefs.edit().putBoolean("reminders", true).apply()
}

fun cancelReminders(context: Context) {
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    listOf(0,1,2,3,4).forEach { i ->
        val intent = Intent(context, PrayerReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 100 + i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pi)
    }
}
