package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutFromPulse
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.notifications.PulseNotificationHelper
import com.example.ui.components.PulseCard
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors
import com.example.ui.viewmodel.SyncState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    preferences: UserPreferences,
    currentUser: FirebaseUser?,
    syncState: SyncState,
    onSetDisplayName: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenAchievements: () -> Unit,
    onGenerateJsonBackup: suspend () -> String,
    onRestoreJsonBackup: suspend (String) -> Result<Int>,
    onGenerateCsvExport: suspend () -> String
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotificationsEnabled(granted)
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = withContext(Dispatchers.IO) { onGenerateJsonBackup() }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                feedbackBanner = if (strings.language == AppLanguage.AR) "تم حفظ النسخة الاحتياطية" else "Backup saved successfully"
            }
        }
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                }
                val res = onRestoreJsonBackup(text)
                feedbackBanner = res.fold(
                    onSuccess = { count ->
                        if (strings.language == AppLanguage.AR) "تمت استعادة $count عادة بنجاح" else "Restored $count habit(s)"
                    },
                    onFailure = { err -> err.localizedMessage ?: "Restore failed" }
                )
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = withContext(Dispatchers.IO) { onGenerateCsvExport() }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }
                feedbackBanner = if (strings.language == AppLanguage.AR) "تم تصدير السجلات بنجاح" else "CSV exported successfully"
            }
        }
    }

    val displayName = preferences.displayName.ifBlank {
        currentUser?.displayName ?: strings.defaultUserName
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = strings.profileTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (feedbackBanner != null) {
                item {
                    PulseCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = colors.primaryAccent
                    ) {
                        Text(
                            text = feedbackBanner!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.primaryAccent
                        )
                    }
                }
            }

            // Profile Header Card
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryAccent.copy(alpha = 0.2f))
                                    .border(2.dp, colors.primaryAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase() ?: "P",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = colors.primaryAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.profileSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier.testTag("profile_edit_name_btn")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = strings.editDisplayName, tint = colors.primaryAccent)
                        }
                    }
                }
            }

            // Google Sign-In & Cloud Sync Card
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Rounded.CloudDone else Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = if (currentUser != null) colors.success else colors.primaryAccent
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.cloudSyncTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (currentUser != null) {
                                    strings.syncedAs(currentUser.email ?: currentUser.displayName ?: currentUser.uid)
                                } else {
                                    strings.offlineLocalOnly
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (currentUser == null) {
                        Button(
                            onClick = {
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onSyncNow()
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        feedbackBanner = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isSigningIn = false }
                                )
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_google_signin_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = Color(0xFF0A1420)
                            )
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF0A1420), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.signInWithGoogle, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onSyncNow,
                                enabled = syncState !is SyncState.Syncing,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primaryAccent,
                                    contentColor = Color(0xFF0A1420)
                                )
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF0A1420), strokeWidth = 2.dp)
                                } else {
                                    Text(strings.syncNow, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    signOutFromPulse(credentialManager, scope) {
                                        onSignedOut()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.failure.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = colors.failure, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.signOut, color = colors.failure)
                            }
                        }
                    }
                }
            }

            // Achievements & Goals Shortcut
            item {
                SettingRowCard(
                    icon = Icons.Rounded.EmojiEvents,
                    iconTint = colors.warning,
                    title = strings.achievementsAndGoalsTitle,
                    subtitle = strings.longTermGoalsSection,
                    onClick = onOpenAchievements
                )
            }

            // Theme Setting
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (preferences.themeMode == ThemeMode.DARK) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = colors.primaryAccent
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(strings.themeSetting, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text(
                                    if (preferences.themeMode == ThemeMode.DARK) strings.darkTheme else strings.lightTheme,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = preferences.themeMode == ThemeMode.DARK,
                            onCheckedChange = { dark ->
                                onSetThemeMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                            },
                            modifier = Modifier.testTag("profile_theme_switch")
                        )
                    }
                }
            }

            // Language Setting (AR RTL / EN LTR)
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = colors.primaryAccent)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.languageSetting, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            AppLanguage.AR to "العربية (RTL)",
                            AppLanguage.EN to "English (LTR)"
                        ).forEach { (lang, label) ->
                            val selected = preferences.language == lang
                            Surface(
                                onClick = { onSetLanguage(lang) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("profile_lang_${lang.code}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.primaryAccent.copy(alpha = 0.2f) else colors.secondaryBackground,
                                border = BorderStroke(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) colors.primaryAccent else colors.cardBorder
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) colors.primaryAccent else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notifications & Exact Alarm Preferences
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = colors.primaryAccent)
                            Spacer(Modifier.width(12.dp))
                            Text(strings.notificationsSetting, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        }
                        Switch(
                            checked = preferences.notificationsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !PulseNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetNotificationsEnabled(enable)
                                }
                            },
                            modifier = Modifier.testTag("profile_notifications_switch")
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.exactAlarmNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !PulseNotificationHelper.canScheduleExactAlarms(context)) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.openExactAlarmSettings)
                        }
                    }
                }
            }

            // Backup, Restore & Export
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(strings.backupRestoreSetting, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { saveJsonLauncher.launch("pulse_backup.json") },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.createBackup, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { restoreJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restoreBackup, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { exportCsvLauncher.launch("pulse_records.csv") },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportDataSetting)
                    }
                }
            }

            // App & Privacy Info
            item {
                SettingRowCard(
                    icon = Icons.Rounded.Info,
                    iconTint = colors.primaryAccent,
                    title = strings.appInfoSetting,
                    subtitle = "PULSE v1.0 • Room SQLite",
                    onClick = { showInfoDialog = true }
                )
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember(displayName) { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = colors.cardBackground,
            title = { Text(strings.editDisplayName, color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(60) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetDisplayName(nameInput)
                    showEditNameDialog = false
                }) { Text(strings.saveButton, color = colors.primaryAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = colors.cardBackground,
            title = { Text("PULSE", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (strings.language == AppLanguage.AR) {
                        "تطبيق PULSE لإدارة العادات والانضباط.\n\nتُحفظ جميع عاداتك وسجلاتك محلياً في قاعدة بيانات Room SQLite وتعمل بالكامل دون إنترنت. في حال تسجيل الدخول عبر Google، تتم مزامنة بياناتك بشكل خاص وآمن مع حسابك فقط."
                    } else {
                        "PULSE Habit & Discipline Companion.\n\nAll habits, daily records, urges, goals, and focus sessions are stored locally in Room SQLite and work 100% offline. Optional Google Sign-In syncs your data privately to your personal Firestore space."
                    },
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("OK", color = colors.primaryAccent) }
            }
        )
    }
}

@Composable
private fun SettingRowCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalPulseColors.current
    PulseCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
    }
}
