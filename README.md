# PULSE Android

**Your Habits. Your Discipline. Your Progress.**

PULSE is a native Android habit tracking, behavioral discipline, urge management, and productivity application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **MVVM** architecture. It is designed offline-first with local SQLite persistence (**Room**) and **DataStore**, optional **Google Sign-In + Cloud Firestore** synchronization, smart local **AlarmManager** reminders, full **English (LTR)** and **Arabic (RTL)** localization, and **Dark** (default) / **Light** themes.

---

## Key Features

- **Home Dashboard**: Current date and personalized greeting, circular daily completion percentage ring, 4-metric overview strip, today's scheduled Build and Quit habits with instant completion or numerical steppers, motivational hero banner with Focus & Urge Mode shortcuts, and empty states.
- **Habit Management**: Create, edit, and delete habits with custom icons, accent colors, categories, positive (Build) or quit (Quit) tracking logic, boolean or numerical targets, Daily or Custom weekday schedules, and per-habit notification reminders.
- **Calendar & History**: Per-habit monthly calendar with Green (success), Red (missed/relapse), and Neutral (unrecorded) indicators, clear legend, month navigation, day inspection, notes, and urge/relapse intensity logging.
- **Analytics & Insights**: Today, Week, and Month views with commitment ring and previous-period delta, interactive custom Canvas bar chart comparing successful vs. missed days across habits, longest streak tracking, habit rankings, and pattern detection computed solely from stored local records. Includes a dedicated **Weekly Review** screen.
- **Focus & Urge Management Mode**:
  - Configurable circular countdown timer (5, 25, 50 min, or custom duration) with session history.
  - Calming Urge Management interface with misty mountain backdrop, animated 4-4-4 breathing circle, 5-minute urge-surfing timer, urge resistance logging, and personal **If-Then Plans**.
- **Achievements & Long-Term Goals**: Permanent milestone badges (7, 30, 60, 90 days, First Habit, Focus Sessions, Urges Resisted) preserved permanently once unlocked even after later relapses, alongside trackable long-term goals.
- **Profile, Backup & Localization**: Instant Arabic (RTL) / English (LTR) switching, Dark / Light themes, exact & inexact reminder controls, JSON full backup & restore, CSV record export via Storage Access Framework, and optional Google Sign-In / Cloud Firestore sync.

---

## Architecture & Database Design

PULSE follows **MVVM** with a unidirectional reactive flow (`Room Flow` / `DataStore Flow` → `PulseRepository` → `PulseViewModel` → `Compose UI`):

### Local Persistence (`PulseDatabase`, SQLite via Room)
1. **`habits`**: Stores habit definitions (`id`, `name`, `description`, `category`, `habitType` [`BUILD`|`QUIT`], `trackingMode` [`BOOLEAN`|`NUMERICAL`], `targetValue`, `unit`, `iconKey`, `colorHex`, `scheduleType`, `scheduleDaysCsv`, `reminderEnabled`, `reminderTime`, `archived`, `createdEpochDay`, `updatedAtMs`).
2. **`habit_records`**: Stores per-day check-ins linked by foreign key to `habits(id)` with `ON DELETE CASCADE` and a **unique composite index on `(habitId, epochDay)`** so daily edits update cleanly without duplicate records, preserving full multi-month/multi-year history.
3. **`urge_records`**: Logs resisted urges and relapses (`habitId` FK with `ON DELETE SET NULL`, `epochDay`, `timeString`, `recordKind`, `intensity`, `notes`, `recordedTimestampMs`).
4. **`goals`**: Long-term user goals (`targetEpochDay`, `currentProgress`, `targetTotal`, `status`).
5. **`focus_sessions`**: Completed focus and urge-surfing sessions (`durationMinutes`, `epochDay`, `completed`, `sessionType`, `startedAtMs`).
6. **`if_then_plans`**: Implementation-intention trigger/response plans (`triggerText`, `responseText`).
7. **`achievements`**: Milestone badge progress and permanent `unlockedAtMs` timestamps.

> Non-destructive database schema policy: `PulseDatabase` exports Room schemas and uses explicit `Migration` definitions instead of destructive fallback migrations so historical records are never lost on upgrade.

### Cloud Sync (Optional, Auth-Gated Firestore)
- Local Room SQLite powers 100% of app features offline with no account required.
- When signed in with **Google Sign-In** (Jetpack `CredentialManager`), `FirestoreSyncRepository` mirrors user-scoped subcollections (`/users/{userId}/habits`, `habit_records`, `urge_records`, `goals`, `focus_sessions`, `if_then_plans`) protected by strict zero-trust ownership rules in `firestore.rules`.

---

## Build & Test Instructions

- **Minimum Android Version**: Android 8.0 (API 26)
- **Compile / Target SDK**: API 36

```bash
# Build debug APK
gradle :app:assembleDebug

# Run local JVM unit & Robolectric tests
gradle :app:testDebugUnitTest
```
