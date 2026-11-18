package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FocusSessionEntity
import com.example.data.local.IfThenPlanEntity
import com.example.data.model.UrgeRecordKind
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.PulseCard
import com.example.ui.components.PulseEmptyState
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import com.example.ui.viewmodel.TimerUiState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@Composable
fun FocusScreen(
    initialTab: Int,
    focusTimer: TimerUiState,
    urgeTimer: TimerUiState,
    sessions: List<FocusSessionEntity>,
    ifThenPlans: List<IfThenPlanEntity>,
    onSelectFocusDuration: (Int) -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onToggleUrgeTimer: () -> Unit,
    onResetUrgeTimer: () -> Unit,
    onLogUrgeResisted: (String) -> Unit,
    onSavePlan: (IfThenPlanEntity) -> Unit,
    onDeletePlan: (String) -> Unit,
    onReturnFromUrgeMode: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = strings.focusTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // 3-tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(colors.secondaryBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to strings.focusTimerTab,
                    1 to strings.urgeModeTab,
                    2 to strings.ifThenPlansTab
                ).forEach { (idx, label) ->
                    val selected = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("focus_tab_$idx"),
                        shape = RoundedCornerShape(50),
                        color = if (selected) colors.primaryAccent else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color(0xFF0A1420) else colors.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (selectedTab) {
                0 -> FocusTimerTabContent(
                    timer = focusTimer,
                    sessions = sessions,
                    onSelectDuration = onSelectFocusDuration,
                    onToggleTimer = onToggleFocusTimer,
                    onResetTimer = onResetFocusTimer
                )
                1 -> UrgeManagementTabContent(
                    urgeTimer = urgeTimer,
                    ifThenPlans = ifThenPlans,
                    onToggleUrgeTimer = onToggleUrgeTimer,
                    onResetUrgeTimer = onResetUrgeTimer,
                    onLogUrgeResisted = onLogUrgeResisted,
                    onReturn = onReturnFromUrgeMode
                )
                else -> IfThenPlansTabContent(
                    plans = ifThenPlans,
                    onSavePlan = onSavePlan,
                    onDeletePlan = onDeletePlan
                )
            }
        }
    }
}

