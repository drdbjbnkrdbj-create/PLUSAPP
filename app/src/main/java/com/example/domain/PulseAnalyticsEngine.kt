package com.example.domain

import com.example.data.local.AchievementEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.AppLanguage
import com.example.data.model.HabitType
import com.example.data.model.RecordStatus
import com.example.data.model.TrackingMode
import com.example.data.model.UrgeRecordKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

data class DailyHabitStatusItem(
    val habit: HabitEntity,
    val record: HabitRecordEntity?,
    val isCompletedOrClean: Boolean,
    val isMissedOrRelapse: Boolean
)

data class DailySummary(
    val date: LocalDate,
    val completionPercent: Int,
    val completedCount: Int,
    val attentionCount: Int,
    val buildCount: Int,
    val quitCount: Int,
    val scheduledTotal: Int,
    val items: List<DailyHabitStatusItem>
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val longestStartDate: LocalDate?,
    val longestEndDate: LocalDate?
)

data class HabitPerformanceRank(
    val habit: HabitEntity,
    val successCount: Int,
    val failureCount: Int,
    val scheduledDaysCount: Int,
    val completionRate: Int
)

data class PatternInsight(
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val isPositive: Boolean
)

data class PeriodAnalytics(
    val commitmentPercent: Int,
    val deltaFromPreviousPercent: Int,
    val totalSuccessfulDays: Int,
    val totalUnsuccessfulDays: Int,
    val longestStreak: Int,
    val longestStreakHabitName: String?,
    val longestStreakStart: LocalDate?,
    val longestStreakEnd: LocalDate?,
    val rankings: List<HabitPerformanceRank>,
    val patterns: List<PatternInsight>
)

data class WeeklyReviewSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val overallPercent: Int,
    val deltaFromPrevWeekPercent: Int,
    val successfulCount: Int,
    val unsuccessfulCount: Int,
    val bestHabit: HabitPerformanceRank?,
    val mostMissedHabit: HabitPerformanceRank?,
    val summaryEn: String,
    val summaryAr: String
)

object PulseAnalyticsEngine {

    fun isHabitScheduledOn(habit: HabitEntity, date: LocalDate): Boolean {
        if (habit.archived) return false
        val epochDay = date.toEpochDay()
        if (epochDay < habit.createdEpochDay) return false
        val dayValue = date.dayOfWeek.value // 1=Mon .. 7=Sun
        return habit.scheduleDaysList().contains(dayValue)
    }

    fun isSuccessfulRecord(habit: HabitEntity, record: HabitRecordEntity?): Boolean {
        if (record == null) return false
        return when (habit.habitTypeEnum()) {
            HabitType.BUILD -> {
                if (habit.trackingModeEnum() == TrackingMode.NUMERICAL) {
                    record.statusEnum() == RecordStatus.COMPLETED || record.progressValue >= habit.targetValue
                } else {
                    record.statusEnum() == RecordStatus.COMPLETED
                }
            }
            HabitType.QUIT -> record.statusEnum() == RecordStatus.CLEAN_DAY
        }
    }

    fun isUnsuccessfulRecord(habit: HabitEntity, record: HabitRecordEntity?): Boolean {
        if (record == null) return false
        return when (habit.habitTypeEnum()) {
            HabitType.BUILD -> record.statusEnum() == RecordStatus.MISSED
            HabitType.QUIT -> record.statusEnum() == RecordStatus.RELAPSE
        }
    }

    fun computeDailySummary(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        date: LocalDate
    ): DailySummary {
        val epochDay = date.toEpochDay()
        val dayRecords = records.filter { it.epochDay == epochDay }.associateBy { it.habitId }
        val scheduled = habits.filter { isHabitScheduledOn(it, date) }
        val items = scheduled.map { habit ->
            val rec = dayRecords[habit.id]
            DailyHabitStatusItem(
                habit = habit,
                record = rec,
                isCompletedOrClean = isSuccessfulRecord(habit, rec),
                isMissedOrRelapse = isUnsuccessfulRecord(habit, rec)
            )
        }
        val completed = items.count { it.isCompletedOrClean }
        val attention = items.count { !it.isCompletedOrClean }
        val buildCount = scheduled.count { it.habitTypeEnum() == HabitType.BUILD }
        val quitCount = scheduled.count { it.habitTypeEnum() == HabitType.QUIT }
        val percent = if (scheduled.isEmpty()) 0 else ((completed * 100f) / scheduled.size).roundToInt()

        return DailySummary(
            date = date,
            completionPercent = percent,
            completedCount = completed,
            attentionCount = attention,
            buildCount = buildCount,
            quitCount = quitCount,
            scheduledTotal = scheduled.size,
            items = items
        )
    }

