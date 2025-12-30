@file:OptIn(ExperimentalMaterial3Api::class)

package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun AuthGateScreen(
    entryId: Long,
    viewModel: AuthViewModel,
    onAuthed: () -> Unit,
    onBack: () -> Unit
) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)
    val authUi by viewModel.tapAuthUiState.collectAsState()

    // prevent double-navigation on recomposition
    var navigated by remember(entryId) { mutableStateOf(false) }

    // clear old messages when entering this gate
    LaunchedEffect(entryId) {
        navigated = false
        viewModel.setAuthMessage("")
    }

    // ✅ auto-navigate when success happens
    LaunchedEffect(authUi.lastResult, navigated) {
        if (!navigated && authUi.lastResult == true) {
            navigated = true
            onAuthed()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Authenticate") }) }
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
            Text("Method: ${e.type}", style = MaterialTheme.typography.bodyMedium)

            when (e.type) {
                AuthType.TAP_JINGLE.id -> {
                    TapInputBox { intervals ->
                        viewModel.authenticateTap(entryId, intervals)
                    }
                }

                AuthType.PIN.id -> {
                    PinInputBox(
                        label = "Enter PIN",
                        onDone = { pin -> viewModel.authenticatePin(entryId, pin) }
                    )
                }

                AuthType.FINGERPRINT.id -> {
                    FingerprintBox(
                        title = "Biometric scan",
                        onSuccess = { viewModel.authenticateBiometricSuccess(entryId) },
                        onError = { msg -> viewModel.setAuthMessage(msg) }
                    )
                }

                AuthType.FLIP_PATTERN.id -> {
                    FlipPatternBox(
                        title = "Record flip pattern",
                        onRecorded = { payload -> viewModel.authenticateFlip(entryId, payload) }
                    )
                }

                else -> Text("Unsupported type: ${e.type}")
            }

            if (authUi.message.isNotBlank()) {
                Text(
                    text = authUi.message,
                    color = if (authUi.lastResult == false) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}