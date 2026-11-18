package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.model.HabitType
import com.example.data.model.TrackingMode
import com.example.data.preferences.UserPreferences
import com.example.domain.DailyHabitStatusItem
import com.example.domain.PulseAnalyticsEngine
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.HabitIconBadge
import com.example.ui.components.PulseCard
import com.example.ui.components.PulseEmptyState
import com.example.ui.components.SectionHeaderRow
import com.example.ui.i18n.formatLocalizedDate
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import java.time.LocalDate

@Composable
fun HomeScreen(
    habits: List<HabitEntity>,
    records: List<HabitRecordEntity>,
    preferences: UserPreferences,
    onToggleQuickStatus: (HabitEntity, Long) -> Unit,
    onUpdateNumerical: (HabitEntity, Long, Int) -> Unit,
    onOpenHabitDetail: (String) -> Unit,
    onOpenNewHabit: () -> Unit,
    onOpenAllHabits: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onOpenUrgeMode: () -> Unit,
    onOpenWeeklyReview: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val today = remember { LocalDate.now() }
    val todayEpoch = today.toEpochDay()

    val summary = remember(habits, records, todayEpoch) {
        PulseAnalyticsEngine.computeDailySummary(habits, records, today)
    }

    // 0 = All, 1 = Build, 2 = Quit
    var habitFilter by remember { mutableIntStateOf(0) }
    val filteredItems = remember(summary.items, habitFilter) {
        when (habitFilter) {
            1 -> summary.items.filter { it.habit.habitTypeEnum() == HabitType.BUILD }
            2 -> summary.items.filter { it.habit.habitTypeEnum() == HabitType.QUIT }
            else -> summary.items
        }
    }

    val displayName = preferences.displayName.ifBlank { strings.defaultUserName }

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
            // Top Greeting Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primaryAccent.copy(alpha = 0.2f))
                                .border(1.5.dp, colors.primaryAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "P",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.helloGreeting(displayName),
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatLocalizedDate(today, strings.language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onOpenWeeklyReview,
                            modifier = Modifier.testTag("home_weekly_review_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Assessment,
                                contentDescription = strings.weeklyReviewTitle,
                                tint = colors.primaryAccent
                            )
                        }
                        IconButton(
                            onClick = onOpenAchievements,
                            modifier = Modifier.testTag("home_achievements_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = strings.achievementsAndGoalsTitle,
                                tint = colors.warning
                            )
                        }
                    }
                }
            }

            // Daily Progress Circular Indicator + 4 Summary Stat Columns
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressRing(
                            progress = summary.completionPercent / 100f,
                            size = 148.dp,
                            strokeWidth = 12.dp,
                            activeColor = colors.primaryAccent
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${summary.completionPercent}%",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.ofTodaysGoals,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.secondaryBackground)
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MiniStatCol(
                                value = "${summary.completedCount}",
                                label = strings.completedHabits,
                                accentColor = colors.success,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCol(
                                value = "${summary.attentionCount}",
                                label = strings.needAttention,
                                accentColor = colors.failure,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCol(
                                value = "${summary.buildCount}",
                                label = strings.buildHabits,
                                accentColor = colors.primaryAccent,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCol(
                                value = "${summary.quitCount}",
                                label = strings.quitHabits,
                                accentColor = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Motivational Mountain Quick Focus & Urge Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_misty_mountains),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                        alpha = 0.45f
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE6101722),
                                        Color(0xCC172130)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = strings.dailyQuote,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF4F7FA),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenFocusTimer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_quick_focus_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primaryAccent,
                                    contentColor = Color(0xFF0A1420)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.focusQuickAction,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenUrgeMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_quick_urge_btn"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF4F7FA)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.urgeQuickAction,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Today's Habits Section Header + Filter Chips
            item {
                SectionHeaderRow(
                    title = strings.todaysHabits,
                    actionLabel = strings.viewAll,
                    onActionClick = onOpenAllHabits
                )
                if (summary.items.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to strings.filterAll,
                            1 to strings.filterBuild,
                            2 to strings.filterQuit
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = habitFilter == idx,
                                onClick = { habitFilter = idx },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = colors.primaryAccent
                                )
                            )
                        }
                    }
                }
            }

            if (summary.items.isEmpty()) {
                item {
                    PulseEmptyState(
                        icon = Icons.Rounded.Add,
                        title = strings.emptyHomeTitle,
                        subtitle = strings.emptyHomeSubtitle
                    ) {
                        Button(
                            onClick = onOpenNewHabit,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("empty_add_first_habit_btn"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = Color(0xFF0A1420)
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addFirstHabit, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.habit.id }) { item ->
                    TodayHabitRowCard(
                        item = item,
                        onToggleQuick = { onToggleQuickStatus(item.habit, todayEpoch) },
                        onStepNumerical = { delta -> onUpdateNumerical(item.habit, todayEpoch, delta) },
                        onClickCard = { onOpenHabitDetail(item.habit.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MiniStatCol(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalPulseColors.current
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodayHabitRowCard(
    item: DailyHabitStatusItem,
    onToggleQuick: () -> Unit,
    onStepNumerical: (Int) -> Unit,
    onClickCard: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val habit = item.habit
    val isNumerical = habit.habitTypeEnum() == HabitType.BUILD &&
        habit.trackingModeEnum() == TrackingMode.NUMERICAL

    PulseCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_habit_card_${habit.id}"),
        onClick = onClickCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                HabitIconBadge(iconKey = habit.iconKey, colorHex = habit.colorHex)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        isNumerical -> {
                            val currentVal = item.record?.progressValue ?: 0
                            val unitSuffix = if (habit.unit.isNotBlank()) " ${habit.unit}" else ""
                            "$currentVal / ${habit.targetValue}$unitSuffix"
                        }
                        habit.habitTypeEnum() == HabitType.QUIT -> strings.quitLabel
                        else -> strings.buildLabel
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isNumerical) {
                    IconButton(
                        onClick = { onStepNumerical(-1) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Decrease",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { onStepNumerical(1) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Increase",
                            tint = colors.primaryAccent
                        )
                    }
                }

                val statusBg = when {
                    item.isCompletedOrClean -> colors.success
                    item.isMissedOrRelapse -> colors.failure
                    else -> Color.Transparent
                }
                val statusBorder = when {
                    item.isCompletedOrClean -> colors.success
                    item.isMissedOrRelapse -> colors.failure
                    else -> colors.cardBorder
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleQuick)
                        .testTag("home_habit_toggle_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = statusBg,
                        border = BorderStroke(1.5.dp, statusBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                item.isCompletedOrClean -> Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = strings.markCompleted,
                                    tint = Color(0xFF0A1420),
                                    modifier = Modifier.size(18.dp)
                                )
                                item.isMissedOrRelapse -> Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.markMissed,
                                    tint = Color(0xFF0A1420),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
