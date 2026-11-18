package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.i18n.rememberStrings
import com.example.ui.screens.AchievementsGoalsScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HabitDetailScreen
import com.example.ui.screens.HabitEditorScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WeeklyReviewScreen
import com.example.ui.theme.LocalPulseColors
import com.example.ui.viewmodel.PulseViewModel
import com.google.firebase.auth.FirebaseUser

object PulseRoutes {
    const val HOME = "home"
    const val HABITS = "habits"
    const val INSIGHTS = "insights"
    const val FOCUS = "focus"
    const val FOCUS_WITH_TAB = "focus?tab={tab}"
    const val PROFILE = "profile"
    const val HABIT_EDITOR = "habit_editor?habitId={habitId}"
    const val HABIT_DETAIL = "habit_detail/{habitId}"
    const val WEEKLY_REVIEW = "weekly_review"
    const val ACHIEVEMENTS = "achievements"

    fun habitEditor(habitId: String? = null): String =
        if (habitId.isNullOrBlank()) "habit_editor" else "habit_editor?habitId=$habitId"

    fun habitDetail(habitId: String): String = "habit_detail/$habitId"
    fun focus(tab: Int = 0): String = "focus?tab=$tab"
}

private data class BottomNavDest(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun PulseMainShell(
    viewModel: PulseViewModel,
    currentUser: FirebaseUser?,
    onSignedOut: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val navController = rememberNavController()

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val urges by viewModel.allUrges.collectAsStateWithLifecycle()
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val focusSessions by viewModel.allFocusSessions.collectAsStateWithLifecycle()
    val ifThenPlans by viewModel.allIfThenPlans.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val focusTimer by viewModel.focusTimer.collectAsStateWithLifecycle()
    val urgeTimer by viewModel.urgeTimer.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: PulseRoutes.HOME

    val bottomDestinations = listOf(
        BottomNavDest(PulseRoutes.HOME, strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
        BottomNavDest(PulseRoutes.HABITS, strings.navHabits, Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle, "nav_habits"),
        BottomNavDest(PulseRoutes.INSIGHTS, strings.navInsights, Icons.Rounded.AutoGraph, Icons.Outlined.AutoGraph, "nav_insights"),
        BottomNavDest(PulseRoutes.FOCUS, strings.navFocus, Icons.Rounded.Timer, Icons.Outlined.Timer, "nav_focus"),
        BottomNavDest(PulseRoutes.PROFILE, strings.navProfile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
    )

    val isTopLevel = currentRoute == PulseRoutes.HOME ||
        currentRoute == PulseRoutes.HABITS ||
        currentRoute == PulseRoutes.INSIGHTS ||
        currentRoute.startsWith(PulseRoutes.FOCUS) ||
        currentRoute == PulseRoutes.PROFILE

    fun navigateTopLevel(route: String) {
        val target = if (route == PulseRoutes.FOCUS) PulseRoutes.focus(0) else route
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar(
                        containerColor = colors.secondaryBackground,
                        contentColor = colors.textPrimary
                    ) {
                        bottomDestinations.forEach { dest ->
                            val selected = if (dest.route == PulseRoutes.FOCUS) {
                                currentRoute.startsWith(PulseRoutes.FOCUS)
                            } else {
                                currentRoute == dest.route
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.primaryAccent,
                                    selectedTextColor = colors.primaryAccent,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                    indicatorColor = colors.primaryAccent.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail(
                        containerColor = colors.secondaryBackground
                    ) {
                        bottomDestinations.forEach { dest ->
                            val selected = if (dest.route == PulseRoutes.FOCUS) {
                                currentRoute.startsWith(PulseRoutes.FOCUS)
                            } else {
                                currentRoute == dest.route
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.primaryAccent,
                                    selectedTextColor = colors.primaryAccent,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                    indicatorColor = colors.primaryAccent.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = PulseRoutes.HOME
                    ) {
                        composable(PulseRoutes.HOME) {
                            HomeScreen(
                                habits = habits,
                                records = records,
                                preferences = preferences,
                                onToggleQuickStatus = viewModel::toggleQuickHabitStatus,
                                onUpdateNumerical = viewModel::updateNumericalProgress,
                                onOpenHabitDetail = { id -> navController.navigate(PulseRoutes.habitDetail(id)) },
                                onOpenNewHabit = { navController.navigate(PulseRoutes.habitEditor()) },
                                onOpenAllHabits = { navigateTopLevel(PulseRoutes.HABITS) },
                                onOpenFocusTimer = { navController.navigate(PulseRoutes.focus(0)) },
                                onOpenUrgeMode = { navController.navigate(PulseRoutes.focus(1)) },
                                onOpenWeeklyReview = { navController.navigate(PulseRoutes.WEEKLY_REVIEW) },
                                onOpenAchievements = { navController.navigate(PulseRoutes.ACHIEVEMENTS) }
                            )
                        }

                        composable(PulseRoutes.HABITS) {
                            HabitsScreen(
                                habits = habits,
                                records = records,
                                onAddHabit = { navController.navigate(PulseRoutes.habitEditor()) },
                                onOpenHabitDetail = { id -> navController.navigate(PulseRoutes.habitDetail(id)) }
                            )
                        }

                        composable(PulseRoutes.INSIGHTS) {
                            InsightsScreen(
                                habits = habits,
                                records = records,
                                urges = urges,
                                onOpenWeeklyReview = { navController.navigate(PulseRoutes.WEEKLY_REVIEW) }
                            )
                        }

                        composable(
                            route = PulseRoutes.FOCUS_WITH_TAB,
                            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
                        ) { backStackEntry ->
                            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                            FocusScreen(
                                initialTab = tab,
                                focusTimer = focusTimer,
                                urgeTimer = urgeTimer,
                                sessions = focusSessions,
                                ifThenPlans = ifThenPlans,
                                onSelectFocusDuration = viewModel::selectFocusDuration,
                                onToggleFocusTimer = viewModel::toggleFocusTimer,
                                onResetFocusTimer = viewModel::resetFocusTimer,
                                onToggleUrgeTimer = viewModel::toggleUrgeTimer,
                                onResetUrgeTimer = viewModel::resetUrgeTimer,
                                onLogUrgeResisted = { note ->
                                    viewModel.logUrgeOrRelapse(
                                        habitId = null,
                                        epochDay = java.time.LocalDate.now().toEpochDay(),
                                        kind = com.example.data.model.UrgeRecordKind.URGE_RESISTED,
                                        intensity = 5,
                                        notes = note
                                    )
                                },
                                onSavePlan = viewModel::saveIfThenPlan,
                                onDeletePlan = viewModel::deleteIfThenPlan,
                                onReturnFromUrgeMode = {
                                    if (!navController.popBackStack()) {
                                        navigateTopLevel(PulseRoutes.HOME)
                                    }
                                }
                            )
                        }

                        composable(PulseRoutes.PROFILE) {
                            ProfileScreen(
                                preferences = preferences,
                                currentUser = currentUser,
                                syncState = syncState,
                                onSetDisplayName = viewModel::setDisplayName,
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetLanguage = viewModel::setLanguage,
                                onSetNotificationsEnabled = viewModel::setNotificationsEnabled,
                                onSyncNow = viewModel::syncNow,
                                onSignedOut = onSignedOut,
                                onOpenAchievements = { navController.navigate(PulseRoutes.ACHIEVEMENTS) },
                                onGenerateJsonBackup = viewModel::generateJsonBackup,
                                onRestoreJsonBackup = viewModel::restoreJsonBackup,
                                onGenerateCsvExport = viewModel::generateCsvExport
                            )
                        }

                        composable(
                            route = PulseRoutes.HABIT_EDITOR,
                            arguments = listOf(navArgument("habitId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { backStackEntry ->
                            val habitId = backStackEntry.arguments?.getString("habitId")
                            val existing = habits.firstOrNull { it.id == habitId }
                            HabitEditorScreen(
                                existingHabit = existing,
                                onSave = { habit ->
                                    viewModel.saveHabit(habit) { navController.popBackStack() }
                                },
                                onDelete = { id ->
                                    viewModel.deleteHabit(id) {
                                        navController.popBackStack(PulseRoutes.HABITS, inclusive = false)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = PulseRoutes.HABIT_DETAIL,
                            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val habitId = backStackEntry.arguments?.getString("habitId") ?: ""
                            val habit = habits.firstOrNull { it.id == habitId }
                            val habitRecords = records.filter { it.habitId == habitId }
                            val habitUrges = urges.filter { it.habitId == habitId }
                            HabitDetailScreen(
                                habit = habit,
                                records = habitRecords,
                                urges = habitUrges,
                                onBack = { navController.popBackStack() },
                                onEditHabit = { id -> navController.navigate(PulseRoutes.habitEditor(id)) },
                                onSaveRecord = viewModel::saveHabitRecord,
                                onLogUrge = viewModel::logUrgeOrRelapse
                            )
                        }

                        composable(PulseRoutes.WEEKLY_REVIEW) {
                            WeeklyReviewScreen(
                                habits = habits,
                                records = records,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(PulseRoutes.ACHIEVEMENTS) {
                            AchievementsGoalsScreen(
                                achievements = achievements,
                                goals = goals,
                                onSaveGoal = viewModel::saveGoal,
                                onIncrementGoal = viewModel::incrementGoalProgress,
                                onDeleteGoal = viewModel::deleteGoal,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
