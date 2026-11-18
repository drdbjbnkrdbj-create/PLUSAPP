package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.model.AppLanguage
import com.example.domain.PulseAnalyticsEngine
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.PulseCard
import com.example.ui.i18n.formatLocalizedShortDate
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun WeeklyReviewScreen(
    habits: List<HabitEntity>,
    records: List<HabitRecordEntity>,
    onBack: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()

    var weekOffset by remember { mutableIntStateOf(0) }
    val currentWeekStart = remember(weekOffset) {
        LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
            .plusWeeks(weekOffset.toLong())
    }

    val review = remember(habits, records, currentWeekStart) {
        PulseAnalyticsEngine.computeWeeklyReview(habits, records, currentWeekStart)
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("weekly_review_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.cancel,
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        text = strings.weeklyReviewTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(48.dp))
                }
            }

            // Week Selector
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { weekOffset -= 1 },
                            modifier = Modifier.testTag("weekly_prev_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous Week",
                                tint = colors.textPrimary
                            )
                        }
                        Text(
                            text = "${formatLocalizedShortDate(review.weekStart, strings.language)} - ${formatLocalizedShortDate(review.weekEnd, strings.language)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { if (weekOffset < 0) weekOffset += 1 },
                            enabled = weekOffset < 0,
                            modifier = Modifier.testTag("weekly_next_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Week",
                                tint = if (weekOffset < 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Overall Ring + Delta + Counts
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressRing(
                            progress = review.overallPercent / 100f,
                            size = 112.dp,
                            strokeWidth = 10.dp,
                            activeColor = colors.primaryAccent
                        ) {
                            Text(
                                text = "${review.overallPercent}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val deltaColor = if (review.deltaFromPrevWeekPercent >= 0) colors.success else colors.failure
                            Text(
                                text = strings.comparedToPrevious(review.deltaFromPrevWeekPercent),
                                style = MaterialTheme.typography.titleMedium,
                                color = deltaColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "${strings.totalSuccessfulDays}: ${review.successfulCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.success
                            )
                            Text(
                                text = "${strings.totalUnsuccessfulDays}: ${review.unsuccessfulCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.failure
                            )
                        }
                    }
                }
            }

            // Best Habit & Most Missed Habit
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulseCard(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = colors.warning
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.bestHabit,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = review.bestHabit?.habit?.name ?: "-",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        review.bestHabit?.let {
                            Text(
                                text = "${it.completionRate}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.success
                            )
                        }
                    }

                    PulseCard(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = colors.failure
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.mostMissedHabit,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = review.mostMissedHabit?.habit?.name ?: "-",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        review.mostMissedHabit?.let {
                            Text(
                                text = "${it.completionRate}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.failure
                            )
                        }
                    }
                }
            }

            // Data-Driven Weekly Summary
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.weeklySummaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (strings.language == AppLanguage.AR) review.summaryAr else review.summaryEn,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
