@file:OptIn(ExperimentalMaterial3Api::class)

package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.screens.components.FingerprintBox
import at.ustp.accessgate.screens.components.FlipPatternBox
import at.ustp.accessgate.screens.components.PinInputBox
import at.ustp.accessgate.screens.components.TapInputBox
import at.ustp.accessgate.ui.theme.authMethodAccent
import at.ustp.accessgate.ui.theme.authTypeIcon
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.userinterfaces.EnrollmentMode
import at.ustp.accessgate.userinterfaces.EnrollmentStep

@Composable
fun AddAuthWizardScreen(
    viewModel: AuthViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.enrollmentUiState.collectAsState()
    var showMethodInfo by remember { mutableStateOf(false) }
    var pendingMethod by remember { mutableStateOf<AuthType?>(null) }


    LaunchedEffect(Unit) {
        val looksFreshCreate =
            state.mode == EnrollmentMode.CREATE &&
                    state.step == EnrollmentStep.ChooseMethod &&
                    state.type == null &&
                    state.name.isBlank() &&
                    state.hint.isBlank()

        if (looksFreshCreate) {
            viewModel.startEnrollment()
        }
    }

    if (showMethodInfo && pendingMethod != null) {
        val method = pendingMethod!!

        AlertDialog(
            onDismissRequest = {
                showMethodInfo = false
                pendingMethod = null
            },
            title = { Text(method.displayName) },
            text = {
                Text(
                    when (method) {
                        AuthType.TAP_JINGLE ->
                            "Tap a rhythm twice. We store the timing intervals and later compare your attempts."
                        AuthType.PIN ->
                            "Enter a PIN twice. We store the PIN (proof of concept). Later you authenticate by entering it again."
                        AuthType.FINGERPRINT ->
                            "Uses Android’s biometric prompt. Android does not reveal which finger—only success/fail."
                        AuthType.FLIP_PATTERN ->
                            "Flip the phone in a short pattern twice. We store the direction sequence."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectEnrollmentMethod(method)
                        showMethodInfo = false
                        pendingMethod = null
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showMethodInfo = false
                        pendingMethod = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.mode == EnrollmentMode.EDIT) "Update Authentication"
                        else "Add Authentication"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            showMethodInfo = false
                            pendingMethod = null

                            if (state.step == EnrollmentStep.ChooseMethod && state.mode == EnrollmentMode.CREATE) {
                                viewModel.cancelEnrollment()
                                onCancel()
                            } else {
                                viewModel.backEnrollmentStep()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            showMethodInfo = false
                            pendingMethod = null
                            viewModel.cancelEnrollment()
                            onCancel()
                        }
                    ) { Text("Cancel") }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            when (state.step) {

                EnrollmentStep.ChooseMethod -> {
                    Text("Choose method", style = MaterialTheme.typography.titleMedium)

                    MethodButton(
                        label = "Tap Jingle",
                        typeId = AuthType.TAP_JINGLE.id,
                        onClick = {
                            pendingMethod = AuthType.TAP_JINGLE
                            showMethodInfo = true
                        }
                    )

                    MethodButton(
                        label = "PIN",
                        typeId = AuthType.PIN.id,
                        onClick = {
                            pendingMethod = AuthType.PIN
                            showMethodInfo = true
                        }
                    )

                    MethodButton(
                        label = "Fingerprint",
                        typeId = AuthType.FINGERPRINT.id,
                        onClick = {
                            pendingMethod = AuthType.FINGERPRINT
                            showMethodInfo = true
                        }
                    )

                    MethodButton(
                        label = "Flip Pattern",
                        typeId = AuthType.FLIP_PATTERN.id,
                        onClick = {
                            pendingMethod = AuthType.FLIP_PATTERN
                            showMethodInfo = true
                        }
                    )
                }

                EnrollmentStep.DoAuth -> {
                    Text(
                        if (state.mode == EnrollmentMode.EDIT) "Step 1: Re-enter authentication"
                        else "Step 1: Do the authentication",
                        style = MaterialTheme.typography.titleMedium
                    )

                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    when (state.type) {
                        AuthType.TAP_JINGLE -> TapInputBox { intervals ->
                            viewModel.setEnrollmentFirstIntervals(intervals)
                        }

                        AuthType.PIN -> PinInputBox(
                            label = "Enter PIN",
                            onDone = { pin -> viewModel.setEnrollmentFirstPin(pin) }
                        )

                        AuthType.FINGERPRINT -> FingerprintBox(
                            title = "Use fingerprint (1/2)",
                            onSuccess = { viewModel.setEnrollmentFirstFingerprint() },
                            onError = { msg -> viewModel.setAuthMessage(msg) }
                        )

                        AuthType.FLIP_PATTERN -> FlipPatternBox(
                            title = "Do movement pattern (1/2)",
                            onRecorded = { payload -> viewModel.setEnrollmentFirstFlip(payload) }
                        )

                        null -> Unit
                    }
                }

                EnrollmentStep.RepeatAuth -> {
                    Text("Step 2: Repeat the authentication", style = MaterialTheme.typography.titleMedium)

                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    when (state.type) {
                        AuthType.TAP_JINGLE -> TapInputBox { intervals ->
                            viewModel.setEnrollmentRepeatIntervals(intervals)
                        }

                        AuthType.PIN -> PinInputBox(
                            label = "Repeat PIN",
                            onDone = { pin -> viewModel.setEnrollmentRepeatPin(pin) }
                        )

                        AuthType.FINGERPRINT -> FingerprintBox(
                            title = "Use fingerprint (2/2)",
                            onSuccess = { viewModel.setEnrollmentRepeatFingerPrint() },
                            onError = { msg -> viewModel.setAuthMessage(msg) }
                        )

                        AuthType.FLIP_PATTERN -> FlipPatternBox(
                            title = "Repeat movement pattern (2/2)",
                            onRecorded = { payload -> viewModel.setEnrollmentRepeatFlip(payload) }
                        )

                        null -> Unit
                    }
                }

                EnrollmentStep.Name -> {
                    Text("Step 3: Name it", style = MaterialTheme.typography.titleMedium)

                    var name by remember(state.name) { mutableStateOf(state.name) }
                    var hint by remember(state.hint) { mutableStateOf(state.hint) }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Authentication name") },
                        modifier = Modifier.fillMaxSize()
                    )

                    OutlinedTextField(
                        value = hint,
                        onValueChange = { hint = it },
                        label = { Text("Hint (optional)") },
                        supportingText = {
                            Text("Example: “birthday”, “3 knocks then pause”, “flip up-left-down”")
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                viewModel.setEnrollmentHint(hint)
                                viewModel.setEnrollmentName(name)
                            },
                            enabled = name.isNotBlank()
                        ) { Text("Next") }
                    }
                }

                EnrollmentStep.ReviewAndSave -> {
                    Text("Review", style = MaterialTheme.typography.titleMedium)
                    Text("Name: ${state.name}")
                    if (state.hint.isNotBlank()) Text("Hint: ${state.hint}")
                    Text("Type: ${state.type?.displayName}")

                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                viewModel.saveEnrollment()
                                onDone()
                            }
                        ) {
                            Text(if (state.mode == EnrollmentMode.EDIT) "Update" else "Save")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MethodButton(
    label: String,
    typeId: String,
    onClick: () -> Unit
) {
    val accent = authMethodAccent(typeId)
    val icon = authTypeIcon(typeId)

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}