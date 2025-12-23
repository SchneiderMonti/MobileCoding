package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.screens.components.*
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun DetailAuthScreen(
    entryId: Long,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)
    val authUi by viewModel.tapAuthUiState.collectAsState()

    if (entry == null) {
        Text("Loading...")
        return
    }

    val e = entry!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Details", style = MaterialTheme.typography.headlineSmall)

        Text("Name: ${e.name}")
        Text("Type: ${e.type}")
        Text("Payload: ${e.payload}")

        Divider()

        Text("Authenticate", style = MaterialTheme.typography.titleMedium)

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

            else -> {
                Text("No authentication UI for type '${e.type}' yet.")
            }
        }

        if (authUi.message.isNotBlank()) {
            Text(
                authUi.message,
                color = if (authUi.lastResult == false) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                onClick = {
                    viewModel.deleteEntry(entryId)
                    onDeleted() // go back to list
                }
            ) { Text("Delete") }
        }
    }
}
