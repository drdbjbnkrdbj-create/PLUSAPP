package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.ui.i18n.LocalPulseStrings
import com.example.ui.i18n.pulseStringsFor
import com.example.ui.navigation.PulseMainShell
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.PulseTheme
import com.example.ui.viewmodel.PulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseAppRoot()
        }
    }
}

@Composable
fun PulseAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as PulseApplication
    val prefs by app.preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.UserPreferences()
    )
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    val credentialManager = remember { CredentialManager.create(context) }
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = this
        )
    }

    val pulseStrings = remember(prefs.language) { pulseStringsFor(prefs.language) }

    CompositionLocalProvider(LocalPulseStrings provides pulseStrings) {
        PulseTheme(
            themeMode = prefs.themeMode,
            language = prefs.language
        ) {
            val viewModelKey = currentUser?.uid ?: "offline_local"
            val viewModel: PulseViewModel = viewModel(
                key = viewModelKey,
                factory = viewModelFactory {
                    initializer {
                        val pulseApp = checkNotNull(this[APPLICATION_KEY]) as PulseApplication
                        PulseViewModel(
                            repository = pulseApp.pulseRepository,
                            prefsRepo = pulseApp.preferencesRepository,
                            backupManager = pulseApp.backupManager
                        )
                    }
                }
            )

            if (currentUser == null && !prefs.offlineModeEntered) {
                WelcomeAuthScreen(
                    currentLanguage = prefs.language,
                    onToggleLanguage = viewModel::setLanguage,
                    onGoogleSignInSuccess = {
                        viewModel.syncNow()
                    },
                    onContinueOffline = {
                        viewModel.setOfflineModeEntered(true)
                    }
                )
            } else {
                key(viewModelKey) {
                    PulseMainShell(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onSignedOut = {
                            viewModel.setOfflineModeEntered(true)
                        }
                    )
                }
            }
        }
    }
}
