package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitRecordEntity::class,
        UrgeRecordEntity::class,
        GoalEntity::class,
        FocusSessionEntity::class,
        IfThenPlanEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun urgeRecordDao(): UrgeRecordDao
    abstract fun goalDao(): GoalDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun ifThenPlanDao(): IfThenPlanDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        private const val DATABASE_NAME = "pulse_habits.db"

        /**
         * Explicit non-destructive migration scaffold for future schema upgrades.
         * Never use fallbackToDestructiveMigration() so user habit history is never erased.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reserved for non-destructive v1 -> v2 schema additions
            }
        }

        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
