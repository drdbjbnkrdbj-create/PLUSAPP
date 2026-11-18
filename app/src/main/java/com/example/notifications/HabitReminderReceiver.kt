package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PulseDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_NAME) ?: "PULSE"
        val habitType = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_TYPE) ?: "BUILD"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(context.applicationContext).preferencesFlow.first()
                val db = PulseDatabase.getInstance(context.applicationContext)
                val habit = db.habitDao().getHabitById(habitId)
                if (prefs.notificationsEnabled && habit != null && habit.reminderEnabled && !habit.archived) {
                    PulseNotificationHelper.showHabitReminder(
                        context = context.applicationContext,
                        habitId = habit.id,
                        habitName = habit.name,
                        habitType = habit.habitType
                    )
                    ReminderScheduler(context.applicationContext).scheduleHabitReminder(habit)
                } else if (habit == null && prefs.notificationsEnabled) {
                    PulseNotificationHelper.showHabitReminder(
                        context = context.applicationContext,
                        habitId = habitId,
                        habitName = habitName,
                        habitType = habitType
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