    fun computeHabitStreak(
        habit: HabitEntity,
        habitRecords: List<HabitRecordEntity>,
        today: LocalDate
    ): StreakInfo {
        val recordByDay = habitRecords.filter { it.habitId == habit.id }.associateBy { it.epochDay }
        val startEpoch = habit.createdEpochDay
        val todayEpoch = today.toEpochDay()
        if (todayEpoch < startEpoch) {
            return StreakInfo(0, 0, null, null)
        }

        // Longest streak across scheduled days from creation up to max(today, max recorded day)
        val maxRecorded = recordByDay.keys.maxOrNull() ?: todayEpoch
        val endEpoch = maxOf(todayEpoch, maxRecorded)

        var longest = 0
        var longestStart: LocalDate? = null
        var longestEnd: LocalDate? = null
        var running = 0
        var runningStart: LocalDate? = null

        var d = startEpoch
        while (d <= endEpoch) {
            val localDate = LocalDate.ofEpochDay(d)
            if (isHabitScheduledOn(habit, localDate)) {
                val rec = recordByDay[d]
                if (isSuccessfulRecord(habit, rec)) {
                    if (running == 0) runningStart = localDate
                    running++
                    if (running >= longest) {
                        longest = running
                        longestStart = runningStart
                        longestEnd = localDate
                    }
                } else if (rec != null && rec.statusEnum() == RecordStatus.SKIPPED) {
                    // Skipped scheduled day neither increments nor breaks
                } else {
                    running = 0
                    runningStart = null
                }
            }
            d++
        }

        // Current active streak walking backward from today (allowing today to be pending)
        var current = 0
        var cursor = todayEpoch
        var checkedToday = false
        while (cursor >= startEpoch) {
            val localDate = LocalDate.ofEpochDay(cursor)
            if (isHabitScheduledOn(habit, localDate)) {
                val rec = recordByDay[cursor]
                if (isSuccessfulRecord(habit, rec)) {
                    current++
                } else if (!checkedToday && rec == null) {
                    // Today not yet logged; don't break yesterday's active streak
                } else if (rec != null && rec.statusEnum() == RecordStatus.SKIPPED) {
                    // Skip preserves streak
                } else {
                    break
                }
                checkedToday = true
            }
            cursor--
        }

        return StreakInfo(
            currentStreak = current,
            longestStreak = longest,
            longestStartDate = longestStart,
            longestEndDate = longestEnd
        )
    }

    fun computePeriodAnalytics(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        urgeRecords: List<UrgeRecordEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        prevStartDate: LocalDate,
        prevEndDate: LocalDate,
        today: LocalDate
    ): PeriodAnalytics {
        val activeHabits = habits.filter { !it.archived }
        val (currentPercent, totalSuccess, totalFailure, rankings) =
            computeRangeMetrics(activeHabits, records, startDate, endDate)
        val (prevPercent, _, _, _) =
            computeRangeMetrics(activeHabits, records, prevStartDate, prevEndDate)

        val delta = if (activeHabits.isEmpty()) 0 else currentPercent - prevPercent

        var bestStreak = 0
        var bestHabitName: String? = null
        var bestStart: LocalDate? = null
        var bestEnd: LocalDate? = null
        for (h in activeHabits) {
            val info = computeHabitStreak(h, records, today)
            if (info.longestStreak > bestStreak) {
                bestStreak = info.longestStreak
                bestHabitName = h.name
                bestStart = info.longestStartDate
                bestEnd = info.longestEndDate
            }
        }

        val patterns = detectPatterns(activeHabits, records, urgeRecords, startDate, endDate, delta)

        return PeriodAnalytics(
            commitmentPercent = currentPercent,
            deltaFromPreviousPercent = delta,
            totalSuccessfulDays = totalSuccess,
            totalUnsuccessfulDays = totalFailure,
            longestStreak = bestStreak,
            longestStreakHabitName = bestHabitName,
            longestStreakStart = bestStart,
            longestStreakEnd = bestEnd,
            rankings = rankings,
            patterns = patterns
        )
    }

    private data class RangeMetrics(
        val percent: Int,
        val totalSuccess: Int,
        val totalFailure: Int,
        val rankings: List<HabitPerformanceRank>
    )

