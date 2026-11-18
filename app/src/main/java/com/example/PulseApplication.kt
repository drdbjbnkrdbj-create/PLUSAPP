package com.example

import android.app.Application
import com.example.data.backup.PulseBackupManager
import com.example.data.local.PulseDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.repository.PulseRepository
import com.example.notifications.PulseNotificationHelper
import com.example.notifications.ReminderScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class PulseApplication : Application() {

    lateinit var database: PulseDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set
    lateinit var backupManager: PulseBackupManager
        private set
    lateinit var pulseRepository: PulseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        PulseNotificationHelper.createNotificationChannel(this)

        database = PulseDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        reminderScheduler = ReminderScheduler(this)
        backupManager = PulseBackupManager(database, preferencesRepository)

        val firestoreSync = runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                val firestore = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
                FirestoreSyncRepository(firestore)
            } else {
                null
            }
        }.getOrNull()

        pulseRepository = PulseRepository(
            db = database,
            prefsRepo = preferencesRepository,
            firestoreSync = firestoreSync,
            reminderScheduler = reminderScheduler
        )
    }
}
