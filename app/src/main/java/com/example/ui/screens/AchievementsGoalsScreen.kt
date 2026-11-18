package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.AchievementEntity
import com.example.data.local.GoalEntity
import com.example.data.model.AppLanguage
import com.example.data.model.GoalStatus
import com.example.ui.components.PulseCard
import com.example.ui.components.PulseEmptyState
import com.example.ui.i18n.formatLocalizedShortDate
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import java.time.LocalDate
import java.util.UUID

@Composable
fun AchievementsGoalsScreen(
    achievements: List<AchievementEntity>,
    goals: List<GoalEntity>,
    onSaveGoal: (GoalEntity) -> Unit,
    onIncrementGoal: (GoalEntity, Int) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    var showGoalDialog by remember { mutableStateOf(false) }

    val achMap = remember(achievements) { achievements.associateBy { it.key } }
    val displayKeys = listOf("days_7", "days_30", "days_60", "days_90", "first_habit", "focus_5", "urge_3")

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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ach_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.cancel,
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        text = strings.achievementsAndGoalsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(48.dp))
                }
            }

            item {
                Text(
                    text = strings.achievementsSection,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    displayKeys.forEach { key ->
                        val entity = achMap[key]
                        val unlocked = entity?.unlockedAtMs != null
                        val current = entity?.progressCurrent ?: 0
                        val target = entity?.progressTarget ?: targetForKey(key)
                        val title = badgeTitle(key, strings.language)

                        PulseCard(
                            modifier = Modifier.width(120.dp),
                            borderColor = if (unlocked) colors.success.copy(alpha = 0.6f) else colors.cardBorder
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (unlocked) colors.success.copy(alpha = 0.18f)
                                            else colors.secondaryBackground
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                        contentDescription = title,
                                        tint = if (unlocked) colors.warning else colors.textSecondary
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (unlocked) colors.textPrimary else colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (unlocked) "✓" else "$current / $target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) colors.success else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.longTermGoalsSection,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { showGoalDialog = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = Color(0xFF0A1420)
                        ),
                        modifier = Modifier.testTag("add_goal_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addNewGoal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (goals.isEmpty()) {
                item {
                    PulseEmptyState(
                        icon = Icons.Rounded.EmojiEvents,
                        title = strings.longTermGoalsSection,
                        subtitle = strings.addNewGoal
                    )
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    val pct = (goal.currentProgress.toFloat() / goal.targetTotal.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val completed = goal.statusEnum() == GoalStatus.COMPLETED
                    val targetDate = LocalDate.ofEpochDay(goal.targetEpochDay)

                    PulseCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (completed) colors.success.copy(alpha = 0.6f) else colors.cardBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (completed) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = colors.success,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (goal.description.isNotBlank()) {
                                    Text(
                                        text = goal.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                }
                                Text(
                                    text = "${strings.goalTargetDateLabel}: ${formatLocalizedShortDate(targetDate, strings.language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                            IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = colors.failure)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (completed) colors.success else colors.primaryAccent,
                                trackColor = colors.secondaryBackground
                            )
                            Text(
                                text = "${goal.currentProgress}/${goal.targetTotal}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                onClick = { onIncrementGoal(goal, -1) },
                                shape = CircleShape,
                                color = colors.secondaryBackground,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                onClick = { onIncrementGoal(goal, 1) },
                                shape = CircleShape,
                                color = colors.primaryAccent,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = Color(0xFF0A1420), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var targetTotal by remember { mutableStateOf("10") }
        var daysAhead by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = colors.cardBackground,
            title = { Text(strings.addNewGoal, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(120) },
                        label = { Text(strings.goalTitleLabel) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(400) },
                        label = { Text(strings.descriptionOptional) },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = targetTotal,
                        onValueChange = { targetTotal = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text(strings.goalTargetLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = daysAhead,
                        onValueChange = { daysAhead = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("${strings.goalTargetDateLabel} (+days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            val total = (targetTotal.toIntOrNull() ?: 1).coerceIn(1, 100000)
                            val days = (daysAhead.toLongOrNull() ?: 30L).coerceIn(1L, 1825L)
                            onSaveGoal(
                                GoalEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    targetEpochDay = LocalDate.now().plusDays(days).toEpochDay(),
                                    currentProgress = 0,
                                    targetTotal = total,
                                    status = GoalStatus.ACTIVE.name
                                )
                            )
                            showGoalDialog = false
                        }
                    }
                ) { Text(strings.saveButton, color = colors.primaryAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

private fun targetForKey(key: String): Int = when (key) {
    "days_7" -> 7
    "days_30" -> 30
    "days_60" -> 60
    "days_90" -> 90
    "focus_5" -> 5
    "urge_3" -> 3
    else -> 1
}

private fun badgeTitle(key: String, lang: AppLanguage): String = when (lang) {
    AppLanguage.AR -> when (key) {
        "days_7" -> "7 أيام ناجحة"
        "days_30" -> "30 يوم ناجح"
        "days_60" -> "60 يوم ناجح"
        "days_90" -> "90 يوم ناجح"
        "first_habit" -> "أول عادة"
        "focus_5" -> "5 جلسات تركيز"
        "urge_3" -> "مقاومة 3 رغبات"
        else -> key
    }
    AppLanguage.EN -> when (key) {
        "days_7" -> "7 Successful Days"
        "days_30" -> "30 Successful Days"
        "days_60" -> "60 Successful Days"
        "days_90" -> "90 Successful Days"
        "first_habit" -> "First Habit"
        "focus_5" -> "5 Focus Sessions"
        "urge_3" -> "3 Urges Resisted"
        else -> key
    }
}
