package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val themeMode: String = "DARK",
    val languageCode: String = "en",
    val notificationsEnabled: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreHabit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val localId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "OTHER",
    val habitType: String = "BUILD",
    val trackingMode: String = "BOOLEAN",
    val targetValue: Long = 1L,
    val unit: String = "",
    val iconKey: String = "check",
    val colorHex: String = "#27C6D9",
    val scheduleType: String = "DAILY",
    val scheduleDays: List<Long> = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "09:00",
    val archived: Boolean = false,
    val createdEpochDay: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreHabitRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitLocalId: String = "",
    val epochDay: Long = 0L,
    val status: String = "COMPLETED",
    val progressValue: Long = 1L,
    val notes: String = "",
    val recordedTimestampMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreUrgeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitLocalId: String? = null,
    val epochDay: Long = 0L,
    val timeString: String = "",
    val recordKind: String = "URGE_RESISTED",
    val intensity: Long = 5L,
    val notes: String = "",
    val recordedTimestampMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val targetEpochDay: Long = 0L,
    val currentProgress: Long = 0L,
    val targetTotal: Long = 1L,
    val status: String = "ACTIVE",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreFocusSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val durationMinutes: Long = 25L,
    val epochDay: Long = 0L,
    val completed: Boolean = true,
    val sessionType: String = "FOCUS",
    val startedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreIfThenPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val triggerText: String = "",
    val responseText: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
