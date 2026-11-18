package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.model.HabitCategory
import com.example.data.model.HabitType
import com.example.data.model.ScheduleType
import com.example.domain.PulseAnalyticsEngine
import com.example.ui.components.HabitIconBadge
import com.example.ui.components.PulseCard
import com.example.ui.components.PulseEmptyState
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import java.time.LocalDate

@Composable
fun HabitsScreen(
    habits: List<HabitEntity>,
    records: List<HabitRecordEntity>,
    onAddHabit: () -> Unit,
    onOpenHabitDetail: (String) -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val today = remember { LocalDate.now() }

    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var typeTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Build, 2 = Quit
    var selectedCategory by remember { mutableStateOf<HabitCategory?>(null) }

    val filteredHabits = remember(habits, searchQuery, typeTab, selectedCategory) {
        habits.filter { h ->
            val matchesQuery = searchQuery.isBlank() ||
                h.name.contains(searchQuery, ignoreCase = true) ||
                h.description.contains(searchQuery, ignoreCase = true)
            val matchesType = when (typeTab) {
                1 -> h.habitTypeEnum() == HabitType.BUILD
                2 -> h.habitTypeEnum() == HabitType.QUIT
                else -> true
            }
            val matchesCat = selectedCategory == null || h.categoryEnum() == selectedCategory
            matchesQuery && matchesType && matchesCat
        }
    }

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

            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) searchQuery = ""
                    },
                    modifier = Modifier.testTag("habits_search_toggle")
                ) {
                    Icon(
                        imageVector = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = strings.searchHabits,
                        tint = colors.textPrimary
                    )
                }

                Text(
                    text = strings.habitsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    onClick = onAddHabit,
                    shape = CircleShape,
                    color = colors.primaryAccent,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("habits_add_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.newHabitTitle,
                            tint = Color(0xFF0A1420)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = searchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchHabits) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("habits_search_input")
                )
            }

            Spacer(Modifier.height(12.dp))

            // 3-Segment Pill Filter (All / Build / Quit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(colors.secondaryBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to strings.filterAll,
                    1 to strings.filterBuild,
                    2 to strings.filterQuit
                ).forEach { (idx, label) ->
                    val selected = typeTab == idx
                    Surface(
                        onClick = { typeTab = idx },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("habits_filter_tab_$idx"),
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

            Spacer(Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(strings.filterAll) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                        selectedLabelColor = colors.primaryAccent
                    )
                )
                HabitCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.localizedName(strings.language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.primaryAccent
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (filteredHabits.isEmpty()) {
                PulseEmptyState(
                    icon = Icons.Rounded.Add,
                    title = strings.emptyHomeTitle,
                    subtitle = strings.emptyHomeSubtitle
                ) {
                    Button(
                        onClick = onAddHabit,
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredHabits, key = { it.id }) { habit ->
                        val streakInfo = remember(habit, records) {
                            PulseAnalyticsEngine.computeHabitStreak(habit, records, today)
                        }
                        val scheduleLabel = if (habit.scheduleTypeEnum() == ScheduleType.DAILY) {
                            strings.dailyLabel
                        } else {
                            strings.customScheduleLabel(habit.scheduleDaysList().size)
                        }
                        val typeLabel = if (habit.habitTypeEnum() == HabitType.BUILD) {
                            strings.buildLabel
                        } else {
                            strings.quitLabel
                        }
                        val typeColor = if (habit.habitTypeEnum() == HabitType.BUILD) {
                            colors.primaryAccent
                        } else {
                            colors.failure
                        }

                        PulseCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("habits_list_card_${habit.id}"),
                            onClick = { onOpenHabitDetail(habit.id) }
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
                                    HabitIconBadge(
                                        iconKey = habit.iconKey,
                                        colorHex = habit.colorHex,
                                        badgeSize = 48.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$scheduleLabel • ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textSecondary
                                            )
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = typeColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (streakInfo.currentStreak > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = colors.success.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, colors.success.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = colors.success,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = "${streakInfo.currentStreak}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.success,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
