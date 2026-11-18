package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.FocusSessionType
import com.example.data.model.GoalStatus
import com.example.data.model.HabitCategory
import com.example.data.model.HabitType
import com.example.data.model.RecordStatus
import com.example.data.model.ScheduleType
import com.example.data.model.TrackingMode
import com.example.data.model.UrgeRecordKind

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val category: String = HabitCategory.HEALTH.name,
    val habitType: String = HabitType.BUILD.name,
    val trackingMode: String = TrackingMode.BOOLEAN.name,
    val targetValue: Int = 1,
    val unit: String = "",
    val iconKey: String = "check",
    val colorHex: String = "#27C6D9",
    val scheduleType: String = ScheduleType.DAILY.name,
    val scheduleDaysCsv: String = "1,2,3,4,5,6,7",
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "09:00",
    val archived: Boolean = false,
    val createdEpochDay: Long,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun categoryEnum(): HabitCategory = HabitCategory.fromNameOrDefault(category)
    fun habitTypeEnum(): HabitType = HabitType.fromNameOrDefault(habitType)
    fun trackingModeEnum(): TrackingMode = TrackingMode.fromNameOrDefault(trackingMode)
    fun scheduleTypeEnum(): ScheduleType = ScheduleType.fromNameOrDefault(scheduleType)

    fun scheduleDaysList(): List<Int> =
        if (scheduleTypeEnum() == ScheduleType.DAILY) {
            listOf(1, 2, 3, 4, 5, 6, 7)
        } else {
            scheduleDaysCsv.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .distinct()
                .sorted()
        }
}

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "epochDay"], unique = true),
        Index(value = ["epochDay"])
    ]
)
data class HabitRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val status: String = RecordStatus.COMPLETED.name,
    val progressValue: Int = 1,
    val notes: String = "",
    val recordedTimestampMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun statusEnum(): RecordStatus = RecordStatus.fromNameOrDefault(status)
}

@Entity(
    tableName = "urge_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["epochDay"])
    ]
)
data class UrgeRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String? = null,
    val epochDay: Long,
    val timeString: String,
    val recordKind: String = UrgeRecordKind.URGE_RESISTED.name,
    val intensity: Int = 5,
    val notes: String = "",
    val recordedTimestampMs: Long = System.currentTimeMillis()
) {
    fun kindEnum(): UrgeRecordKind = UrgeRecordKind.fromNameOrDefault(recordKind)
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val targetEpochDay: Long,
    val currentProgress: Int = 0,
    val targetTotal: Int = 1,
    val status: String = GoalStatus.ACTIVE.name,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun statusEnum(): GoalStatus = GoalStatus.fromNameOrDefault(status)
}

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val durationMinutes: Int,
    val epochDay: Long,
    val completed: Boolean = true,
    val sessionType: String = FocusSessionType.FOCUS.name,
    val startedAtMs: Long = System.currentTimeMillis()
) {
    fun sessionTypeEnum(): FocusSessionType = FocusSessionType.fromNameOrDefault(sessionType)
}

@Entity(tableName = "if_then_plans")
data class IfThenPlanEntity(
    @PrimaryKey val id: String,
    val triggerText: String,
    val responseText: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val unlockedAtMs: Long? = null,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1
)
