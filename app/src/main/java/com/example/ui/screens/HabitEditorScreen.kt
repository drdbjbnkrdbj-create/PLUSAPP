package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.data.model.AppLanguage
import com.example.data.model.HabitCategory
import com.example.data.model.HabitType
import com.example.data.model.ScheduleType
import com.example.data.model.TrackingMode
import com.example.ui.components.HabitIconBadge
import com.example.ui.components.PulseCard
import com.example.ui.components.parseColorHex
import com.example.ui.components.pulseHabitColorPalette
import com.example.ui.components.pulseHabitIconOptions
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.rememberStrings
import com.example.ui.i18n.shortDayName
import com.example.ui.theme.LocalPulseColors
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorScreen(
    existingHabit: HabitEntity?,
    onSave: (HabitEntity) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()

    var name by remember(existingHabit) { mutableStateOf(existingHabit?.name ?: "") }
    var description by remember(existingHabit) { mutableStateOf(existingHabit?.description ?: "") }
    var habitType by remember(existingHabit) { mutableStateOf(existingHabit?.habitTypeEnum() ?: HabitType.BUILD) }
    var trackingMode by remember(existingHabit) { mutableStateOf(existingHabit?.trackingModeEnum() ?: TrackingMode.BOOLEAN) }
    var targetValueStr by remember(existingHabit) { mutableStateOf((existingHabit?.targetValue ?: 1).toString()) }
    var unit by remember(existingHabit) { mutableStateOf(existingHabit?.unit ?: "") }
    var category by remember(existingHabit) { mutableStateOf(existingHabit?.categoryEnum() ?: HabitCategory.HEALTH) }
    var iconKey by remember(existingHabit) { mutableStateOf(existingHabit?.iconKey ?: "book") }
    var colorHex by remember(existingHabit) { mutableStateOf(existingHabit?.colorHex ?: "#27C6D9") }
    var scheduleType by remember(existingHabit) { mutableStateOf(existingHabit?.scheduleTypeEnum() ?: ScheduleType.DAILY) }
    var selectedDays by remember(existingHabit) {
        mutableStateOf(existingHabit?.scheduleDaysList()?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7))
    }
    var reminderEnabled by remember(existingHabit) { mutableStateOf(existingHabit?.reminderEnabled ?: false) }
    var reminderTime by remember(existingHabit) { mutableStateOf(existingHabit?.reminderTime ?: "09:00") }

    var showIconSheet by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.cancel,
                        tint = colors.textPrimary
                    )
                }

                Text(
                    text = if (existingHabit == null) strings.newHabitTitle else strings.editHabitTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (existingHabit != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("editor_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = strings.deleteHabit,
                            tint = colors.failure
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Icon & Accent Color Avatar Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HabitIconBadge(
                    iconKey = iconKey,
                    colorHex = colorHex,
                    badgeSize = 72.dp,
                    modifier = Modifier.clickable { showIconSheet = true }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showIconSheet = true },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.testTag("editor_change_icon_btn")
                ) {
                    Text(
                        text = strings.changeIconColor,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
            }

            // Name & Description
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(100)
                    nameError = false
                },
                label = { Text(strings.habitNameLabel) },
                placeholder = { Text(strings.habitNameHint) },
                isError = nameError,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_name_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(400) },
                label = { Text(strings.descriptionOptional) },
                placeholder = { Text(strings.descriptionHint) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_description_input")
            )

            // Habit Type: Build vs Quit
            Text(
                text = strings.habitTypeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    HabitType.BUILD to colors.success,
                    HabitType.QUIT to colors.failure
                ).forEach { (type, accent) ->
                    val selected = habitType == type
                    Surface(
                        onClick = {
                            habitType = type
                            if (type == HabitType.QUIT) {
                                trackingMode = TrackingMode.BOOLEAN
                                colorHex = "#F07878"
                            } else if (colorHex == "#F07878") {
                                colorHex = "#27C6D9"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("editor_type_${type.name}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent.copy(alpha = 0.16f) else colors.cardBackground,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) accent else colors.cardBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = type.localizedName(strings.language),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) accent else colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Tracking Method (for BUILD habits)
            if (habitType == HabitType.BUILD) {
                Text(
                    text = strings.trackingMethodLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        TrackingMode.BOOLEAN to strings.simpleCheckin,
                        TrackingMode.NUMERICAL to strings.numericalTarget
                    ).forEach { (mode, label) ->
                        val selected = trackingMode == mode
                        Surface(
                            onClick = { trackingMode = mode },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("editor_tracking_${mode.name}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) colors.primaryAccent.copy(alpha = 0.16f) else colors.cardBackground,
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) colors.primaryAccent else colors.cardBorder
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) colors.primaryAccent else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                if (trackingMode == TrackingMode.NUMERICAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = targetValueStr,
                            onValueChange = { raw ->
                                targetValueStr = raw.filter { it.isDigit() }.take(4)
                            },
                            label = { Text(strings.targetValueLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it.take(30) },
                            label = { Text(strings.unitLabel) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }

            // Category
            Text(
                text = strings.categoryLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.localizedName(strings.language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.primaryAccent
                        )
                    )
                }
            }

            // Schedule (Daily vs Specific Days)
            Text(
                text = strings.scheduleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    ScheduleType.DAILY to strings.dailyLabel,
                    ScheduleType.CUSTOM_DAYS to strings.customScheduleLabel(selectedDays.size)
                ).forEach { (type, label) ->
                    val selected = scheduleType == type
                    Surface(
                        onClick = { scheduleType = type },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) colors.primaryAccent.copy(alpha = 0.16f) else colors.cardBackground,
                        border = BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) colors.primaryAccent else colors.cardBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) colors.primaryAccent else colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Weekday pills (Saturday=6, Sunday=7, Mon=1..Fri=5)
            val weekOrder = listOf(6, 7, 1, 2, 3, 4, 5)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekOrder.forEach { dayNum ->
                    val active = scheduleType == ScheduleType.DAILY || dayNum in selectedDays
                    FilterChip(
                        selected = active,
                        onClick = {
                            scheduleType = ScheduleType.CUSTOM_DAYS
                            selectedDays = if (dayNum in selectedDays && selectedDays.size > 1) {
                                selectedDays - dayNum
                            } else {
                                selectedDays + dayNum
                            }
                        },
                        label = { Text(shortDayName(dayNum, strings.language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryAccent.copy(alpha = 0.22f),
                            selectedLabelColor = colors.primaryAccent
                        )
                    )
                }
            }

            // Reminder Toggle & Time Picker
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = colors.primaryAccent
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.reminderLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            if (reminderEnabled) {
                                Text(
                                    text = "${strings.reminderTimeLabel}: $reminderTime",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.primaryAccent
                                )
                            }
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        modifier = Modifier.testTag("editor_reminder_switch")
                    )
                }

                if (reminderEnabled) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${strings.reminderTimeLabel}: $reminderTime")
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Save Button
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        nameError = true
                        return@Button
                    }
                    val target = if (habitType == HabitType.BUILD && trackingMode == TrackingMode.NUMERICAL) {
                        (targetValueStr.toIntOrNull() ?: 1).coerceIn(1, 10000)
                    } else 1
                    val daysCsv = if (scheduleType == ScheduleType.DAILY) {
                        "1,2,3,4,5,6,7"
                    } else {
                        selectedDays.sorted().joinToString(",").ifBlank { "1,2,3,4,5,6,7" }
                    }
                    val habit = HabitEntity(
                        id = existingHabit?.id ?: UUID.randomUUID().toString(),
                        name = trimmed,
                        description = description.trim(),
                        category = category.name,
                        habitType = habitType.name,
                        trackingMode = if (habitType == HabitType.QUIT) TrackingMode.BOOLEAN.name else trackingMode.name,
                        targetValue = target,
                        unit = if (habitType == HabitType.BUILD && trackingMode == TrackingMode.NUMERICAL) unit.trim() else "",
                        iconKey = iconKey,
                        colorHex = colorHex,
                        scheduleType = scheduleType.name,
                        scheduleDaysCsv = daysCsv,
                        reminderEnabled = reminderEnabled,
                        reminderTime = reminderTime,
                        archived = false,
                        createdEpochDay = existingHabit?.createdEpochDay ?: LocalDate.now().toEpochDay(),
                        updatedAtMs = System.currentTimeMillis()
                    )
                    onSave(habit)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("editor_save_btn"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = Color(0xFF0A1420)
                )
            ) {
                Text(
                    text = strings.saveButton,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showIconSheet) {
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false },
            containerColor = colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = strings.changeIconColor,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                // Color row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pulseHabitColorPalette.forEach { hex ->
                        val c = parseColorHex(hex)
                        val isSel = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSel) 3.dp else 1.dp,
                                    color = if (isSel) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0A1420),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Icon grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pulseHabitIconOptions.forEach { opt ->
                        val isSel = iconKey == opt.key
                        val accent = parseColorHex(colorHex)
                        Surface(
                            onClick = { iconKey = opt.key },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) accent.copy(alpha = 0.25f) else colors.secondaryBackground,
                            border = BorderStroke(
                                if (isSel) 2.dp else 1.dp,
                                if (isSel) accent else colors.cardBorder
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.key,
                                    tint = if (isSel) accent else colors.textPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showTimePicker) {
        val parts = reminderTime.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val initMin = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val tpState = rememberTimePickerState(initialHour = initHour, initialMinute = initMin, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = colors.cardBackground,
            title = { Text(strings.reminderTimeLabel, color = colors.textPrimary) },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = String.format(Locale.US, "%02d:%02d", tpState.hour, tpState.minute)
                    showTimePicker = false
                }) { Text(strings.saveButton, color = colors.primaryAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showDeleteConfirm && existingHabit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.cardBackground,
            title = { Text(strings.deleteConfirmTitle, color = colors.textPrimary) },
            text = { Text(strings.deleteConfirmBody, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(existingHabit.id)
                }) { Text(strings.confirm, color = colors.failure) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}