    private fun computeRangeMetrics(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): RangeMetrics {
        if (habits.isEmpty() || endDate.isBefore(startDate)) {
            return RangeMetrics(0, 0, 0, emptyList())
        }
        val recordMap = records.associateBy { it.habitId to it.epochDay }
        var totalScheduled = 0
        var totalSuccess = 0
        var totalFailure = 0

        val rankings = habits.map { habit ->
            var hScheduled = 0
            var hSuccess = 0
            var hFailure = 0
            var d = startDate.toEpochDay()
            val endEpoch = endDate.toEpochDay()
            while (d <= endEpoch) {
                val date = LocalDate.ofEpochDay(d)
                if (isHabitScheduledOn(habit, date)) {
                    hScheduled++
                    val rec = recordMap[habit.id to d]
                    if (isSuccessfulRecord(habit, rec)) {
                        hSuccess++
                    } else if (isUnsuccessfulRecord(habit, rec)) {
                        hFailure++
                    }
                }
                d++
            }
            totalScheduled += hScheduled
            totalSuccess += hSuccess
            totalFailure += hFailure
            val rate = if (hScheduled == 0) 0 else ((hSuccess * 100f) / hScheduled).roundToInt()
            HabitPerformanceRank(
                habit = habit,
                successCount = hSuccess,
                failureCount = hFailure,
                scheduledDaysCount = hScheduled,
                completionRate = rate
            )
        }.sortedByDescending { it.completionRate }

        val overallPercent = if (totalScheduled == 0) 0 else ((totalSuccess * 100f) / totalScheduled).roundToInt()
        return RangeMetrics(overallPercent, totalSuccess, totalFailure, rankings)
    }

    private fun detectPatterns(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        urgeRecords: List<UrgeRecordEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        deltaPercent: Int
    ): List<PatternInsight> {
        if (habits.isEmpty() || records.isEmpty()) return emptyList()
        val insights = mutableListOf<PatternInsight>()
        val recordMap = records.associateBy { it.habitId to it.epochDay }
        val startEpoch = startDate.toEpochDay()
        val endEpoch = endDate.toEpochDay()

        // 1. Identify frequently missed habit in range
        val missCounts = habits.map { h ->
            var missed = 0
            var scheduled = 0
            var d = startEpoch
            while (d <= endEpoch) {
                val date = LocalDate.ofEpochDay(d)
                if (isHabitScheduledOn(h, date)) {
                    scheduled++
                    val rec = recordMap[h.id to d]
                    if (!isSuccessfulRecord(h, rec)) missed++
                }
                d++
            }
            Triple(h, missed, scheduled)
        }.filter { it.third > 0 && it.second > 0 }
            .maxByOrNull { it.second }

        if (missCounts != null && missCounts.second >= 2) {
            insights.add(
                PatternInsight(
                    titleEn = "Needs Attention: ${missCounts.first.name}",
                    titleAr = "عادة تحتاج متابعة: ${missCounts.first.name}",
                    descriptionEn = "Missed or incomplete on ${missCounts.second} scheduled day(s) in this period.",
                    descriptionAr = "لم تكتمل في ${missCounts.second} أيام مجدولة خلال هذه الفترة.",
                    isPositive = false
                )
            )
        }

        // 2. Recurring difficult day of week
        val missesByDow = mutableMapOf<DayOfWeek, Int>()
        var d = startEpoch
        while (d <= endEpoch) {
            val date = LocalDate.ofEpochDay(d)
            for (h in habits) {
                if (isHabitScheduledOn(h, date)) {
                    val rec = recordMap[h.id to d]
                    if (isUnsuccessfulRecord(h, rec) || rec == null) {
                        missesByDow[date.dayOfWeek] = (missesByDow[date.dayOfWeek] ?: 0) + 1
                    }
                }
            }
            d++
        }
        val hardestDow = missesByDow.maxByOrNull { it.value }
        if (hardestDow != null && hardestDow.value >= 2) {
            val dayNameEn = dayOfWeekName(hardestDow.key, AppLanguage.EN)
            val dayNameAr = dayOfWeekName(hardestDow.key, AppLanguage.AR)
            insights.add(
                PatternInsight(
                    titleEn = "Challenging Day Pattern: $dayNameEn",
                    titleAr = "يوم أكثر صعوبة: $dayNameAr",
                    descriptionEn = "$dayNameEn appears most frequently among missed or unlogged check-ins.",
                    descriptionAr = "يظهر يوم $dayNameAr بشكل متكرر ضمن الأيام غير المكتملة.",
                    isPositive = false
                )
            )
        }

        // 3. Time-of-day pattern from recorded timestamps
        val successfulRecordsInRange = records.filter {
            it.epochDay in startEpoch..endEpoch &&
                (it.statusEnum() == RecordStatus.COMPLETED || it.statusEnum() == RecordStatus.CLEAN_DAY)
        }
        if (successfulRecordsInRange.size >= 3) {
            val buckets = successfulRecordsInRange.groupingBy { rec ->
                val hour = Instant.ofEpochMilli(rec.recordedTimestampMs)
                    .atZone(ZoneId.systemDefault())
                    .hour
                when (hour) {
                    in 5..11 -> "MORNING"
                    in 12..16 -> "AFTERNOON"
                    in 17..21 -> "EVENING"
                    else -> "NIGHT"
                }
            }.eachCount()
            val topBucket = buckets.maxByOrNull { it.value }
            if (topBucket != null) {
                val (labelEn, labelAr) = when (topBucket.key) {
                    "MORNING" -> "Morning (5 AM – 12 PM)" to "الصباح (5 ص - 12 م)"
                    "AFTERNOON" -> "Afternoon (12 PM – 5 PM)" to "الظهيرة (12 م - 5 م)"
                    "EVENING" -> "Evening (5 PM – 10 PM)" to "المساء (5 م - 10 م)"
                    else -> "Late Night" to "الليل"
                }
                insights.add(
                    PatternInsight(
                        titleEn = "Peak Check-in Window: $labelEn",
                        titleAr = "أكثر أوقات الالتزام: $labelAr",
                        descriptionEn = "${topBucket.value} successful check-ins were logged during this time window.",
                        descriptionAr = "تم تسجيل ${topBucket.value} إنجازات ناجحة في هذه الفترة الزمنية.",
                        isPositive = true
                    )
                )
            }
        }

        // 4. Urge resistance vs relapse trend
        val urgesInRange = urgeRecords.filter { it.epochDay in startEpoch..endEpoch }
        if (urgesInRange.isNotEmpty()) {
            val resisted = urgesInRange.count { it.kindEnum() == UrgeRecordKind.URGE_RESISTED }
            insights.add(
                PatternInsight(
                    titleEn = "Urge Resilience",
                    titleAr = "مقاومة الرغبة",
                    descriptionEn = "Logged $resisted resisted urge(s) out of ${urgesInRange.size} recorded urge event(s).",
                    descriptionAr = "تمت مقاومة $resisted رغبة من أصل ${urgesInRange.size} حالة مسجلة.",
                    isPositive = resisted >= (urgesInRange.size - resisted)
                )
            )
        }

        // 5. Recent trend shift
        if (deltaPercent != 0) {
            val sign = if (deltaPercent > 0) "+" else ""
            insights.add(
                PatternInsight(
                    titleEn = if (deltaPercent > 0) "Upward Momentum" else "Recent Dip Observed",
                    titleAr = if (deltaPercent > 0) "تحسن في الالتزام" else "تراجع ملحوظ مؤخراً",
                    descriptionEn = "Completion rate shifted by $sign$deltaPercent% compared with the previous period.",
                    descriptionAr = "تغيرت نسبة الالتزام بمقدار $sign$deltaPercent% مقارنة بالفترة السابقة.",
                    isPositive = deltaPercent > 0
                )
            )
        }

        return insights
    }

