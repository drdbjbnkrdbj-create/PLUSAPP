package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AchievementEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitRecordEntity
import com.example.data.local.IfThenPlanEntity
import com.example.data.local.PulseDatabase
import com.example.data.local.UrgeRecordEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class PulseBackupManager(
    private val db: PulseDatabase,
    private val prefsRepo: UserPreferencesRepository
) {
    suspend fun createJsonBackup(): String {
        val prefs = prefsRepo.preferencesFlow.first()
        val habits = db.habitDao().getAllHabitsOnce()
        val records = db.habitRecordDao().getAllRecordsOnce()
        val urges = db.urgeRecordDao().getAllUrgeRecordsOnce()
        val goals = db.goalDao().getAllGoalsOnce()
        val sessions = db.focusSessionDao().getAllSessionsOnce()
        val plans = db.ifThenPlanDao().getAllPlansOnce()
        val achievements = db.achievementDao().getAllAchievementsOnce()

        return JSONObject().apply {
            put("app", "PULSE")
            put("schemaVersion", 1)
            put("exportedAtMs", System.currentTimeMillis())
            put("preferences", JSONObject().apply {
                put("displayName", prefs.displayName)
                put("themeMode", prefs.themeMode.name)
                put("language", prefs.language.code)
                put("notificationsEnabled", prefs.notificationsEnabled)
            })
            put("habits", JSONArray().apply {
                habits.forEach { h ->
                    put(JSONObject().apply {
                        put("id", h.id)
                        put("name", h.name)
                        put("description", h.description)
                        put("category", h.category)
                        put("habitType", h.habitType)
                        put("trackingMode", h.trackingMode)
                        put("targetValue", h.targetValue)
                        put("unit", h.unit)
                        put("iconKey", h.iconKey)
                        put("colorHex", h.colorHex)
                        put("scheduleType", h.scheduleType)
                        put("scheduleDaysCsv", h.scheduleDaysCsv)
                        put("reminderEnabled", h.reminderEnabled)
                        put("reminderTime", h.reminderTime)
                        put("archived", h.archived)
                        put("createdEpochDay", h.createdEpochDay)
                        put("updatedAtMs", h.updatedAtMs)
                    })
                }
            })
            put("habitRecords", JSONArray().apply {
                records.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("habitId", r.habitId)
                        put("epochDay", r.epochDay)
                        put("status", r.status)
                        put("progressValue", r.progressValue)
                        put("notes", r.notes)
                        put("recordedTimestampMs", r.recordedTimestampMs)
                        put("updatedAtMs", r.updatedAtMs)
                    })
                }
            })
            put("urgeRecords", JSONArray().apply {
                urges.forEach { u ->
                    put(JSONObject().apply {
                        put("id", u.id)
                        put("habitId", u.habitId ?: JSONObject.NULL)
                        put("epochDay", u.epochDay)
                        put("timeString", u.timeString)
                        put("recordKind", u.recordKind)
                        put("intensity", u.intensity)
                        put("notes", u.notes)
                        put("recordedTimestampMs", u.recordedTimestampMs)
                    })
                }
            })
            put("goals", JSONArray().apply {
                goals.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("title", g.title)
                        put("description", g.description)
                        put("targetEpochDay", g.targetEpochDay)
                        put("currentProgress", g.currentProgress)
                        put("targetTotal", g.targetTotal)
                        put("status", g.status)
                        put("createdAtMs", g.createdAtMs)
                        put("updatedAtMs", g.updatedAtMs)
                    })
                }
            })
            put("focusSessions", JSONArray().apply {
                sessions.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("durationMinutes", s.durationMinutes)
                        put("epochDay", s.epochDay)
                        put("completed", s.completed)
                        put("sessionType", s.sessionType)
                        put("startedAtMs", s.startedAtMs)
                    })
                }
            })
            put("ifThenPlans", JSONArray().apply {
                plans.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("triggerText", p.triggerText)
                        put("responseText", p.responseText)
                        put("createdAtMs", p.createdAtMs)
                        put("updatedAtMs", p.updatedAtMs)
                    })
                }
            })
            put("achievements", JSONArray().apply {
                achievements.forEach { a ->
                    put(JSONObject().apply {
                        put("key", a.key)
                        put("unlockedAtMs", a.unlockedAtMs ?: JSONObject.NULL)
                        put("progressCurrent", a.progressCurrent)
                        put("progressTarget", a.progressTarget)
                    })
                }
            })
        }.toString(2)
    }

    suspend fun restoreFromJsonBackup(jsonText: String): Result<Int> {
        return try {
            val root = JSONObject(jsonText)
            if (root.optString("app") != "PULSE") {
                return Result.failure(IllegalArgumentException("Invalid PULSE backup file"))
            }

            val habitsArr = root.optJSONArray("habits") ?: JSONArray()
            val habits = (0 until habitsArr.length()).map { i ->
                val o = habitsArr.getJSONObject(i)
                HabitEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    description = o.optString("description", ""),
                    category = o.optString("category", "OTHER"),
                    habitType = o.optString("habitType", "BUILD"),
                    trackingMode = o.optString("trackingMode", "BOOLEAN"),
                    targetValue = o.optInt("targetValue", 1).coerceAtLeast(1),
                    unit = o.optString("unit", ""),
                    iconKey = o.optString("iconKey", "check"),
                    colorHex = o.optString("colorHex", "#27C6D9"),
                    scheduleType = o.optString("scheduleType", "DAILY"),
                    scheduleDaysCsv = o.optString("scheduleDaysCsv", "1,2,3,4,5,6,7"),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderTime = o.optString("reminderTime", "09:00"),
                    archived = o.optBoolean("archived", false),
                    createdEpochDay = o.optLong("createdEpochDay", LocalDate.now().toEpochDay()),
                    updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis())
                )
            }
            val habitIds = habits.map { it.id }.toSet()

            val recArr = root.optJSONArray("habitRecords") ?: JSONArray()
            val records = (0 until recArr.length()).mapNotNull { i ->
                val o = recArr.getJSONObject(i)
                val hid = o.getString("habitId")
                if (hid !in habitIds) null else HabitRecordEntity(
                    id = o.getString("id"),
                    habitId = hid,
                    epochDay = o.getLong("epochDay"),
                    status = o.optString("status", "COMPLETED"),
                    progressValue = o.optInt("progressValue", 0),
                    notes = o.optString("notes", ""),
                    recordedTimestampMs = o.optLong("recordedTimestampMs", System.currentTimeMillis()),
                    updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis())
                )
            }

            val urgeArr = root.optJSONArray("urgeRecords") ?: JSONArray()
            val urges = (0 until urgeArr.length()).map { i ->
                val o = urgeArr.getJSONObject(i)
                val rawHabitId = if (o.isNull("habitId")) null else o.optString("habitId")
                UrgeRecordEntity(
                    id = o.getString("id"),
                    habitId = rawHabitId?.takeIf { it in habitIds },
                    epochDay = o.getLong("epochDay"),
                    timeString = o.optString("timeString", "00:00"),
                    recordKind = o.optString("recordKind", "URGE_RESISTED"),
                    intensity = o.optInt("intensity", 5).coerceIn(1, 10),
                    notes = o.optString("notes", ""),
                    recordedTimestampMs = o.optLong("recordedTimestampMs", System.currentTimeMillis())
                )
            }

            val goalArr = root.optJSONArray("goals") ?: JSONArray()
            val goals = (0 until goalArr.length()).map { i ->
                val o = goalArr.getJSONObject(i)
                GoalEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    description = o.optString("description", ""),
                    targetEpochDay = o.optLong("targetEpochDay", LocalDate.now().plusDays(30).toEpochDay()),
                    currentProgress = o.optInt("currentProgress", 0),
                    targetTotal = o.optInt("targetTotal", 1).coerceAtLeast(1),
                    status = o.optString("status", "ACTIVE"),
                    createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
                    updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis())
                )
            }

            val sessionArr = root.optJSONArray("focusSessions") ?: JSONArray()
            val sessions = (0 until sessionArr.length()).map { i ->
                val o = sessionArr.getJSONObject(i)
                FocusSessionEntity(
                    id = o.getString("id"),
                    durationMinutes = o.optInt("durationMinutes", 25).coerceAtLeast(1),
                    epochDay = o.optLong("epochDay", LocalDate.now().toEpochDay()),
                    completed = o.optBoolean("completed", true),
                    sessionType = o.optString("sessionType", "FOCUS"),
                    startedAtMs = o.optLong("startedAtMs", System.currentTimeMillis())
                )
            }

            val planArr = root.optJSONArray("ifThenPlans") ?: JSONArray()
            val plans = (0 until planArr.length()).map { i ->
                val o = planArr.getJSONObject(i)
                IfThenPlanEntity(
                    id = o.getString("id"),
                    triggerText = o.getString("triggerText"),
                    responseText = o.getString("responseText"),
                    createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
                    updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis())
                )
            }

            val achArr = root.optJSONArray("achievements") ?: JSONArray()
            val achievements = (0 until achArr.length()).map { i ->
                val o = achArr.getJSONObject(i)
                AchievementEntity(
                    key = o.getString("key"),
                    unlockedAtMs = if (o.isNull("unlockedAtMs")) null else o.optLong("unlockedAtMs"),
                    progressCurrent = o.optInt("progressCurrent", 0),
                    progressTarget = o.optInt("progressTarget", 1)
                )
            }

            db.withTransaction {
                db.habitRecordDao().deleteAllRecords()
                db.urgeRecordDao().deleteAllUrgeRecords()
                db.habitDao().deleteAllHabits()
                db.goalDao().deleteAllGoals()
                db.focusSessionDao().deleteAllSessions()
                db.ifThenPlanDao().deleteAllPlans()
                db.achievementDao().deleteAllAchievements()

                db.habitDao().upsertHabits(habits)
                db.habitRecordDao().upsertRecords(records)
                db.urgeRecordDao().upsertUrgeRecords(urges)
                db.goalDao().upsertGoals(goals)
                db.focusSessionDao().upsertSessions(sessions)
                db.ifThenPlanDao().upsertPlans(plans)
                db.achievementDao().upsertAchievements(achievements)
            }

            root.optJSONObject("preferences")?.let { p ->
                prefsRepo.restorePreferences(
                    displayName = p.optString("displayName", ""),
                    themeMode = ThemeMode.fromNameOrDefault(p.optString("themeMode")),
                    language = AppLanguage.fromCodeOrDefault(p.optString("language")),
                    notificationsEnabled = p.optBoolean("notificationsEnabled", true)
                )
            }

            Result.success(habits.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCsvExport(): String {
        val habits = db.habitDao().getAllHabitsOnce().associateBy { it.id }
        val records = db.habitRecordDao().getAllRecordsOnce()
        val sb = StringBuilder()
        sb.appendLine("date,habit_name,habit_type,category,status,progress_value,target_value,notes")
        records.forEach { rec ->
            val h = habits[rec.habitId]
            val date = LocalDate.ofEpochDay(rec.epochDay).toString()
            val name = csvEscape(h?.name ?: rec.habitId)
            val type = h?.habitType ?: ""
            val category = h?.category ?: ""
            val notes = csvEscape(rec.notes)
            sb.appendLine("$date,$name,$type,$category,${rec.status},${rec.progressValue},${h?.targetValue ?: 1},$notes")
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$cleaned\""
    }
}
