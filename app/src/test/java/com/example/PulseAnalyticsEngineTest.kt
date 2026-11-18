package com.example

import com.example.data.local.AchievementEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.FocusSessionType
import com.example.data.model.HabitCategory
import com.example.data.model.HabitType
import com.example.data.model.RecordStatus
import com.example.data.model.ScheduleType
import com.example.data.model.TrackingMode
import com.example.data.model.UrgeRecordKind
import com.example.domain.PulseAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class PulseAnalyticsEngineTest {

    private fun buildHabit(
        id: String = "h_build",
        name: String = "Read 20 pages",
        type: HabitType = HabitType.BUILD,
        mode: TrackingMode = TrackingMode.BOOLEAN,
        target: Int = 1,
        scheduleType: ScheduleType = ScheduleType.DAILY,
        scheduleDaysCsv: String = "1,2,3,4,5,6,7",
        createdDate: LocalDate = LocalDate.of(2026, 1, 1)
    ) = HabitEntity(
        id = id,
        name = name,
        category = HabitCategory.LEARNING.name,
        habitType = type.name,
        trackingMode = mode.name,
        targetValue = target,
        scheduleType = scheduleType.name,
        scheduleDaysCsv = scheduleDaysCsv,
        createdEpochDay = createdDate.toEpochDay()
    )

    @Test
    fun `daily summary handles empty, boolean build, numerical build, and quit habits accurately`() {
        val date = LocalDate.of(2026, 2, 10)
        val epoch = date.toEpochDay()

        val empty = PulseAnalyticsEngine.computeDailySummary(emptyList(), emptyList(), date)
        assertEquals(0, empty.completionPercent)
        assertEquals(0, empty.scheduledTotal)

        val readHabit = buildHabit(id = "h1", name = "Read", type = HabitType.BUILD, mode = TrackingMode.BOOLEAN)
        val waterHabit = buildHabit(id = "h2", name = "Water", type = HabitType.BUILD, mode = TrackingMode.NUMERICAL, target = 8)
        val socialQuit = buildHabit(id = "h3", name = "No Social", type = HabitType.QUIT, mode = TrackingMode.BOOLEAN)

        val records = listOf(
            HabitRecordEntity(id = "r1", habitId = "h1", epochDay = epoch, status = RecordStatus.COMPLETED.name, progressValue = 1),
            // Numerical progress reaches target (8/8)
            HabitRecordEntity(id = "r2", habitId = "h2", epochDay = epoch, status = RecordStatus.COMPLETED.name, progressValue = 8),
            // Quit habit had a relapse
            HabitRecordEntity(id = "r3", habitId = "h3", epochDay = epoch, status = RecordStatus.RELAPSE.name, progressValue = 0)
        )

        val summary = PulseAnalyticsEngine.computeDailySummary(
            habits = listOf(readHabit, waterHabit, socialQuit),
            records = records,
            date = date
        )
        assertEquals(3, summary.scheduledTotal)
        assertEquals(2, summary.completedCount)
        assertEquals(1, summary.attentionCount)
        assertEquals(2, summary.buildCount)
        assertEquals(1, summary.quitCount)
        assertEquals(67, summary.completionPercent)
    }

    @Test
    fun `numerical habit below target is not counted as successful`() {
        val date = LocalDate.of(2026, 2, 10)
        val waterHabit = buildHabit(id = "h_water", mode = TrackingMode.NUMERICAL, target = 8)
        val partialRecord = HabitRecordEntity(
            id = "r_partial",
            habitId = "h_water",
            epochDay = date.toEpochDay(),
            status = RecordStatus.MISSED.name,
            progressValue = 5
        )
        assertFalse(PulseAnalyticsEngine.isSuccessfulRecord(waterHabit, partialRecord))

        val completedViaProgress = partialRecord.copy(progressValue = 8)
        assertTrue(PulseAnalyticsEngine.isSuccessfulRecord(waterHabit, completedViaProgress))
    }

    @Test
    fun `streak calculation preserves longest streak across relapse and handles custom schedules and skips`() {
        // Scheduled Mon (1), Wed (3), Fri (5) starting Feb 2, 2026 (Monday)
        val start = LocalDate.of(2026, 2, 2)
        val customHabit = buildHabit(
            id = "h_gym",
            name = "Gym",
            scheduleType = ScheduleType.CUSTOM_DAYS,
            scheduleDaysCsv = "1,3,5",
            createdDate = start
        )

        // Streak 1: Mon Feb 2, Wed Feb 4, Fri Feb 6 -> length 3
        // Skip Mon Feb 9 -> preserves streak!
        // Wed Feb 11 -> length 4!
        // Missed Fri Feb 13 -> resets current streak to 0
        // Wed Feb 18 -> new streak length 1 (checking on Thu Feb 19 where today is not scheduled)
        val records = listOf(
            HabitRecordEntity("r1", "h_gym", LocalDate.of(2026, 2, 2).toEpochDay(), RecordStatus.COMPLETED.name, 1),
            HabitRecordEntity("r2", "h_gym", LocalDate.of(2026, 2, 4).toEpochDay(), RecordStatus.COMPLETED.name, 1),
            HabitRecordEntity("r3", "h_gym", LocalDate.of(2026, 2, 6).toEpochDay(), RecordStatus.COMPLETED.name, 1),
            HabitRecordEntity("r4", "h_gym", LocalDate.of(2026, 2, 9).toEpochDay(), RecordStatus.SKIPPED.name, 0),
            HabitRecordEntity("r5", "h_gym", LocalDate.of(2026, 2, 11).toEpochDay(), RecordStatus.COMPLETED.name, 1),
            HabitRecordEntity("r6", "h_gym", LocalDate.of(2026, 2, 13).toEpochDay(), RecordStatus.MISSED.name, 0),
            HabitRecordEntity("r7", "h_gym", LocalDate.of(2026, 2, 18).toEpochDay(), RecordStatus.COMPLETED.name, 1)
        )

        val info = PulseAnalyticsEngine.computeHabitStreak(customHabit, records, LocalDate.of(2026, 2, 19))
        assertEquals(4, info.longestStreak)
        assertEquals(1, info.currentStreak)
        assertEquals(LocalDate.of(2026, 2, 2), info.longestStartDate)
        assertEquals(LocalDate.of(2026, 2, 11), info.longestEndDate)
    }

    @Test
    fun `monthly analytics computes comparisons, rankings, and data-driven patterns`() {
        val created = LocalDate.of(2026, 1, 1)
        val habitA = buildHabit(id = "ha", name = "Meditation", createdDate = created)
        val habitB = buildHabit(id = "hb", name = "No Sugar", type = HabitType.QUIT, createdDate = created)

        val records = mutableListOf<HabitRecordEntity>()
        val morningTs = ZonedDateTime.of(2026, 2, 5, 8, 30, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()

        // January (prev): 2 days total logged across Jan 1..Jan 31 -> low rate
        records.add(HabitRecordEntity("ja1", "ha", LocalDate.of(2026, 1, 10).toEpochDay(), RecordStatus.COMPLETED.name, 1, recordedTimestampMs = morningTs))
        records.add(HabitRecordEntity("jb1", "hb", LocalDate.of(2026, 1, 10).toEpochDay(), RecordStatus.CLEAN_DAY.name, 1, recordedTimestampMs = morningTs))

        // February 1..7: habitA completed 6/7 (misses Thursday Feb 5), habitB missed/relapsed twice (Thu Feb 5 and Fri Feb 6)
        for (day in 1..7) {
            val d = LocalDate.of(2026, 2, day)
            val aStatus = if (d.dayOfWeek == DayOfWeek.THURSDAY) RecordStatus.MISSED else RecordStatus.COMPLETED
            val bStatus = if (d.dayOfWeek == DayOfWeek.THURSDAY || d.dayOfWeek == DayOfWeek.FRIDAY) RecordStatus.RELAPSE else RecordStatus.CLEAN_DAY
            records.add(
                HabitRecordEntity(
                    id = "fa_$day",
                    habitId = "ha",
                    epochDay = d.toEpochDay(),
                    status = aStatus.name,
                    progressValue = if (aStatus == RecordStatus.COMPLETED) 1 else 0,
                    recordedTimestampMs = morningTs
                )
            )
            records.add(
                HabitRecordEntity(
                    id = "fb_$day",
                    habitId = "hb",
                    epochDay = d.toEpochDay(),
                    status = bStatus.name,
                    progressValue = if (bStatus == RecordStatus.CLEAN_DAY) 1 else 0,
                    recordedTimestampMs = morningTs
                )
            )
        }

        val urges = listOf(
            UrgeRecordEntity("u1", "hb", LocalDate.of(2026, 2, 4).toEpochDay(), "16:00", UrgeRecordKind.URGE_RESISTED.name, 7)
        )

        val analytics = PulseAnalyticsEngine.computePeriodAnalytics(
            habits = listOf(habitA, habitB),
            records = records,
            urgeRecords = urges,
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 2, 7),
            prevStartDate = LocalDate.of(2026, 1, 1),
            prevEndDate = LocalDate.of(2026, 1, 7),
            today = LocalDate.of(2026, 2, 7)
        )

        // Total scheduled = 14 (7 * 2). Successful = 6 + 5 = 11 -> 79%
        assertEquals(79, analytics.commitmentPercent)
        assertEquals(11, analytics.totalSuccessfulDays)
        assertEquals(3, analytics.totalUnsuccessfulDays)
        assertTrue(analytics.deltaFromPreviousPercent > 0)
        assertEquals("Meditation", analytics.rankings.first().habit.name)
        assertTrue(analytics.patterns.any { it.titleEn.contains("No Sugar") || it.titleEn.contains("Thursday") })
        assertTrue(analytics.patterns.any { it.titleEn.contains("Morning") })
        assertTrue(analytics.patterns.any { it.titleEn.contains("Urge Resilience") })
    }

    @Test
    fun `weekly review picks best habit and most missed habit accurately`() {
        val weekStart = LocalDate.of(2026, 2, 7) // Saturday
        val h1 = buildHabit(id = "h1", name = "Quran", createdDate = weekStart)
        val h2 = buildHabit(id = "h2", name = "Workout", createdDate = weekStart)

        val records = (0L..6L).flatMap { offset ->
            val day = weekStart.plusDays(offset).toEpochDay()
            listOf(
                HabitRecordEntity("r1_$offset", "h1", day, RecordStatus.COMPLETED.name, 1),
                HabitRecordEntity("r2_$offset", "h2", day, if (offset < 3) RecordStatus.COMPLETED.name else RecordStatus.MISSED.name, if (offset < 3) 1 else 0)
            )
        }

        val review = PulseAnalyticsEngine.computeWeeklyReview(listOf(h1, h2), records, weekStart)
        assertEquals("Quran", review.bestHabit?.habit?.name)
        assertEquals(100, review.bestHabit?.completionRate)
        assertEquals("Workout", review.mostMissedHabit?.habit?.name)
        assertEquals(10, review.successfulCount)
        assertEquals(4, review.unsuccessfulCount)
    }

    @Test
    fun `achievements stay permanently unlocked even if user later relapses or misses days`() {
        val created = LocalDate.of(2026, 1, 1)
        val habit = buildHabit(id = "hq", type = HabitType.QUIT, createdDate = created)

        val sevenDaysRecords = (0L..6L).map { offset ->
            HabitRecordEntity(
                id = "rec_$offset",
                habitId = "hq",
                epochDay = created.plusDays(offset).toEpochDay(),
                status = RecordStatus.CLEAN_DAY.name,
                progressValue = 1
            )
        }
        val firstEval = PulseAnalyticsEngine.evaluateAchievements(
            habits = listOf(habit),
            records = sevenDaysRecords,
            focusSessions = listOf(
                FocusSessionEntity("fs1", 25, created.toEpochDay(), true, FocusSessionType.FOCUS.name)
            ),
            urgeRecords = emptyList(),
            existing = emptyList(),
            nowMs = 123456789L
        )
        val days7Unlocked = firstEval.first { it.key == "days_7" }
        assertNotNull(days7Unlocked.unlockedAtMs)
        assertEquals(123456789L, days7Unlocked.unlockedAtMs)

        // Simulate clearing records or adding relapse: days_7 timestamp must remain preserved
        val relapseEval = PulseAnalyticsEngine.evaluateAchievements(
            habits = listOf(habit),
            records = listOf(
                HabitRecordEntity("relapse", "hq", created.plusDays(7).toEpochDay(), RecordStatus.RELAPSE.name, 0)
            ),
            focusSessions = emptyList(),
            urgeRecords = emptyList(),
            existing = firstEval,
            nowMs = 999999999L
        )
        val preserved = relapseEval.first { it.key == "days_7" }
        assertEquals(123456789L, preserved.unlockedAtMs)
    }

    @Test
    fun `month calendar starts on Saturday and covers all days of month`() {
        // November 2024 starts on Friday -> Saturday(0), Sun(1), Mon(2), Tue(3), Wed(4), Thu(5), Fri(6) => 6 leading nulls
        val cells = PulseAnalyticsEngine.monthCalendarDays(YearMonth.of(2024, 11))
        assertEquals(0, cells.size % 7)
        assertEquals(30, cells.filterNotNull().size)
        assertEquals(LocalDate.of(2024, 11, 1), cells[6])
    }
}
