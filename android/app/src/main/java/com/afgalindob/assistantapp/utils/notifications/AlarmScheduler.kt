package com.afgalindob.assistantapp.utils.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import java.util.Calendar

const val tag = "AlarmScheduler"

object AlarmScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.afgalindob.assistantapp.ACTION_TRIGGER_REMINDER"

    fun schedule(context: Context, time: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toInt() ?: 8
        val minute = parts.getOrNull(1)?.toInt() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        try {
            if (isIgnoringBattery) {
                val alarmInfo = AlarmManager.AlarmClockInfo(
                    calendar.timeInMillis,
                    pendingIntent
                )
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                Log.d(tag, "Alarma de Reloj (Exacta) establecida para: ${calendar.time}")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.w(tag, "Batería optimizada. Usando fallback setAndAllowWhileIdle.")
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.w(tag, "Error: ${e.message}. Usando fallback inexacta.")
        }
    }
}