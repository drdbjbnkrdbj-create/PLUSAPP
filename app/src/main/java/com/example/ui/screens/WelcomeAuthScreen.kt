package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.AppLanguage
import com.example.ui.components.PulseLogoMark
import com.example.ui.i18n.rememberStrings
import com.example.ui.theme.LocalPulseColors

@Composable
fun WelcomeAuthScreen(
    currentLanguage: AppLanguage,
    onToggleLanguage: (AppLanguage) -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onContinueOffline: () -> Unit
) {
    val colors = LocalPulseColors.current
    val strings = rememberStrings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_misty_mountains),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.55f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD9101722),
                            Color(0x99101722),
                            Color(0xF5101722)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.cardBackground.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    TextButton(
                        onClick = {
                            onToggleLanguage(
                                if (currentLanguage == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
                            )
                        },
                        modifier = Modifier.testTag("welcome_language_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.AR) "English" else "العربية",
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 440.dp)
            ) {
                PulseLogoMark(
                    modifier = Modifier.size(width = 110.dp, height = 64.dp),
                    color = colors.primaryAccent,
                    strokeWidth = 5.dp
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "PULSE",
                    style = MaterialTheme.typography.displayLarge.copy(
                        letterSpacing = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFF4F7FA)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFAAB5C5),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (authError != null) {
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.failure,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        authError = null
                        isSigningIn = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                onGoogleSignInSuccess()
                            },
                            onAuthError = { msg ->
                                isSigningIn = false
                                authError = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isSigningIn = false }
                        )
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = Color(0xFF0A1420)
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF0A1420),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = strings.signingIn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onContinueOffline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_offline_button"),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF4F7FA)
                    )
                ) {
                    Text(
                        text = strings.getStartedOffline,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
