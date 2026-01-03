@file:OptIn(ExperimentalMaterial3Api::class)

package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.screens.components.FingerprintBox
import at.ustp.accessgate.screens.components.FlipPatternBox
import at.ustp.accessgate.screens.components.PinInputBox
import at.ustp.accessgate.screens.components.TapInputBox
import at.ustp.accessgate.ui.theme.authMethodAccent
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.ui.theme.authTypeIcon


@Composable
fun AuthGateScreen(
    entryId: Long,
    viewModel: AuthViewModel,
    onAuthed: () -> Unit,
    onBack: () -> Unit
) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)
    val authUi by viewModel.tapAuthUiState.collectAsState()

    var navigated by remember(entryId) { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        navigated = false
        viewModel.setAuthMessage("")
    }

    LaunchedEffect(authUi.lastResult, navigated) {
        if (!navigated && authUi.lastResult == true) {
            navigated = true
            onAuthed()
        }
    }

    val typeId = entry?.type ?: ""
    val accent = authMethodAccent(typeId)
    val typeIcon = authTypeIcon(typeId)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = accent
                        )
                        Spacer(Modifier.padding(start = 10.dp))
                        Text("Authenticate")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (entry == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Loading...")
            }
            return@Scaffold
        }

        val e = entry!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Unlock: ${e.name}", style = MaterialTheme.typography.titleLarge)

            if (e.hint.isNotBlank()) {
                Text("Hint: ${e.hint}", style = MaterialTheme.typography.bodyMedium)
            }


            when (e.type) {

                AuthType.TAP_JINGLE.id -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    TapInputBox { intervals ->
                        viewModel.authenticateTap(entryId, intervals)
                    }
                }

                AuthType.PIN.id -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    PinInputBox(
                        label = "Enter PIN",
                        onDone = { pin ->
                            viewModel.authenticatePin(entryId, pin)
                        }
                    )
                }

                AuthType.FINGERPRINT.id -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    FingerprintBox(
                        title = "Biometric scan",
                        onSuccess = {
                            viewModel.authenticateBiometricSuccess(entryId)
                        },
                        onError = { msg ->
                            viewModel.setAuthMessage(msg)
                        }
                    )
                }

                AuthType.FLIP_PATTERN.id -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    FlipPatternBox(
                        title = "Record flip pattern",
                        onRecorded = { payload ->
                            viewModel.authenticateFlip(entryId, payload)
                        }
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Unsupported type: ${e.type}")
                }
            }

            if (authUi.message.isNotBlank()) {
                Text(
                    text = authUi.message,
                    color = if (authUi.lastResult == false) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}