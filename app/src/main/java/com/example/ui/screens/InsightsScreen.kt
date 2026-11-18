package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.AppLanguage
import com.example.domain.HabitPerformanceRank
import com.example.domain.PulseAnalyticsEngine
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.HabitIconBadge
import com.example.ui.components.PulseCard
import com.example.ui.components.PulseEmptyState
import com.example.ui.i18n.formatLocalizedShortDate
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import java.time.LocalDate

@Composable
fun InsightsScreen(
    habits: List<HabitEntity>,
    records: List<HabitRecordEntity>,
    urges: List<UrgeRecordEntity>,
    onOpenWeeklyReview: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val today = remember { LocalDate.now() }

    // 0 = Today, 1 = Week, 2 = Month
    var periodTab by remember { mutableIntStateOf(2) }

    val (start, end, prevStart, prevEnd) = remember(periodTab, today) {
        when (periodTab) {
            0 -> arrayOf(today, today, today.minusDays(1), today.minusDays(1))
            1 -> arrayOf(today.minusDays(6), today, today.minusDays(13), today.minusDays(7))
            else -> arrayOf(today.minusDays(29), today, today.minusDays(59), today.minusDays(30))
        }
    }

    val analytics = remember(habits, records, urges, periodTab, today) {
        PulseAnalyticsEngine.computePeriodAnalytics(
            habits = habits,
            records = records,
            urgeRecords = urges,
            startDate = start,
            endDate = end,
            prevStartDate = prevStart,
            prevEndDate = prevEnd,
            today = today
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.insightsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onOpenWeeklyReview,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.cardBackground,
                            contentColor = colors.primaryAccent
                        ),
                        modifier = Modifier.testTag("insights_weekly_review_btn")
                    ) {
                        Icon(Icons.Rounded.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.openWeeklyReview)
                    }
                }
            }

            // Period Selector (Today / Week / Month)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.secondaryBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        0 to strings.todayTab,
                        1 to strings.weekTab,
                        2 to strings.monthTab
                    ).forEach { (idx, label) ->
                        val selected = periodTab == idx
                        Surface(
                            onClick = { periodTab = idx },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("insights_period_$idx"),
                            shape = RoundedCornerShape(50),
                            color = if (selected) colors.primaryAccent else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color(0xFF0A1420) else colors.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (habits.isEmpty()) {
                item {
                    PulseEmptyState(
                        icon = Icons.Rounded.AutoGraph,
                        title = strings.insightsTitle,
                        subtitle = strings.noDataYet
                    )
                }
            } else {
                // Commitment Rate Card
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CircularProgressRing(
                                progress = analytics.commitmentPercent / 100f,
                                size = 104.dp,
                                strokeWidth = 10.dp,
                                activeColor = colors.primaryAccent
                            ) {
                                Text(
                                    text = "${analytics.commitmentPercent}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.commitmentRate,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                val deltaColor = if (analytics.deltaFromPreviousPercent >= 0) colors.success else colors.failure
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (analytics.deltaFromPreviousPercent >= 0) {
                                            Icons.Rounded.TrendingUp
                                        } else {
                                            Icons.Rounded.TrendingDown
                                        },
                                        contentDescription = null,
                                        tint = deltaColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.comparedToPrevious(analytics.deltaFromPreviousPercent),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = deltaColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Habit Performance Grouped Bar Chart
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.habitPerformance,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LegendDot(colors.primaryAccent, strings.successLegend)
                                LegendDot(colors.failure, strings.missedLegend)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        HabitPerformanceBarChart(
                            rankings = analytics.rankings.take(6),
                            successColor = colors.primaryAccent,
                            failureColor = colors.failure
                        )
                    }
                }

                // Longest Successful Streak Card + Totals Row
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colors.success.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = colors.success
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.longestSuccessStreak,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = strings.daysCount(analytics.longestStreak),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (analytics.longestStreakStart != null && analytics.longestStreakEnd != null) {
                                        Text(
                                            text = "${analytics.longestStreakHabitName ?: ""} • ${formatLocalizedShortDate(analytics.longestStreakStart, strings.language)} - ${formatLocalizedShortDate(analytics.longestStreakEnd, strings.language)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PulseCard(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${analytics.totalSuccessfulDays}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.success,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.totalSuccessfulDays,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                        PulseCard(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${analytics.totalUnsuccessfulDays}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.failure,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.totalUnsuccessfulDays,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Pattern Detection Insights
                if (analytics.patterns.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.patternInsightsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(analytics.patterns) { insight ->
                        val title = if (strings.language == AppLanguage.AR) insight.titleAr else insight.titleEn
                        val desc = if (strings.language == AppLanguage.AR) insight.descriptionAr else insight.descriptionEn
                        val accent = if (insight.isPositive) colors.success else colors.warning
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Habit Performance Rankings
                item {
                    Text(
                        text = strings.habitRankingsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(analytics.rankings, key = { it.habit.id }) { rank ->
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HabitIconBadge(
                                iconKey = rank.habit.iconKey,
                                colorHex = rank.habit.colorHex,
                                badgeSize = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = rank.habit.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${rank.completionRate}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.primaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { rank.completionRate / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = colors.primaryAccent,
                                    trackColor = colors.secondaryBackground
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = LocalPulseColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun HabitPerformanceBarChart(
    rankings: List<HabitPerformanceRank>,
    successColor: Color,
    failureColor: Color
) {
    val colors = LocalPulseColors.current
    if (rankings.isEmpty()) return
    val maxVal = rankings.maxOf { maxOf(it.successCount, it.failureCount, 1) }.toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val count = rankings.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.28f).coerceAtMost(28.dp.toPx())
            val gap = 4.dp.toPx()

            rankings.forEachIndexed { idx, rank ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val sHeight = (rank.successCount / maxVal) * (size.height - 12.dp.toPx())
                val fHeight = (rank.failureCount / maxVal) * (size.height - 12.dp.toPx())

                if (sHeight > 0f) {
                    drawRoundRect(
                        color = successColor,
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - sHeight),
                        size = Size(barWidth, sHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
                if (fHeight > 0f) {
                    drawRoundRect(
                        color = failureColor,
                        topLeft = Offset(centerX + gap / 2f, size.height - fHeight),
                        size = Size(barWidth, fHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            rankings.forEach { rank ->
                Text(
                    text = rank.habit.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
