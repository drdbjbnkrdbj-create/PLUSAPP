package com.example.data.repository

import com.example.data.local.AchievementEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.IfThenPlanEntity
import com.example.data.local.PulseDatabase
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.GoalStatus
import com.example.data.model.HabitType
import com.example.data.model.RecordStatus
import com.example.data.model.TrackingMode
import com.example.data.model.UrgeRecordKind
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.FirestoreSyncRepository
import com.example.domain.PulseAnalyticsEngine
import com.example.notifications.ReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class PulseRepository(
    private val db: PulseDatabase,
    private val prefsRepo: UserPreferencesRepository,
    private val firestoreSync: FirestoreSyncRepository?,
    private val reminderScheduler: ReminderScheduler?
) {
    private val habitDao = db.habitDao()
    private val recordDao = db.habitRecordDao()
    private val urgeDao = db.urgeRecordDao()
    private val goalDao = db.goalDao()
    private val focusDao = db.focusSessionDao()
    private val planDao = db.ifThenPlanDao()
    private val achievementDao = db.achievementDao()

    val activeHabitsFlow: Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
    val allHabitsFlow: Flow<List<HabitEntity>> = habitDao.observeAllHabits()
    val allRecordsFlow: Flow<List<HabitRecordEntity>> = recordDao.observeAllRecords()
    val allUrgesFlow: Flow<List<UrgeRecordEntity>> = urgeDao.observeAllUrgeRecords()
    val allGoalsFlow: Flow<List<GoalEntity>> = goalDao.observeAllGoals()
    val allFocusSessionsFlow: Flow<List<FocusSessionEntity>> = focusDao.observeAllSessions()
    val allIfThenPlansFlow: Flow<List<IfThenPlanEntity>> = planDao.observeAllPlans()
    val allAchievementsFlow: Flow<List<AchievementEntity>> = achievementDao.observeAllAchievements()

    fun observeHabit(habitId: String): Flow<HabitEntity?> = habitDao.observeHabitById(habitId)
    fun observeRecordsForHabit(habitId: String): Flow<List<HabitRecordEntity>> =
        recordDao.observeRecordsForHabit(habitId)
    fun observeUrgesForHabit(habitId: String): Flow<List<UrgeRecordEntity>> =
        urgeDao.observeUrgesForHabit(habitId)

    private fun isSignedIn(): Boolean =
        firestoreSync != null && runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)

    suspend fun saveHabit(habit: HabitEntity): HabitEntity {
        val clean = habit.copy(
            id = habit.id.ifBlank { UUID.randomUUID().toString() },
            name = habit.name.trim(),
            description = habit.description.trim(),
            targetValue = habit.targetValue.coerceIn(1, 10000),
            updatedAtMs = System.currentTimeMillis()
        )
        habitDao.upsertHabit(clean)
        val prefs = prefsRepo.preferencesFlow.first()
        if (prefs.notificationsEnabled && clean.reminderEnabled && !clean.archived) {
            reminderScheduler?.scheduleHabitReminder(clean)
        } else {
            reminderScheduler?.cancelHabitReminder(clean.id)
        }
        recalculateAchievements()
        if (isSignedIn()) {
            firestoreSync?.upsertHabit(clean)
        }
        return clean
    }

    suspend fun deleteHabit(habitId: String) {
        reminderScheduler?.cancelHabitReminder(habitId)
        habitDao.deleteHabitById(habitId)
        if (isSignedIn()) {
            firestoreSync?.deleteHabit(habitId)
        }
    }

    suspend fun toggleQuickHabitStatus(habit: HabitEntity, epochDay: Long) {
        val existing = recordDao.getRecord(habit.id, epochDay)
        val now = System.currentTimeMillis()
        val nextStatus: RecordStatus
        val nextProgress: Int

        when (habit.habitTypeEnum()) {
            HabitType.BUILD -> {
                if (existing == null || existing.statusEnum() == RecordStatus.MISSED || existing.statusEnum() == RecordStatus.SKIPPED) {
                    nextStatus = RecordStatus.COMPLETED
                    nextProgress = habit.targetValue
                } else {
                    nextStatus = RecordStatus.MISSED
                    nextProgress = 0
                }
            }
            HabitType.QUIT -> {
                if (existing == null || existing.statusEnum() == RecordStatus.RELAPSE || existing.statusEnum() == RecordStatus.MISSED) {
                    nextStatus = RecordStatus.CLEAN_DAY
                    nextProgress = 1
                } else {
                    nextStatus = RecordStatus.RELAPSE
                    nextProgress = 0
                }
            }
        }

        val record = HabitRecordEntity(
            id = existing?.id ?: "${habit.id}_$epochDay",
            habitId = habit.id,
            epochDay = epochDay,
            status = nextStatus.name,
            progressValue = nextProgress,
            notes = existing?.notes ?: "",
            recordedTimestampMs = now,
            updatedAtMs = now
        )
        recordDao.upsertRecord(record)

        if (habit.habitTypeEnum() == HabitType.QUIT && nextStatus == RecordStatus.RELAPSE) {
            val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val urge = UrgeRecordEntity(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                epochDay = epochDay,
                timeString = timeStr,
                recordKind = UrgeRecordKind.RELAPSE.name,
                intensity = 5,
                notes = existing?.notes ?: "",
                recordedTimestampMs = now
            )
            urgeDao.upsertUrgeRecord(urge)
            if (isSignedIn()) firestoreSync?.upsertUrgeRecord(urge)
        }

        recalculateAchievements()
        if (isSignedIn()) {
            firestoreSync?.upsertHabitRecord(record)
        }
    }

    suspend fun updateNumericalProgress(habit: HabitEntity, epochDay: Long, delta: Int) {
        val existing = recordDao.getRecord(habit.id, epochDay)
        val currentVal = existing?.progressValue ?: 0
        val newVal = (currentVal + delta).coerceIn(0, 100000)
        val status = if (newVal >= habit.targetValue) RecordStatus.COMPLETED else RecordStatus.MISSED
        saveHabitRecord(
            habit = habit,
            epochDay = epochDay,
            status = status,
            progressValue = newVal,
            notes = existing?.notes ?: ""
        )
    }

    suspend fun saveHabitRecord(
        habit: HabitEntity,
        epochDay: Long,
        status: RecordStatus,
        progressValue: Int,
        notes: String
    ) {
        val existing = recordDao.getRecord(habit.id, epochDay)
        val now = System.currentTimeMillis()
        val effectiveProgress = when {
            habit.habitTypeEnum() == HabitType.BUILD && habit.trackingModeEnum() == TrackingMode.NUMERICAL -> progressValue
            status == RecordStatus.COMPLETED || status == RecordStatus.CLEAN_DAY -> habit.targetValue
            else -> 0
        }
        val record = HabitRecordEntity(
            id = existing?.id ?: "${habit.id}_$epochDay",
            habitId = habit.id,
            epochDay = epochDay,
            status = status.name,
            progressValue = effectiveProgress,
            notes = notes.trim().take(500),
            recordedTimestampMs = now,
            updatedAtMs = now
        )
        recordDao.upsertRecord(record)
        recalculateAchievements()
        if (isSignedIn()) {
            firestoreSync?.upsertHabitRecord(record)
        }
    }

    suspend fun logUrgeOrRelapse(
        habitId: String?,
        epochDay: Long,
        kind: UrgeRecordKind,
        intensity: Int,
        notes: String
    ) {
        val now = System.currentTimeMillis()
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val entry = UrgeRecordEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            epochDay = epochDay,
            timeString = timeStr,
            recordKind = kind.name,
            intensity = intensity.coerceIn(1, 10),
            notes = notes.trim().take(500),
            recordedTimestampMs = now
        )
        urgeDao.upsertUrgeRecord(entry)
        if (kind == UrgeRecordKind.RELAPSE && habitId != null) {
            val habit = habitDao.getHabitById(habitId)
            if (habit != null) {
                saveHabitRecord(
                    habit = habit,
                    epochDay = epochDay,
                    status = RecordStatus.RELAPSE,
                    progressValue = 0,
                    notes = notes
                )
            }
        }
        recalculateAchievements()
        if (isSignedIn()) {
            firestoreSync?.upsertUrgeRecord(entry)
        }
    }

    suspend fun saveGoal(goal: GoalEntity) {
        val now = System.currentTimeMillis()
        val computedStatus = when {
            goal.currentProgress >= goal.targetTotal -> GoalStatus.COMPLETED.name
            goal.status == GoalStatus.COMPLETED.name && goal.currentProgress < goal.targetTotal -> GoalStatus.ACTIVE.name
            else -> goal.status
        }
        val clean = goal.copy(
            id = goal.id.ifBlank { UUID.randomUUID().toString() },
            title = goal.title.trim(),
            description = goal.description.trim(),
            targetTotal = goal.targetTotal.coerceAtLeast(1),
            currentProgress = goal.currentProgress.coerceAtLeast(0),
            status = computedStatus,
            updatedAtMs = now
        )
        goalDao.upsertGoal(clean)
        if (isSignedIn()) {
            firestoreSync?.upsertGoal(clean)
        }
    }

    suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoalById(goalId)
        if (isSignedIn()) {
            firestoreSync?.deleteGoal(goalId)
        }
    }

    suspend fun logFocusSession(durationMinutes: Int, completed: Boolean, type: String) {
        val now = System.currentTimeMillis()
        val session = FocusSessionEntity(
            id = UUID.randomUUID().toString(),
            durationMinutes = durationMinutes.coerceIn(1, 480),
            epochDay = LocalDate.now().toEpochDay(),
            completed = completed,
            sessionType = type,
            startedAtMs = now
        )
        focusDao.upsertSession(session)
        recalculateAchievements()
        if (isSignedIn()) {
            firestoreSync?.upsertFocusSession(session)
        }
    }

    suspend fun saveIfThenPlan(plan: IfThenPlanEntity) {
        val now = System.currentTimeMillis()
        val clean = plan.copy(
            id = plan.id.ifBlank { UUID.randomUUID().toString() },
            triggerText = plan.triggerText.trim(),
            responseText = plan.responseText.trim(),
            updatedAtMs = now
        )
        planDao.upsertPlan(clean)
        if (isSignedIn()) {
            firestoreSync?.upsertIfThenPlan(clean)
        }
    }

    suspend fun deleteIfThenPlan(planId: String) {
        planDao.deletePlanById(planId)
        if (isSignedIn()) {
            firestoreSync?.deleteIfThenPlan(planId)
        }
    }

    suspend fun recalculateAchievements() {
        val habits = habitDao.getAllHabitsOnce()
        val records = recordDao.getAllRecordsOnce()
        val sessions = focusDao.getAllSessionsOnce()
        val urges = urgeDao.getAllUrgeRecordsOnce()
        val existing = achievementDao.getAllAchievementsOnce()
        val updated = PulseAnalyticsEngine.evaluateAchievements(
            habits = habits,
            records = records,
            focusSessions = sessions,
            urgeRecords = urges,
            existing = existing
        )
        achievementDao.upsertAchievements(updated)
    }

    suspend fun rescheduleAllReminders() {
        val prefs = prefsRepo.preferencesFlow.first()
        val reminderHabits = habitDao.getReminderEnabledHabits()
        if (!prefs.notificationsEnabled) {
            reminderHabits.forEach { reminderScheduler?.cancelHabitReminder(it.id) }
        } else {
            reminderHabits.forEach { reminderScheduler?.scheduleHabitReminder(it) }
        }
    }

    suspend fun syncWithCloud(): Result<Unit> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val sync = firestoreSync ?: return Result.failure(IllegalStateException("Sync unavailable"))

        val prefs = prefsRepo.preferencesFlow.first()
        sync.upsertUserProfile(prefs, user.displayName ?: "PULSE User")

        // Push local entities to Firestore
        habitDao.getAllHabitsOnce().forEach { sync.upsertHabit(it) }
        recordDao.getAllRecordsOnce().forEach { sync.upsertHabitRecord(it) }
        urgeDao.getAllUrgeRecordsOnce().forEach { sync.upsertUrgeRecord(it) }
        goalDao.getAllGoalsOnce().forEach { sync.upsertGoal(it) }
        focusDao.getAllSessionsOnce().forEach { sync.upsertFocusSession(it) }
        planDao.getAllPlansOnce().forEach { sync.upsertIfThenPlan(it) }

        // Pull any cloud-only entities into Room (preserving local history via upsert)
        val cloudResult = sync.pullAllUserDataFromCloud(user.uid)
        cloudResult.onSuccess { bundle ->
            val localHabitIds = habitDao.getAllHabitsOnce().map { it.id }.toSet()
            val newCloudHabits = bundle.habits.filter { it.id !in localHabitIds }
            if (newCloudHabits.isNotEmpty()) habitDao.upsertHabits(newCloudHabits)

            val allHabitIds = (localHabitIds + newCloudHabits.map { it.id }).toSet()
            val localRecordKeys = recordDao.getAllRecordsOnce().map { it.habitId to it.epochDay }.toSet()
            val newCloudRecords = bundle.records.filter {
                it.habitId in allHabitIds && (it.habitId to it.epochDay) !in localRecordKeys
            }
            if (newCloudRecords.isNotEmpty()) recordDao.upsertRecords(newCloudRecords)

            val localUrgeIds = urgeDao.getAllUrgeRecordsOnce().map { it.id }.toSet()
            val newCloudUrges = bundle.urges.filter { it.id !in localUrgeIds }
                .map { u -> if (u.habitId != null && u.habitId !in allHabitIds) u.copy(habitId = null) else u }
            if (newCloudUrges.isNotEmpty()) urgeDao.upsertUrgeRecords(newCloudUrges)

            val localGoalIds = goalDao.getAllGoalsOnce().map { it.id }.toSet()
            val newCloudGoals = bundle.goals.filter { it.id !in localGoalIds }
            if (newCloudGoals.isNotEmpty()) goalDao.upsertGoals(newCloudGoals)

            val localSessionIds = focusDao.getAllSessionsOnce().map { it.id }.toSet()
            val newCloudSessions = bundle.focusSessions.filter { it.id !in localSessionIds }
            if (newCloudSessions.isNotEmpty()) focusDao.upsertSessions(newCloudSessions)

            val localPlanIds = planDao.getAllPlansOnce().map { it.id }.toSet()
            val newCloudPlans = bundle.ifThenPlans.filter { it.id !in localPlanIds }
            if (newCloudPlans.isNotEmpty()) planDao.upsertPlans(newCloudPlans)

            recalculateAchievements()
            rescheduleAllReminders()
            prefsRepo.setLastSyncedAtMs(System.currentTimeMillis())
        }
        return cloudResult.map { Unit }
    }
}