    fun computeWeeklyReview(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        weekStart: LocalDate
    ): WeeklyReviewSummary {
        val weekEnd = weekStart.plusDays(6)
        val prevStart = weekStart.minusDays(7)
        val prevEnd = weekStart.minusDays(1)
        val activeHabits = habits.filter { !it.archived }

        val current = computeRangeMetrics(activeHabits, records, weekStart, weekEnd)
        val previous = computeRangeMetrics(activeHabits, records, prevStart, prevEnd)
        val delta = if (activeHabits.isEmpty()) 0 else current.percent - previous.percent

        val bestHabit = current.rankings.firstOrNull { it.scheduledDaysCount > 0 }
        val mostMissed = current.rankings
            .filter { it.scheduledDaysCount > 0 }
            .maxByOrNull { (it.scheduledDaysCount - it.successCount) }
            ?.takeIf { (it.scheduledDaysCount - it.successCount) > 0 }

        val summaryEn = when {
            activeHabits.isEmpty() -> "Create habits to generate weekly discipline insights."
            current.percent >= 80 -> "Strong consistency this week (${current.percent}%). ${bestHabit?.habit?.name ?: "Your routine"} led your progress."
            current.percent >= 50 -> "Steady week at ${current.percent}%. Focusing on ${mostMissed?.habit?.name ?: "missed days"} can lift next week."
            else -> "A lighter week (${current.percent}%). Restart small with one key habit tomorrow."
        }

        val summaryAr = when {
            activeHabits.isEmpty() -> "أضف عاداتك لتظهر خلاصة المراجعة الأسبوعية تلقائياً."
            current.percent >= 80 -> "أداء قوي هذا الأسبوع بنسبة ${current.percent}%. كانت عادة ${bestHabit?.habit?.name ?: ""} الأفضل التزاماً."
            current.percent >= 50 -> "أسبوع مستقر بنسبة ${current.percent}%. التركيز على ${mostMissed?.habit?.name ?: "الأيام الفائتة"} سيرفع النتيجة."
            else -> "أسبوع هادئ بنسبة ${current.percent}%. ابدأ بخطوة صغيرة غداً وحافظ على الاستمرارية."
        }

        return WeeklyReviewSummary(
            weekStart = weekStart,
            weekEnd = weekEnd,
            overallPercent = current.percent,
            deltaFromPrevWeekPercent = delta,
            successfulCount = current.totalSuccess,
            unsuccessfulCount = current.totalFailure,
            bestHabit = bestHabit,
            mostMissedHabit = mostMissed,
            summaryEn = summaryEn,
            summaryAr = summaryAr
        )
    }

