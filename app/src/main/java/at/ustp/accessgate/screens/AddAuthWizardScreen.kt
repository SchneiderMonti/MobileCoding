import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.userinterfaces.EnrollmentStep

@Composable
fun AddAuthWizardScreen(
    viewModel: AuthViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.enrollmentUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startEnrollment()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Add Authentication", style = MaterialTheme.typography.headlineSmall)

        when (state.step) {

            EnrollmentStep.ChooseMethod -> {
                Button(onClick = { viewModel.selectEnrollmentMethod(AuthType.TAP_JINGLE) }) {
                    Text("Tap Jingle")
                }

                Button(onClick = { viewModel.selectEnrollmentMethod(AuthType.PIN) }) {
                    Text("PIN")
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Fingerprint", style = MaterialTheme.typography.titleMedium)
                        Text("Coming soon", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Face", style = MaterialTheme.typography.titleMedium)
                        Text("Coming soon", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            EnrollmentStep.DoAuth -> {
                Text("Step 1: Do the authentication")

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                when (state.type) {
                    AuthType.TAP_JINGLE -> TapInputBox(
                        onFinished = { intervals -> viewModel.setEnrollmentFirstIntervals(intervals) }
                    )

                    AuthType.PIN -> PinInputBox(
                        buttonText = "Done",
                        onFinished = { pin -> viewModel.setEnrollmentFirstPin(pin) }
                    )

                    null -> {}
                }
            }

            EnrollmentStep.RepeatAuth -> {
                Text("Step 2: Repeat the authentication")

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                when (state.type) {
                    AuthType.TAP_JINGLE -> TapInputBox(
                        onFinished = { intervals -> viewModel.setEnrollmentRepeatIntervals(intervals) }
                    )

                    AuthType.PIN -> PinInputBox(
                        buttonText = "Done",
                        onFinished = { pin -> viewModel.setEnrollmentRepeatPin(pin) }
                    )

                    null -> {}
                }
            }

            EnrollmentStep.Name -> {
                Text("Step 3: Name it")

                var name by remember { mutableStateOf(state.name) }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Authentication name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.setEnrollmentName(name) },
                        enabled = name.isNotBlank()
                    ) { Text("Next") }
                }
            }

            EnrollmentStep.ReviewAndSave -> {
                Text("Review")
                Text("Name: ${state.name}")
                Text("Type: ${state.type?.displayName}")

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        viewModel.saveEnrollment()
                        onDone()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun TapInputBox(
    onFinished: (List<Long>) -> Unit
) {
    val tapTimes = remember { mutableStateListOf<Long>() }

    fun intervalsFromTapTimes(times: List<Long>): List<Long> =
        times.zipWithNext { a, b -> b - a }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.LightGray)
                .clickable { tapTimes.add(System.currentTimeMillis()) },
            contentAlignment = Alignment.Center
        ) {
            Text("Tap here\nTaps recorded: ${tapTimes.size}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { tapTimes.clear() }) { Text("Reset") }

            Button(
                onClick = {
                    val intervals = intervalsFromTapTimes(tapTimes)
                    onFinished(intervals)
                    tapTimes.clear()
                },
                enabled = tapTimes.size >= 2
            ) { Text("Done") }
        }
    }
}

@Composable
private fun PinInputBox(
    buttonText: String,
    onFinished: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Enter PIN") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { pin = "" }) { Text("Reset") }

            Button(
                onClick = {
                    onFinished(pin)
                    pin = ""
                },
                enabled = pin.trim().length >= 4
            ) { Text(buttonText) }
        }
    }
}
