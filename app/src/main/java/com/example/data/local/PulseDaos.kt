package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdEpochDay ASC, name ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdEpochDay ASC, name ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeHabitById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE reminderEnabled = 1 AND archived = 0")
    suspend fun getReminderEnabledHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}

@Dao
interface HabitRecordDao {
    @Query("SELECT * FROM habit_records ORDER BY epochDay DESC")
    fun observeAllRecords(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE epochDay = :epochDay")
    fun observeRecordsForDay(epochDay: Long): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY epochDay DESC")
    fun observeRecordsForHabit(habitId: String): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getRecord(habitId: String, epochDay: Long): HabitRecordEntity?

    @Query("SELECT * FROM habit_records")
    suspend fun getAllRecordsOnce(): List<HabitRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: HabitRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecords(records: List<HabitRecordEntity>)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteRecord(habitId: String, epochDay: Long)

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllRecords()
}

@Dao
interface UrgeRecordDao {
    @Query("SELECT * FROM urge_records ORDER BY recordedTimestampMs DESC")
    fun observeAllUrgeRecords(): Flow<List<UrgeRecordEntity>>

    @Query("SELECT * FROM urge_records WHERE habitId = :habitId ORDER BY recordedTimestampMs DESC")
    fun observeUrgesForHabit(habitId: String): Flow<List<UrgeRecordEntity>>

    @Query("SELECT * FROM urge_records")
    suspend fun getAllUrgeRecordsOnce(): List<UrgeRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUrgeRecord(record: UrgeRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUrgeRecords(records: List<UrgeRecordEntity>)

    @Query("DELETE FROM urge_records WHERE id = :id")
    suspend fun deleteUrgeRecordById(id: String)

    @Query("DELETE FROM urge_records")
    suspend fun deleteAllUrgeRecords()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY targetEpochDay ASC, createdAtMs DESC")
    fun observeAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsOnce(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMs DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE epochDay = :epochDay ORDER BY startedAtMs DESC")
    fun observeSessionsForDay(epochDay: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllSessionsOnce(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface IfThenPlanDao {
    @Query("SELECT * FROM if_then_plans ORDER BY createdAtMs DESC")
    fun observeAllPlans(): Flow<List<IfThenPlanEntity>>

    @Query("SELECT * FROM if_then_plans")
    suspend fun getAllPlansOnce(): List<IfThenPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: IfThenPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlans(plans: List<IfThenPlanEntity>)

    @Query("DELETE FROM if_then_plans WHERE id = :id")
    suspend fun deletePlanById(id: String)

    @Query("DELETE FROM if_then_plans")
    suspend fun deleteAllPlans()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(achievements: List<AchievementEntity>)

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()
}