    fun evaluateAchievements(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        focusSessions: List<FocusSessionEntity>,
        urgeRecords: List<UrgeRecordEntity>,
        existing: List<AchievementEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): List<AchievementEntity> {
        val existingMap = existing.associateBy { it.key }
        val habitMap = habits.associateBy { it.id }

        // Total distinct successful habit completions & total unique successful days
        val successfulRecords = records.filter { rec ->
            val h = habitMap[rec.habitId]
            h != null && isSuccessfulRecord(h, rec)
        }
        val uniqueSuccessfulDays = successfulRecords.map { it.epochDay }.distinct().size
        val completedFocusCount = focusSessions.count { it.completed }
        val resistedUrgeCount = urgeRecords.count { it.kindEnum() == UrgeRecordKind.URGE_RESISTED }

        data class Spec(val key: String, val current: Int, val target: Int)

        val specs = listOf(
            Spec("first_habit", habits.size.coerceAtMost(1), 1),
            Spec("days_7", uniqueSuccessfulDays, 7),
            Spec("days_30", uniqueSuccessfulDays, 30),
            Spec("days_60", uniqueSuccessfulDays, 60),
            Spec("days_90", uniqueSuccessfulDays, 90),
            Spec("focus_5", completedFocusCount, 5),
            Spec("urge_3", resistedUrgeCount, 3)
        )

        return specs.map { spec ->
            val prev = existingMap[spec.key]
            // Preserve unlockedAtMs forever once earned; never remove due to relapse
            val unlockedAt = prev?.unlockedAtMs ?: if (spec.current >= spec.target) nowMs else null
            AchievementEntity(
                key = spec.key,
                unlockedAtMs = unlockedAt,
                progressCurrent = spec.current.coerceAtMost(spec.target),
                progressTarget = spec.target
            )
        }
    }

    fun monthCalendarDays(yearMonth: YearMonth): List<LocalDate?> {
        val firstDay = yearMonth.atDay(1)
        // Week starts on Saturday (matching Arabic calendar in reference screenshot!) or Sunday
        // Saturday = 6 in ISO DayOfWeek (Mon=1..Sun=7). Offset from Saturday: (dayOfWeek % 7 + 1) % 7
        val leadingEmpty = when (firstDay.dayOfWeek) {
            DayOfWeek.SATURDAY -> 0
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
        }
        val daysInMonth = yearMonth.lengthOfMonth()
        val cells = mutableListOf<LocalDate?>()
        repeat(leadingEmpty) { cells.add(null) }
        for (day in 1..daysInMonth) {
            cells.add(yearMonth.atDay(day))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    fun dayOfWeekName(dow: DayOfWeek, lang: AppLanguage): String = when (lang) {
        AppLanguage.AR -> when (dow) {
            DayOfWeek.SATURDAY -> "السبت"
            DayOfWeek.SUNDAY -> "الأحد"
            DayOfWeek.MONDAY -> "الاثنين"
            DayOfWeek.TUESDAY -> "الثلاثاء"
            DayOfWeek.WEDNESDAY -> "الأربعاء"
            DayOfWeek.THURSDAY -> "الخميس"
            DayOfWeek.FRIDAY -> "الجمعة"
        }
        AppLanguage.EN -> when (dow) {
            DayOfWeek.SATURDAY -> "Saturday"
            DayOfWeek.SUNDAY -> "Sunday"
            DayOfWeek.MONDAY -> "Monday"
            DayOfWeek.TUESDAY -> "Tuesday"
            DayOfWeek.WEDNESDAY -> "Wednesday"
            DayOfWeek.THURSDAY -> "Thursday"
            DayOfWeek.FRIDAY -> "Friday"
        }
    }
}