@Composable
private fun FocusTimerTabContent(
    timer: TimerUiState,
    sessions: List<FocusSessionEntity>,
    onSelectDuration: (Int) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    var showCustomDialog by remember { mutableStateOf(false) }
    var customMinInput by remember { mutableStateOf("40") }

    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    val todaysSessions = remember(sessions, todayEpoch) {
        sessions.filter { it.epochDay == todayEpoch && it.completed }
    }
    val totalMinutesToday = todaysSessions.sumOf { it.durationMinutes }

    val progress = if (timer.totalSeconds > 0) {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    } else 0f

    val mins = timer.remainingSeconds / 60
    val secs = timer.remainingSeconds % 60
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressRing(
                        progress = progress,
                        size = 200.dp,
                        strokeWidth = 12.dp,
                        activeColor = colors.primaryAccent
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.displayLarge,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.focusTimeCaption,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onToggleTimer,
                            shape = CircleShape,
                            color = colors.primaryAccent,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("focus_play_pause_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (timer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (timer.isRunning) strings.pauseTimer else strings.startTimer,
                                    tint = Color(0xFF0A1420),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onResetTimer,
                            shape = CircleShape,
                            color = colors.secondaryBackground,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("focus_reset_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = strings.resetTimer,
                                    tint = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preset duration selector cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    5 to strings.shortSession,
                    25 to strings.mediumSession,
                    50 to strings.longSession
                ).forEach { (minutes, label) ->
                    val selected = timer.selectedMinutes == minutes
                    Surface(
                        onClick = { onSelectDuration(minutes) },
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp)
                            .testTag("focus_preset_$minutes"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) colors.primaryAccent.copy(alpha = 0.16f) else colors.cardBackground,
                        border = BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) colors.primaryAccent else colors.cardBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.primaryAccent else colors.textPrimary,
                                textAlign = TextAlign.Center,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Surface(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier
                        .weight(0.85f)
                        .height(74.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = strings.customMinutes,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Today's Sessions Card
        item {
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = colors.primaryAccent)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.todaysSessions,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = strings.totalMinutesToday(totalMinutesToday),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Text(
                        text = "${todaysSessions.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = colors.cardBackground,
            title = { Text(strings.customMinutes, color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = customMinInput,
                    onValueChange = { customMinInput = it.filter { ch -> ch.isDigit() }.take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = (customMinInput.toIntOrNull() ?: 25).coerceIn(1, 180)
                    onSelectDuration(m)
                    showCustomDialog = false
                }) { Text(strings.saveButton, color = colors.primaryAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun UrgeManagementTabContent(
    urgeTimer: TimerUiState,
    ifThenPlans: List<IfThenPlanEntity>,
    onToggleUrgeTimer: () -> Unit,
    onResetUrgeTimer: () -> Unit,
    onLogUrgeResisted: (String) -> Unit,
    onReturn: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    var breathingActive by remember { mutableStateOf(false) }
    var breathPhase by remember { mutableIntStateOf(0) } // 0=inhale, 1=hold, 2=exhale

    LaunchedEffect(breathingActive) {
        if (!breathingActive) return@LaunchedEffect
        while (breathingActive) {
            breathPhase = 0
            delay(4000L)
            if (!breathingActive) break
            breathPhase = 1
            delay(4000L)
            if (!breathingActive) break
            breathPhase = 2
            delay(6000L)
        }
    }

    val breathScale by animateFloatAsState(
        targetValue = when {
            !breathingActive -> 0.65f
            breathPhase == 0 || breathPhase == 1 -> 1.0f
            else -> 0.55f
        },
        animationSpec = tween(durationMillis = if (breathPhase == 2) 5500 else 3800),
        label = "breathCircle"
    )

    val mins = urgeTimer.remainingSeconds / 60
    val secs = urgeTimer.remainingSeconds % 60
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calming Misty Mountain Hero Message
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_misty_mountains),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    alpha = 0.55f
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xB3101722), Color(0xF0101722))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.urgeHeadline,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF4F7FA),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = strings.urgeSubheadline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAB5C5),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Breathing Exercise Card
        item {
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Air, contentDescription = null, tint = colors.primaryAccent)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.breathingExerciseTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = strings.breathingSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
                if (breathingActive) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((96 * breathScale).dp)
                                .clip(CircleShape)
                                .background(colors.primaryAccent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (breathPhase) {
                                    0 -> strings.breathingInhale
                                    1 -> strings.breathingHold
                                    else -> strings.breathingExhale
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { breathingActive = !breathingActive },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = Color(0xFF0A1420)
                    )
                ) {
                    Text(
                        text = if (breathingActive) strings.stopExercise else strings.startExercise,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5-Minute Urge Timer Card
        item {
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.fiveMinuteTimerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = strings.fiveMinuteTimerSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onToggleUrgeTimer,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("urge_timer_toggle_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = Color(0xFF0A1420)
                        )
                    ) {
                        Text(
                            text = if (urgeTimer.isRunning) strings.pauseTimer else strings.startTimer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onResetUrgeTimer,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.resetTimer)
                    }
                }
            }
        }

        // Coping If-Then Plans preview + Log Resisted + Return
        if (ifThenPlans.isNotEmpty()) {
            items(ifThenPlans.take(3), key = { it.id }) { plan ->
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${plan.triggerText} -> ${plan.responseText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onLogUrgeResisted("Urge surfed in Focus Mode") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("urge_log_resisted_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.success,
                    contentColor = Color(0xFF0A1420)
                )
            ) {
                Text(strings.logResistedBtn, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onReturn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("urge_return_btn"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.cardBorder)
            ) {
                Text(strings.returnPreviousActivity, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun IfThenPlansTabContent(
    plans: List<IfThenPlanEntity>,
    onSavePlan: (IfThenPlanEntity) -> Unit,
    onDeletePlan: (String) -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    var editingPlan by remember { mutableStateOf<IfThenPlanEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                editingPlan = null
                showDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_if_then_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryAccent,
                contentColor = Color(0xFF0A1420)
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(strings.addIfThenPlan, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        if (plans.isEmpty()) {
            PulseEmptyState(
                icon = Icons.Rounded.Shield,
                title = strings.ifThenPlansTab,
                subtitle = strings.ifThenEmpty
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.triggerText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.primaryAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = plan.responseText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editingPlan = plan
                                    showDialog = true
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = colors.textSecondary)
                                }
                                IconButton(onClick = { onDeletePlan(plan.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = colors.failure)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var trigger by remember(editingPlan) { mutableStateOf(editingPlan?.triggerText ?: "") }
        var response by remember(editingPlan) { mutableStateOf(editingPlan?.responseText ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = colors.cardBackground,
            title = { Text(strings.addIfThenPlan, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = trigger,
                        onValueChange = { trigger = it.take(200) },
                        label = { Text(strings.ifTriggerLabel) },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = response,
                        onValueChange = { response = it.take(300) },
                        label = { Text(strings.thenResponseLabel) },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (trigger.isNotBlank() && response.isNotBlank()) {
                            onSavePlan(
                                IfThenPlanEntity(
                                    id = editingPlan?.id ?: UUID.randomUUID().toString(),
                                    triggerText = trigger.trim(),
                                    responseText = response.trim(),
                                    createdAtMs = editingPlan?.createdAtMs ?: System.currentTimeMillis(),
                                    updatedAtMs = System.currentTimeMillis()
                                )
                            )
                            showDialog = false
                        }
                    }
                ) { Text(strings.saveButton, color = colors.primaryAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
