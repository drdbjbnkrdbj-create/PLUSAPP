package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.IfThenPlanEntity
import com.example.data.local.UrgeRecordEntity
import com.example.data.preferences.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.snapshots

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "doc" }

    fun observeRemoteHabits(userId: String): Flow<List<HabitEntity>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated user mismatch" }
        val path = "users/$uid/habits"
        db.collection("users").document(uid).collection("habits")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreHabit::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { dto ->
                    HabitEntity(
                        id = dto.localId.ifBlank { dto.id },
                        name = dto.name,
                        description = dto.description,
                        category = dto.category,
                        habitType = dto.habitType,
                        trackingMode = dto.trackingMode,
                        targetValue = dto.targetValue.toInt().coerceAtLeast(1),
                        unit = dto.unit,
                        iconKey = dto.iconKey,
                        colorHex = dto.colorHex,
                        scheduleType = dto.scheduleType,
                        scheduleDaysCsv = dto.scheduleDays.joinToString(","),
                        reminderEnabled = dto.reminderEnabled,
                        reminderTime = dto.reminderTime,
                        archived = dto.archived,
                        createdEpochDay = dto.createdEpochDay,
                        updatedAtMs = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun upsertUserProfile(prefs: UserPreferences, fallbackName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid)
        val path = docRef.path
        return try {
            val existing = docRef.get().await()
            val safeName = prefs.displayName.ifBlank { fallbackName.ifBlank { "PULSE User" } }.take(60)
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "themeMode" to prefs.themeMode.name,
                        "languageCode" to prefs.language.code,
                        "notificationsEnabled" to prefs.notificationsEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "themeMode" to prefs.themeMode.name,
                        "languageCode" to prefs.language.code,
                        "notificationsEnabled" to prefs.notificationsEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertHabit(habit: HabitEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = sanitizeDocId(habit.id)
        val docRef = db.collection("users").document(uid).collection("habits").document(docId)
        val path = docRef.path
        return try {
            val existing = docRef.get().await()
            val days = habit.scheduleDaysList().map { it.toLong() }.take(7)
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "name" to habit.name.take(100).ifBlank { "Habit" },
                        "description" to habit.description.take(400),
                        "category" to habit.category,
                        "habitType" to habit.habitType,
                        "trackingMode" to habit.trackingMode,
                        "targetValue" to habit.targetValue.coerceIn(1, 10000).toLong(),
                        "unit" to habit.unit.take(30),
                        "iconKey" to habit.iconKey.take(40).ifBlank { "check" },
                        "colorHex" to habit.colorHex.take(16).ifBlank { "#27C6D9" },
                        "scheduleType" to habit.scheduleType,
                        "scheduleDays" to days,
                        "reminderEnabled" to habit.reminderEnabled,
                        "reminderTime" to habit.reminderTime.take(10),
                        "archived" to habit.archived,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "localId" to docId,
                        "name" to habit.name.take(100).ifBlank { "Habit" },
                        "description" to habit.description.take(400),
                        "category" to habit.category,
                        "habitType" to habit.habitType,
                        "trackingMode" to habit.trackingMode,
                        "targetValue" to habit.targetValue.coerceIn(1, 10000).toLong(),
                        "unit" to habit.unit.take(30),
                        "iconKey" to habit.iconKey.take(40).ifBlank { "check" },
                        "colorHex" to habit.colorHex.take(16).ifBlank { "#27C6D9" },
                        "scheduleType" to habit.scheduleType,
                        "scheduleDays" to days,
                        "reminderEnabled" to habit.reminderEnabled,
                        "reminderTime" to habit.reminderTime.take(10),
                        "archived" to habit.archived,
                        "createdEpochDay" to habit.createdEpochDay.coerceIn(0L, 100000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = sanitizeDocId(habitId)
        val docRef = db.collection("users").document(uid).collection("habits").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertHabitRecord(record: HabitRecordEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = sanitizeDocId(record.id)
        val docRef = db.collection("users").document(uid).collection("habit_records").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "status" to record.status,
                        "progressValue" to record.progressValue.coerceIn(0, 100000).toLong(),
                        "notes" to record.notes.take(500),
                        "recordedTimestampMs" to record.recordedTimestampMs.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "habitLocalId" to sanitizeDocId(record.habitId),
                        "epochDay" to record.epochDay.coerceIn(0L, 100000L),
                        "status" to record.status,
                        "progressValue" to record.progressValue.coerceIn(0, 100000).toLong(),
                        "notes" to record.notes.take(500),
                        "recordedTimestampMs" to record.recordedTimestampMs.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertUrgeRecord(record: UrgeRecordEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = sanitizeDocId(record.id)
        val docRef = db.collection("users").document(uid).collection("urge_records").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mutableMapOf<String, Any>(
                    "epochDay" to record.epochDay.coerceIn(0L, 100000L),
                    "timeString" to record.timeString.take(20).ifBlank { "00:00" },
                    "recordKind" to record.recordKind,
                    "intensity" to record.intensity.coerceIn(1, 10).toLong(),
                    "notes" to record.notes.take(500),
                    "recordedTimestampMs" to record.recordedTimestampMs.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                record.habitId?.let { updateMap["habitLocalId"] = sanitizeDocId(it) }
                docRef.update(updateMap).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "habitLocalId" to record.habitId?.let { sanitizeDocId(it) },
                    "epochDay" to record.epochDay.coerceIn(0L, 100000L),
                    "timeString" to record.timeString.take(20).ifBlank { "00:00" },
                    "recordKind" to record.recordKind,
                    "intensity" to record.intensity.coerceIn(1, 10).toLong(),
                    "notes" to record.notes.take(500),
                    "recordedTimestampMs" to record.recordedTimestampMs.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertGoal(goal: GoalEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = sanitizeDocId(goal.id)
        val docRef = db.collection("users").document(uid).collection("goals").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to goal.title.take(120).ifBlank { "Goal" },
                        "description" to goal.description.take(400),
                        "targetEpochDay" to goal.targetEpochDay.coerceIn(0L, 100000L),
                        "currentProgress" to goal.currentProgress.coerceIn(0, 100000).toLong(),
                        "targetTotal" to goal.targetTotal.coerceIn(1, 100000).toLong(),
                        "status" to goal.status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to goal.title.take(120).ifBlank { "Goal" },
                        "description" to goal.description.take(400),
                        "targetEpochDay" to goal.targetEpochDay.coerceIn(0L, 100000L),
                        "currentProgress" to goal.currentProgress.coerceIn(0, 100000).toLong(),
                        "targetTotal" to goal.targetTotal.coerceIn(1, 100000).toLong(),
                        "status" to goal.status,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("goals").document(sanitizeDocId(goalId))
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertFocusSession(session: FocusSessionEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("focus_sessions")
            .document(sanitizeDocId(session.id))
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "completed" to session.completed,
                        "durationMinutes" to session.durationMinutes.coerceIn(1, 480).toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "durationMinutes" to session.durationMinutes.coerceIn(1, 480).toLong(),
                        "epochDay" to session.epochDay.coerceIn(0L, 100000L),
                        "completed" to session.completed,
                        "sessionType" to session.sessionType,
                        "startedAtMs" to session.startedAtMs.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertIfThenPlan(plan: IfThenPlanEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("if_then_plans")
            .document(sanitizeDocId(plan.id))
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "triggerText" to plan.triggerText.take(200).ifBlank { "Trigger" },
                        "responseText" to plan.responseText.take(300).ifBlank { "Response" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "triggerText" to plan.triggerText.take(200).ifBlank { "Trigger" },
                        "responseText" to plan.responseText.take(300).ifBlank { "Response" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteIfThenPlan(planId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("if_then_plans")
            .document(sanitizeDocId(planId))
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun pullAllUserDataFromCloud(userId: String): Result<CloudSnapshotBundle> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid != userId) return Result.failure(IllegalStateException("User mismatch"))
        return try {
            val userRef = db.collection("users").document(uid)
            val habitsSnap = userRef.collection("habits").get().await()
            val recordsSnap = userRef.collection("habit_records").get().await()
            val urgesSnap = userRef.collection("urge_records").get().await()
            val goalsSnap = userRef.collection("goals").get().await()
            val focusSnap = userRef.collection("focus_sessions").get().await()
            val plansSnap = userRef.collection("if_then_plans").get().await()

            val habits = habitsSnap.toObjects(
                FirestoreHabit::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { dto ->
                HabitEntity(
                    id = dto.localId.ifBlank { dto.id },
                    name = dto.name,
                    description = dto.description,
                    category = dto.category,
                    habitType = dto.habitType,
                    trackingMode = dto.trackingMode,
                    targetValue = dto.targetValue.toInt().coerceAtLeast(1),
                    unit = dto.unit,
                    iconKey = dto.iconKey,
                    colorHex = dto.colorHex,
                    scheduleType = dto.scheduleType,
                    scheduleDaysCsv = dto.scheduleDays.joinToString(","),
                    reminderEnabled = dto.reminderEnabled,
                    reminderTime = dto.reminderTime,
                    archived = dto.archived,
                    createdEpochDay = dto.createdEpochDay,
                    updatedAtMs = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            }

            val records = recordsSnap.toObjects(
                FirestoreHabitRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { dto ->
                HabitRecordEntity(
                    id = dto.id.ifBlank { "${dto.habitLocalId}_${dto.epochDay}" },
                    habitId = dto.habitLocalId,
                    epochDay = dto.epochDay,
                    status = dto.status,
                    progressValue = dto.progressValue.toInt().coerceAtLeast(0),
                    notes = dto.notes,
                    recordedTimestampMs = dto.recordedTimestampMs,
                    updatedAtMs = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            }

            val urges = urgesSnap.toObjects(
                FirestoreUrgeRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { dto ->
                UrgeRecordEntity(
                    id = dto.id,
                    habitId = dto.habitLocalId,
                    epochDay = dto.epochDay,
                    timeString = dto.timeString,
                    recordKind = dto.recordKind,
                    intensity = dto.intensity.toInt().coerceIn(1, 10),
                    notes = dto.notes,
                    recordedTimestampMs = dto.recordedTimestampMs
                )
            }

            val goals = goalsSnap.toObjects(
                FirestoreGoal::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { dto ->
                GoalEntity(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    targetEpochDay = dto.targetEpochDay,
                    currentProgress = dto.currentProgress.toInt().coerceAtLeast(0),
                    targetTotal = dto.targetTotal.toInt().coerceAtLeast(1),
                    status = dto.status,
                    createdAtMs = dto.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAtMs = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            }

            val sessions = focusSnap.toObjects(
                FirestoreFocusSession::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { dto ->
                FocusSessionEntity(
                    id = dto.id,
                    durationMinutes = dto.durationMinutes.toInt().coerceAtLeast(1),
                    epochDay = dto.epochDay,
                    completed = dto.completed,
                    sessionType = dto.sessionType,
                    startedAtMs = dto.startedAtMs
                )
            }

            val plans = plansSnap.toObjects(
                FirestoreIfThenPlan::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).map { dto ->
                IfThenPlanEntity(
                    id = dto.id,
                    triggerText = dto.triggerText,
                    responseText = dto.responseText,
                    createdAtMs = dto.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAtMs = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            }

            Result.success(
                CloudSnapshotBundle(
                    habits = habits,
                    records = records,
                    urges = urges,
                    goals = goals,
                    focusSessions = sessions,
                    ifThenPlans = plans
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid")
            Result.failure(e)
        }
    }
}

data class CloudSnapshotBundle(
    val habits: List<HabitEntity>,
    val records: List<HabitRecordEntity>,
    val urges: List<UrgeRecordEntity>,
    val goals: List<GoalEntity>,
    val focusSessions: List<FocusSessionEntity>,
    val ifThenPlans: List<IfThenPlanEntity>
)
