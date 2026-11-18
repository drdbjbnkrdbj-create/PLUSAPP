package com.example.data.model

enum class HabitCategory {
    HEALTH,
    LEARNING,
    FITNESS,
    MINDFULNESS,
    PRODUCTIVITY,
    FINANCE,
    SOCIAL,
    OTHER;

    companion object {
        fun fromNameOrDefault(raw: String?): HabitCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class HabitType {
    BUILD,
    QUIT;

    companion object {
        fun fromNameOrDefault(raw: String?): HabitType =
            entries.firstOrNull { it.name == raw } ?: BUILD
    }
}

enum class TrackingMode {
    BOOLEAN,
    NUMERICAL;

    companion object {
        fun fromNameOrDefault(raw: String?): TrackingMode =
            entries.firstOrNull { it.name == raw } ?: BOOLEAN
    }
}

enum class ScheduleType {
    DAILY,
    CUSTOM_DAYS;

    companion object {
        fun fromNameOrDefault(raw: String?): ScheduleType =
            entries.firstOrNull { it.name == raw } ?: DAILY
    }
}

enum class RecordStatus {
    COMPLETED,
    MISSED,
    SKIPPED,
    CLEAN_DAY,
    RELAPSE;

    companion object {
        fun fromNameOrDefault(raw: String?): RecordStatus =
            entries.firstOrNull { it.name == raw } ?: MISSED
    }
}

enum class UrgeRecordKind {
    URGE_RESISTED,
    RELAPSE;

    companion object {
        fun fromNameOrDefault(raw: String?): UrgeRecordKind =
            entries.firstOrNull { it.name == raw } ?: URGE_RESISTED
    }
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    PAUSED;

    companion object {
        fun fromNameOrDefault(raw: String?): GoalStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class FocusSessionType {
    FOCUS,
    URGE_SURF;

    companion object {
        fun fromNameOrDefault(raw: String?): FocusSessionType =
            entries.firstOrNull { it.name == raw } ?: FOCUS
    }
}

enum class ThemeMode {
    DARK,
    LIGHT;

    companion object {
        fun fromNameOrDefault(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DARK
    }
}

enum class AppLanguage(val code: String) {
    AR("ar"),
    EN("en");

    companion object {
        fun fromCodeOrDefault(raw: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: EN
    }
}
