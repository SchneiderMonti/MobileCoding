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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.screens.components.FingerprintBox
import at.ustp.accessgate.screens.components.FlipPatternBox
import at.ustp.accessgate.screens.components.PinInputBox
import at.ustp.accessgate.screens.components.TapInputBox
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAuthScreen(
    entryId: Long,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)
    val authUi by viewModel.tapAuthUiState.collectAsState()

    // ✅ Clear any old "Saved ..." messages when opening a detail screen
    LaunchedEffect(entryId) {
        viewModel.clearAuthMessage()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Details") }) }
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
            Text("Name: ${e.name}", style = MaterialTheme.typography.titleMedium)
            Text("Type: ${e.type}", style = MaterialTheme.typography.bodyMedium)
            Text("Created At: ${formatTimestamp(e.createdAt)}", style = MaterialTheme.typography.bodyMedium)
            Text("Updated At: ${formatTimestamp(e.updatedAt)}", style = MaterialTheme.typography.bodyMedium)
            Text("Payload: ${e.payload}", style = MaterialTheme.typography.bodyMedium)

            Divider()

            Text("Authenticate", style = MaterialTheme.typography.titleMedium)

            when (e.type) {
                AuthType.TAP_JINGLE.id -> TapInputBox { intervals ->
                    viewModel.authenticateTap(entryId, intervals)
                }

                AuthType.PIN.id -> PinInputBox(
                    label = "Enter PIN",
                    onDone = { pin -> viewModel.authenticatePin(entryId, pin) }
                )

                AuthType.FINGERPRINT.id -> FingerprintBox(
                    title = "Biometric scan",
                    onSuccess = { viewModel.authenticateBiometricSuccess(entryId) },
                    onError = { msg -> viewModel.setAuthMessage(msg) }
                )

                AuthType.FLIP_PATTERN.id -> FlipPatternBox(
                    title = "Record flip pattern",
                    onRecorded = { payload -> viewModel.authenticateFlip(entryId, payload) }
                )

                else -> Text("No authentication UI for type '${e.type}' yet.")
            }

            if (authUi.message.isNotBlank()) {
                Text(
                    text = authUi.message,
                    color = if (authUi.lastResult == false) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    viewModel.clearAuthMessage()
                    onBack()
                }) { Text("Back") }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        viewModel.deleteEntry(entryId)
                        viewModel.clearAuthMessage()
                        onDeleted()
                    }
                ) { Text("Delete") }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    return formatter.format(Instant.ofEpochMilli(timestamp))
}