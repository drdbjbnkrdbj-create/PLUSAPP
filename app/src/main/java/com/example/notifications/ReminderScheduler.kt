package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.HabitEntity
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleHabitReminder(habit: HabitEntity) {
        if (!habit.reminderEnabled || habit.archived) {
            cancelHabitReminder(habit.id)
            return
        }
        val manager = alarmManager ?: return
        val triggerMs = computeNextTriggerMillis(habit) ?: return

        val pendingIntent = buildPendingIntent(habit.id, habit.name, habit.habitType)
        try {
            val useExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            if (useExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                // Graceful fallback when exact alarm permission is unavailable on Android 12+
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        } catch (se: SecurityException) {
            Log.w("ReminderScheduler", "Exact alarm permission denied, using inexact alarm", se)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        }
    }

    fun cancelHabitReminder(habitId: String) {
        val manager = alarmManager ?: return
        val pendingIntent = buildPendingIntent(habitId, "", "BUILD")
        manager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        habitId: String,
        habitName: String,
        habitType: String
    ): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_HABIT_TYPE, habitType)
        }
        return PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.aistudio.pulse.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_HABIT_TYPE = "extra_habit_type"

        fun computeNextTriggerMillis(
            habit: HabitEntity,
            now: LocalDateTime = LocalDateTime.now()
        ): Long? {
            val parts = habit.reminderTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: return null
            val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: return null
            val targetTime = LocalTime.of(hour, minute)
            val scheduledDays = habit.scheduleDaysList().ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }

            for (offset in 0..7) {
                val candidateDate = now.toLocalDate().plusDays(offset.toLong())
                if (candidateDate.dayOfWeek.value in scheduledDays) {
                    val candidateDateTime = LocalDateTime.of(candidateDate, targetTime)
                    if (candidateDateTime.isAfter(now.plusSeconds(5))) {
                        return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                }
            }
            return null
        }
    }
}
