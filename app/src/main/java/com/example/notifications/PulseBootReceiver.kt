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

class PulseBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appContext = context.applicationContext
                    val prefs = UserPreferencesRepository(appContext).preferencesFlow.first()
                    if (prefs.notificationsEnabled) {
                        val db = PulseDatabase.getInstance(appContext)
                        val scheduler = ReminderScheduler(appContext)
                        db.habitDao().getReminderEnabledHabits().forEach { habit ->
                            scheduler.scheduleHabitReminder(habit)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
