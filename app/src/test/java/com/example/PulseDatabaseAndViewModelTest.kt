package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.PulseBackupManager
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.IfThenPlanEntity
import com.example.data.local.PulseDatabase
import com.example.data.model.AppLanguage
import com.example.data.model.GoalStatus
import com.example.data.model.HabitCategory
import com.example.data.model.HabitType
import com.example.data.model.RecordStatus
import com.example.data.model.ThemeMode
import com.example.data.model.TrackingMode
import com.example.data.model.UrgeRecordKind
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.PulseRepository
import com.example.ui.viewmodel.PulseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PulseDatabaseAndViewModelTest {

    private lateinit var context: Context
    private lateinit var db: PulseDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: PulseBackupManager
    private lateinit var repository: PulseRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        val inlineExecutor = Executor { runnable -> runnable.run() }
        db = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(inlineExecutor)
            .setTransactionExecutor(inlineExecutor)
            .build()
        prefsRepo = UserPreferencesRepository(context)
        backupManager = PulseBackupManager(db, prefsRepo)
        repository = PulseRepository(
            db = db,
            prefsRepo = prefsRepo,
            firestoreSync = null,
            reminderScheduler = null
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `room database persists habits, enforces unique habit-day record key, and cascades deletions`() = runTest(testDispatcher) {
        val today = LocalDate.now().toEpochDay()
        val habit = repository.saveHabit(
            HabitEntity(
                id = "habit_1",
                name = "Drink Water",
                category = HabitCategory.HEALTH.name,
                habitType = HabitType.BUILD.name,
                trackingMode = TrackingMode.NUMERICAL.name,
                targetValue = 8,
                unit = "cups",
                createdEpochDay = today
            )
        )

        repository.updateNumericalProgress(habit, today, 3)
        repository.updateNumericalProgress(habit, today, 5)

        val records = db.habitRecordDao().getAllRecordsOnce()
        // Unique (habitId, epochDay) guarantees single row upserted, never duplicates
        assertEquals(1, records.size)
        assertEquals(8, records.first().progressValue)
        assertEquals(RecordStatus.COMPLETED.name, records.first().status)

        repository.logUrgeOrRelapse(
            habitId = habit.id,
            epochDay = today,
            kind = UrgeRecordKind.URGE_RESISTED,
            intensity = 6,
            notes = "Walked outside"
        )
        assertEquals(1, db.urgeRecordDao().getAllUrgeRecordsOnce().size)

        // Delete parent habit -> habit_records cascade delete, urge_records keep entry with habitId = null
        repository.deleteHabit(habit.id)
        assertTrue(db.habitRecordDao().getAllRecordsOnce().isEmpty())
        val urges = db.urgeRecordDao().getAllUrgeRecordsOnce()
        assertEquals(1, urges.size)
        assertNull(urges.first().habitId)
    }

    @Test
    fun `data survives closing and reopening disk-backed SQLite database`() = runTest(testDispatcher) {
        val dbFile = File(context.filesDir, "pulse_persistence_verify.db")
        if (dbFile.exists()) dbFile.delete()

        val diskDb1 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        val repo1 = PulseRepository(diskDb1, prefsRepo, null, null)

        val createdDay = LocalDate.of(2026, 3, 1).toEpochDay()
        val saved = repo1.saveHabit(
            HabitEntity(
                id = "persist_h1",
                name = "Morning Run",
                habitType = HabitType.BUILD.name,
                createdEpochDay = createdDay
            )
        )
        repo1.toggleQuickHabitStatus(saved, createdDay)
        repo1.saveGoal(
            GoalEntity(
                id = "persist_g1",
                title = "Read 12 books",
                targetEpochDay = createdDay + 90,
                currentProgress = 4,
                targetTotal = 12
            )
        )
        repo1.saveIfThenPlan(
            IfThenPlanEntity(
                id = "persist_p1",
                triggerText = "If distracted at desk",
                responseText = "Stand up and breathe 5 times"
            )
        )
        diskDb1.close()

        // Reopen database
        val diskDb2 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        val loadedHabit = diskDb2.habitDao().getHabitById("persist_h1")
        val loadedRecord = diskDb2.habitRecordDao().getRecord("persist_h1", createdDay)
        val loadedGoals = diskDb2.goalDao().getAllGoalsOnce()
        val loadedPlans = diskDb2.ifThenPlanDao().getAllPlansOnce()
        diskDb2.close()
        dbFile.delete()

        assertNotNull(loadedHabit)
        assertEquals("Morning Run", loadedHabit?.name)
        assertEquals(RecordStatus.COMPLETED.name, loadedRecord?.status)
        assertEquals(1, loadedGoals.size)
        assertEquals(4, loadedGoals.first().currentProgress)
        assertEquals(1, loadedPlans.size)
    }

    @Test
    fun `json backup and restore preserves full state and preferences and csv export formats rows`() = runTest(testDispatcher) {
        val today = LocalDate.of(2026, 4, 15).toEpochDay()
        prefsRepo.setDisplayName("Ahmad")
        prefsRepo.setLanguage(AppLanguage.AR)
        prefsRepo.setThemeMode(ThemeMode.LIGHT)

        val h = repository.saveHabit(
            HabitEntity(
                id = "bk_h1",
                name = "No Social Media",
                habitType = HabitType.QUIT.name,
                createdEpochDay = today
            )
        )
        repository.toggleQuickHabitStatus(h, today)

        val json = backupManager.createJsonBackup()
        val csv = backupManager.createCsvExport()
        assertTrue(csv.contains("No Social Media"))
        assertTrue(csv.contains("CLEAN_DAY"))

        // Wipe database & reset preferences
        db.habitRecordDao().deleteAllRecords()
        db.habitDao().deleteAllHabits()
        prefsRepo.setDisplayName("Empty")
        prefsRepo.setLanguage(AppLanguage.EN)

        val restoreResult = backupManager.restoreFromJsonBackup(json)
        assertTrue(restoreResult.isSuccess)
        assertEquals(1, restoreResult.getOrNull())

        val restoredHabits = db.habitDao().getAllHabitsOnce()
        val restoredRecords = db.habitRecordDao().getAllRecordsOnce()
        val restoredPrefs = prefsRepo.preferencesFlow.first()
        assertEquals(1, restoredHabits.size)
        assertEquals(1, restoredRecords.size)
        assertEquals("Ahmad", restoredPrefs.displayName)
        assertEquals(AppLanguage.AR, restoredPrefs.language)
        assertEquals(ThemeMode.LIGHT, restoredPrefs.themeMode)
    }

    @Test
    fun `viewmodel manages quit habit relapse logging, goal auto-completion, and focus timer state`() = runTest(testDispatcher) {
        val vm = PulseViewModel(repository, prefsRepo, backupManager)
        val today = LocalDate.now().toEpochDay()

        val quitHabit = HabitEntity(
            id = "vm_quit",
            name = "Quit Caffeine",
            habitType = HabitType.QUIT.name,
            createdEpochDay = today
        )
        vm.saveHabit(quitHabit)
        testDispatcher.scheduler.advanceUntilIdle()
        withContext(Dispatchers.Default) {
            withTimeout(5000L) {
                db.habitDao().observeHabitById("vm_quit").first { it != null }
            }
        }

        // First toggle -> CLEAN_DAY; second toggle -> RELAPSE (which also writes UrgeRecord)
        vm.toggleQuickHabitStatus(quitHabit, today)
        testDispatcher.scheduler.advanceUntilIdle()
        val cleanRec = withContext(Dispatchers.Default) {
            withTimeout(5000L) {
                db.habitRecordDao().observeRecordsForHabit("vm_quit").first { list ->
                    list.any { it.epochDay == today && it.status == RecordStatus.CLEAN_DAY.name }
                }
            }
        }
        assertEquals(1, cleanRec.size)

        vm.toggleQuickHabitStatus(quitHabit, today)
        testDispatcher.scheduler.advanceUntilIdle()
        withContext(Dispatchers.Default) {
            withTimeout(5000L) {
                db.habitRecordDao().observeRecordsForHabit("vm_quit").first { list ->
                    list.any { it.epochDay == today && it.status == RecordStatus.RELAPSE.name }
                }
            }
        }
        val urges = withContext(Dispatchers.Default) {
            withTimeout(5000L) {
                db.urgeRecordDao().observeUrgesForHabit("vm_quit").first { it.isNotEmpty() }
            }
        }
        assertEquals(1, urges.size)

        // Goal progress auto-marks COMPLETED when reaching targetTotal
        val goal = GoalEntity(
            id = "vm_goal",
            title = "30 Day Discipline",
            targetEpochDay = today + 30,
            currentProgress = 2,
            targetTotal = 3
        )
        vm.saveGoal(goal)
        testDispatcher.scheduler.advanceUntilIdle()
        withContext(Dispatchers.Default) {
            withTimeout(5000L) {
                db.goalDao().observeAllGoals().first { list -> list.any { it.id == "vm_goal" } }
            }
        }

        vm.incrementGoalProgress(goal, 1)
        testDispatcher.scheduler.advanceUntilIdle()
        val savedGoals = withContext(Dispatchers.Default) {
            withTimeout(5000L) {
                db.goalDao().observeAllGoals().first { list ->
                    list.any { it.id == "vm_goal" && it.status == GoalStatus.COMPLETED.name }
                }
            }
        }
        assertEquals(3, savedGoals.first { it.id == "vm_goal" }.currentProgress)

        // Focus timer selection & reset
        vm.selectFocusDuration(50)
        assertEquals(50 * 60, vm.focusTimer.value.totalSeconds)
        assertEquals(50 * 60, vm.focusTimer.value.remainingSeconds)
        vm.resetFocusTimer()
        assertEquals(50 * 60, vm.focusTimer.value.remainingSeconds)
    }
}
