package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.PulseBackupManager
import com.example.data.local.AchievementEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.IfThenPlanEntity
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.AppLanguage
import com.example.data.model.FocusSessionType
import com.example.data.model.RecordStatus
import com.example.data.model.ThemeMode
import com.example.data.model.UrgeRecordKind
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.PulseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerUiState(
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val selectedMinutes: Int = 25,
    val completedBannerVisible: Boolean = false
)

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Success : SyncState
    data class Error(val message: String) : SyncState
}

class PulseViewModel(
    private val repository: PulseRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val backupManager: PulseBackupManager
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPreferences())

    val habits: StateFlow<List<HabitEntity>> = repository.activeHabitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allRecords: StateFlow<List<HabitRecordEntity>> = repository.allRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allUrges: StateFlow<List<UrgeRecordEntity>> = repository.allUrgesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allGoals: StateFlow<List<GoalEntity>> = repository.allGoalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allFocusSessions: StateFlow<List<FocusSessionEntity>> = repository.allFocusSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allIfThenPlans: StateFlow<List<IfThenPlanEntity>> = repository.allIfThenPlansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.allAchievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _focusTimer = MutableStateFlow(TimerUiState(totalSeconds = 25 * 60, remainingSeconds = 25 * 60, selectedMinutes = 25))
    val focusTimer: StateFlow<TimerUiState> = _focusTimer.asStateFlow()

    private val _urgeTimer = MutableStateFlow(TimerUiState(totalSeconds = 5 * 60, remainingSeconds = 5 * 60, selectedMinutes = 5))
    val urgeTimer: StateFlow<TimerUiState> = _urgeTimer.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var focusTimerJob: Job? = null
    private var urgeTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.recalculateAchievements()
        }
    }

    // --- Habit & Record Actions ---
    fun toggleQuickHabitStatus(habit: HabitEntity, epochDay: Long) {
        viewModelScope.launch {
            repository.toggleQuickHabitStatus(habit, epochDay)
        }
    }

    fun updateNumericalProgress(habit: HabitEntity, epochDay: Long, delta: Int) {
        viewModelScope.launch {
            repository.updateNumericalProgress(habit, epochDay, delta)
        }
    }

    fun saveHabit(habit: HabitEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveHabit(habit)
            onSaved()
        }
    }

    fun deleteHabit(habitId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            onDeleted()
        }
    }

    fun saveHabitRecord(
        habit: HabitEntity,
        epochDay: Long,
        status: RecordStatus,
        progressValue: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.saveHabitRecord(habit, epochDay, status, progressValue, notes)
        }
    }

    fun logUrgeOrRelapse(
        habitId: String?,
        epochDay: Long,
        kind: UrgeRecordKind,
        intensity: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.logUrgeOrRelapse(habitId, epochDay, kind, intensity, notes)
        }
    }

    // --- Goals ---
    fun saveGoal(goal: GoalEntity) {
        viewModelScope.launch { repository.saveGoal(goal) }
    }

    fun incrementGoalProgress(goal: GoalEntity, delta: Int) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentProgress = (goal.currentProgress + delta).coerceIn(0, goal.targetTotal)
            )
            repository.saveGoal(updated)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch { repository.deleteGoal(goalId) }
    }

    // --- If-Then Plans ---
    fun saveIfThenPlan(plan: IfThenPlanEntity) {
        viewModelScope.launch { repository.saveIfThenPlan(plan) }
    }

    fun deleteIfThenPlan(planId: String) {
        viewModelScope.launch { repository.deleteIfThenPlan(planId) }
    }

    // --- Focus Timer ---
    fun selectFocusDuration(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        focusTimerJob?.cancel()
        _focusTimer.value = TimerUiState(
            totalSeconds = clamped * 60,
            remainingSeconds = clamped * 60,
            isRunning = false,
            selectedMinutes = clamped,
            completedBannerVisible = false
        )
    }

    fun toggleFocusTimer() {
        val current = _focusTimer.value
        if (current.isRunning) {
            focusTimerJob?.cancel()
            _focusTimer.value = current.copy(isRunning = false)
        } else {
            val startRem = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
            _focusTimer.value = current.copy(
                remainingSeconds = startRem,
                isRunning = true,
                completedBannerVisible = false
            )
            focusTimerJob?.cancel()
            focusTimerJob = viewModelScope.launch {
                while (_focusTimer.value.remainingSeconds > 0 && _focusTimer.value.isRunning) {
                    delay(1000L)
                    val cur = _focusTimer.value
                    if (!cur.isRunning) break
                    val next = (cur.remainingSeconds - 1).coerceAtLeast(0)
                    if (next == 0) {
                        _focusTimer.value = cur.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            completedBannerVisible = true
                        )
                        repository.logFocusSession(
                            durationMinutes = cur.selectedMinutes,
                            completed = true,
                            type = FocusSessionType.FOCUS.name
                        )
                        break
                    } else {
                        _focusTimer.value = cur.copy(remainingSeconds = next)
                    }
                }
            }
        }
    }

    fun resetFocusTimer() {
        focusTimerJob?.cancel()
        val cur = _focusTimer.value
        _focusTimer.value = cur.copy(
            remainingSeconds = cur.totalSeconds,
            isRunning = false,
            completedBannerVisible = false
        )
    }

    // --- Urge 5-Minute Timer ---
    fun toggleUrgeTimer() {
        val current = _urgeTimer.value
        if (current.isRunning) {
            urgeTimerJob?.cancel()
            _urgeTimer.value = current.copy(isRunning = false)
        } else {
            val startRem = if (current.remainingSeconds <= 0) 5 * 60 else current.remainingSeconds
            _urgeTimer.value = current.copy(
                remainingSeconds = startRem,
                isRunning = true,
                completedBannerVisible = false
            )
            urgeTimerJob?.cancel()
            urgeTimerJob = viewModelScope.launch {
                while (_urgeTimer.value.remainingSeconds > 0 && _urgeTimer.value.isRunning) {
                    delay(1000L)
                    val cur = _urgeTimer.value
                    if (!cur.isRunning) break
                    val next = (cur.remainingSeconds - 1).coerceAtLeast(0)
                    if (next == 0) {
                        _urgeTimer.value = cur.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            completedBannerVisible = true
                        )
                        repository.logFocusSession(
                            durationMinutes = 5,
                            completed = true,
                            type = FocusSessionType.URGE_SURF.name
                        )
                        break
                    } else {
                        _urgeTimer.value = cur.copy(remainingSeconds = next)
                    }
                }
            }
        }
    }

    fun resetUrgeTimer() {
        urgeTimerJob?.cancel()
        _urgeTimer.value = TimerUiState(
            totalSeconds = 5 * 60,
            remainingSeconds = 5 * 60,
            isRunning = false,
            selectedMinutes = 5,
            completedBannerVisible = false
        )
    }

    // --- Preferences & Sync & Backup ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefsRepo.setLanguage(language) }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch { prefsRepo.setDisplayName(name) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setNotificationsEnabled(enabled)
            repository.rescheduleAllReminders()
        }
    }

    fun setOfflineModeEntered(entered: Boolean) {
        viewModelScope.launch { prefsRepo.setOfflineModeEntered(entered) }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = repository.syncWithCloud()
            _syncState.value = result.fold(
                onSuccess = { SyncState.Success },
                onFailure = { SyncState.Error(it.localizedMessage ?: "Sync failed") }
            )
        }
    }

    suspend fun generateJsonBackup(): String = backupManager.createJsonBackup()
    suspend fun restoreJsonBackup(json: String): Result<Int> {
        val result = backupManager.restoreFromJsonBackup(json)
        if (result.isSuccess) {
            repository.recalculateAchievements()
            repository.rescheduleAllReminders()
        }
        return result
    }
    suspend fun generateCsvExport(): String = backupManager.createCsvExport()

    override fun onCleared() {
        super.onCleared()
        focusTimerJob?.cancel()
        urgeTimerJob?.cancel()
    }
}
