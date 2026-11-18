package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.HabitType
import com.example.data.model.RecordStatus
import com.example.data.model.TrackingMode
import com.example.data.model.UrgeRecordKind
import com.example.domain.PulseAnalyticsEngine
import com.example.ui.components.HabitIconBadge
import com.example.ui.components.PulseCard
import com.example.ui.i18n.formatLocalizedDate
import com.example.ui.i18n.formatLocalizedMonth
import com.example.ui.i18n.rememberStrings
import com.example.ui.i18n.shortDayName
import com.example.ui.theme.LocalPulseColors
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun HabitDetailScreen(
    habit: HabitEntity?,
    records: List<HabitRecordEntity>,
    urges: List<UrgeRecordEntity>,
    onBack: () -> Unit,
    onEditHabit: (String) -> Unit,
    onSaveRecord: (HabitEntity, Long, RecordStatus, Int, String) -> Unit,
    onLogUrge: (String?, Long, UrgeRecordKind, Int, String) -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()

    if (habit == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(onClick = onBack) { Text(strings.cancel) }
        }
        return
    }

    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }

    val recordMap = remember(records) { records.associateBy { it.epochDay } }
    val selectedRecord = recordMap[selectedDate.toEpochDay()]
    val streakInfo = remember(habit, records) {
        PulseAnalyticsEngine.computeHabitStreak(habit, records, today)
    }

    var noteDraft by remember(selectedDate, selectedRecord?.notes) {
        mutableStateOf(selectedRecord?.notes ?: "")
    }
    var progressDraft by remember(selectedDate, selectedRecord?.progressValue) {
        mutableIntStateOf(selectedRecord?.progressValue ?: 0)
    }
    var urgeIntensity by remember { mutableFloatStateOf(5f) }

    LaunchedEffect(selectedDate, selectedRecord) {
        noteDraft = selectedRecord?.notes ?: ""
        progressDraft = selectedRecord?.progressValue ?: 0
    }

    val calendarCells = remember(currentMonth) {
        PulseAnalyticsEngine.monthCalendarDays(currentMonth)
    }
    val weekHeaders = listOf(6, 7, 1, 2, 3, 4, 5) // Sat..Fri

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
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.cancel,
                            tint = colors.textPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        HabitIconBadge(
                            iconKey = habit.iconKey,
                            colorHex = habit.colorHex,
                            badgeSize = 38.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { onEditHabit(habit.id) },
                        modifier = Modifier.testTag("detail_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = strings.editHabitTitle,
                            tint = colors.primaryAccent
                        )
                    }
                }
            }

            // Streak Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulseCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.currentStreakLabel(streakInfo.currentStreak),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    PulseCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.longestStreakLabel(streakInfo.longestStreak),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Monthly Calendar Card
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentMonth = currentMonth.minusMonths(1) },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = colors.textPrimary
                            )
                        }
                        Text(
                            text = formatLocalizedMonth(currentMonth, strings.language),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { currentMonth = currentMonth.plusMonths(1) },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = colors.textPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekHeaders.forEach { dNum ->
                            Text(
                                text = shortDayName(dNum, strings.language),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Calendar Grid Rows
                    val rows = calendarCells.chunked(7)
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { cellDate ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cellDate != null) {
                                        val rec = recordMap[cellDate.toEpochDay()]
                                        val isSuccess = PulseAnalyticsEngine.isSuccessfulRecord(habit, rec)
                                        val isFail = PulseAnalyticsEngine.isUnsuccessfulRecord(habit, rec)
                                        val isSelected = cellDate == selectedDate

                                        val bg = when {
                                            isSuccess -> colors.success
                                            isFail -> colors.failure
                                            else -> colors.secondaryBackground
                                        }
                                        val textColor = when {
                                            isSuccess || isFail -> Color(0xFF0A1420)
                                            else -> colors.textSecondary
                                        }
                                        val border = if (isSelected) {
                                            BorderStroke(2.dp, colors.primaryAccent)
                                        } else null

                                        Surface(
                                            shape = CircleShape,
                                            color = bg,
                                            border = border,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .clickable { selectedDate = cellDate }
                                                .testTag("calendar_day_${cellDate}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${cellDate.dayOfMonth}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = textColor,
                                                    fontWeight = if (isSelected || isSuccess || isFail) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Clear Calendar Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CalendarLegendItem(color = colors.success, label = strings.legendSuccess)
                        CalendarLegendItem(color = colors.failure, label = strings.legendFailed)
                        CalendarLegendItem(color = colors.secondaryBackground, label = strings.legendUnrecorded)
                    }
                }
            }

            // Selected Day Details & Editing Card
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${strings.dayDetailsTitle} • ${formatLocalizedDate(selectedDate, strings.language)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    if (habit.habitTypeEnum() == HabitType.BUILD) {
                        if (habit.trackingModeEnum() == TrackingMode.NUMERICAL) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$progressDraft / ${habit.targetValue} ${habit.unit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.primaryAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        onClick = { progressDraft = (progressDraft - 1).coerceAtLeast(0) },
                                        shape = CircleShape,
                                        color = colors.secondaryBackground,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Remove, contentDescription = null, tint = colors.textPrimary)
                                        }
                                    }
                                    Surface(
                                        onClick = { progressDraft += 1 },
                                        shape = CircleShape,
                                        color = colors.primaryAccent,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF0A1420))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it.take(500) },
                            label = { Text(strings.notesLabel) },
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val value = if (habit.trackingModeEnum() == TrackingMode.NUMERICAL) {
                                        progressDraft.coerceAtLeast(habit.targetValue)
                                    } else habit.targetValue
                                    progressDraft = value
                                    onSaveRecord(habit, selectedDate.toEpochDay(), RecordStatus.COMPLETED, value, noteDraft)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.success,
                                    contentColor = Color(0xFF0A1420)
                                )
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.markCompleted, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onSaveRecord(habit, selectedDate.toEpochDay(), RecordStatus.MISSED, progressDraft, noteDraft)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.failure.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.failure)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.markMissed)
                            }
                        }
                    } else {
                        // QUIT HABIT Controls: Clean Day vs Relapse vs Log Urge Resisted
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it.take(500) },
                            label = { Text(strings.notesLabel) },
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${strings.intensityLabel}: ${urgeIntensity.roundToInt()}/10",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary
                        )
                        Slider(
                            value = urgeIntensity,
                            onValueChange = { urgeIntensity = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSaveRecord(habit, selectedDate.toEpochDay(), RecordStatus.CLEAN_DAY, 1, noteDraft)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.success,
                                    contentColor = Color(0xFF0A1420)
                                )
                            ) {
                                Text(strings.markCleanDay, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onLogUrge(
                                        habit.id,
                                        selectedDate.toEpochDay(),
                                        UrgeRecordKind.RELAPSE,
                                        urgeIntensity.roundToInt(),
                                        noteDraft
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.failure.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.failure)
                            ) {
                                Text(strings.logRelapse)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onLogUrge(
                                    habit.id,
                                    selectedDate.toEpochDay(),
                                    UrgeRecordKind.URGE_RESISTED,
                                    urgeIntensity.roundToInt(),
                                    noteDraft
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.logUrgeResisted, color = colors.primaryAccent)
                        }
                    }
                }
            }

            if (urges.isNotEmpty()) {
                items(urges.take(10), key = { it.id }) { urge ->
                    val kindColor = if (urge.kindEnum() == UrgeRecordKind.URGE_RESISTED) colors.success else colors.failure
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (urge.kindEnum() == UrgeRecordKind.URGE_RESISTED) strings.logUrgeResisted else strings.logRelapse,
                                style = MaterialTheme.typography.titleMedium,
                                color = kindColor
                            )
                            Text(
                                text = "${LocalDate.ofEpochDay(urge.epochDay)} • ${urge.timeString}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                        if (urge.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = urge.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    val colors = LocalPulseColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, colors.cardBorder, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}
