package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.pulsePrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_preferences")

data class UserPreferences(
    val displayName: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.EN,
    val notificationsEnabled: Boolean = true,
    val offlineModeEntered: Boolean = false,
    val lastSyncedAtMs: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val OFFLINE_MODE_ENTERED = booleanPreferencesKey("offline_mode_entered")
        val LAST_SYNCED_AT_MS = longPreferencesKey("last_synced_at_ms")
    }

    private fun defaultLanguage(): AppLanguage {
        val sysLang = Locale.getDefault().language
        return if (sysLang.equals("ar", ignoreCase = true)) AppLanguage.AR else AppLanguage.EN
    }

    val preferencesFlow: Flow<UserPreferences> = context.pulsePrefsDataStore.data.map { prefs ->
        UserPreferences(
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            themeMode = ThemeMode.fromNameOrDefault(prefs[Keys.THEME_MODE]),
            language = prefs[Keys.LANGUAGE]?.let { AppLanguage.fromCodeOrDefault(it) } ?: defaultLanguage(),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            offlineModeEntered = prefs[Keys.OFFLINE_MODE_ENTERED] ?: false,
            lastSyncedAtMs = prefs[Keys.LAST_SYNCED_AT_MS] ?: 0L
        )
    }

    suspend fun setDisplayName(name: String) {
        context.pulsePrefsDataStore.edit { it[Keys.DISPLAY_NAME] = name.trim().take(60) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.pulsePrefsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.pulsePrefsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.pulsePrefsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setOfflineModeEntered(entered: Boolean) {
        context.pulsePrefsDataStore.edit { it[Keys.OFFLINE_MODE_ENTERED] = entered }
    }

    suspend fun setLastSyncedAtMs(timestampMs: Long) {
        context.pulsePrefsDataStore.edit { it[Keys.LAST_SYNCED_AT_MS] = timestampMs }
    }

    suspend fun restorePreferences(
        displayName: String,
        themeMode: ThemeMode,
        language: AppLanguage,
        notificationsEnabled: Boolean
    ) {
        context.pulsePrefsDataStore.edit {
            it[Keys.DISPLAY_NAME] = displayName.trim().take(60)
            it[Keys.THEME_MODE] = themeMode.name
            it[Keys.LANGUAGE] = language.code
            it[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
        }
    }
}
