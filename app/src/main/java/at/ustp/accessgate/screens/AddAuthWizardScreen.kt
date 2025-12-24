@file:OptIn(ExperimentalMaterial3Api::class)

package at.ustp.accessgate.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import at.ustp.accessgate.ui.theme.authMethodAccent
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.userinterfaces.EnrollmentStep
import kotlin.math.abs

@Composable
fun AddAuthWizardScreen(
    viewModel: AuthViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.enrollmentUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startEnrollment()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Authentication") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
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
                        onClick = { viewModel.selectEnrollmentMethod(AuthType.TAP_JINGLE) }
                    )

                    MethodButton(
                        label = "PIN",
                        typeId = AuthType.PIN.id,
                        onClick = { viewModel.selectEnrollmentMethod(AuthType.PIN) }
                    )

                    MethodButton(
                        label = "Fingerprint",
                        typeId = AuthType.FINGERPRINT.id,
                        onClick = { viewModel.selectEnrollmentMethod(AuthType.FINGERPRINT) }
                    )

                    MethodButton(
                        label = "Flip Pattern",
                        typeId = AuthType.FLIP_PATTERN.id,
                        onClick = { viewModel.selectEnrollmentMethod(AuthType.FLIP_PATTERN) }
                    )
                }

                EnrollmentStep.DoAuth -> {
                    Text("Step 1: Do the authentication", style = MaterialTheme.typography.titleMedium)

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
                            onError = { /* optional: show a message in VM */ }
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

                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

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
                            onError = { /* optional: show a message in VM */ }
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
                    Text("Review", style = MaterialTheme.typography.titleMedium)
                    Text("Name: ${state.name}")
                    Text("Type: ${state.type?.displayName}")

                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            viewModel.saveEnrollment()
                            onDone()
                        }) { Text("Save") }
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
    // Uses the same accent mapping as the list screen.
    // Make sure you have authMethodAccent(...) available in scope.
    val accent = authMethodAccent(typeId)

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = accent)
    ) {
        Text(label)
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
    label: String,
    onDone: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { pin = "" }) { Text("Reset") }
            Button(
                onClick = {
                    onDone(pin)
                    pin = ""
                },
                enabled = pin.isNotBlank()
            ) { Text("Done") }
        }
    }
}

@Composable
fun FingerprintBox(
    title: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title)

        Button(onClick = {
            if (activity == null) {
                onError("No FragmentActivity found.")
                return@Button
            }

            val authenticators =
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL

            val biometricManager = BiometricManager.from(activity)
            val canAuth = biometricManager.canAuthenticate(authenticators)

            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                val msg = when (canAuth) {
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                        "No fingerprint enrolled on this device."
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                        "This device has no biometric hardware."
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                        "Biometric hardware currently unavailable."
                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                        "Security update required."
                    else ->
                        "Biometric unavailable (code $canAuth)."
                }
                onError(msg)
                return@Button
            }

            val executor = ContextCompat.getMainExecutor(activity)

            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        val msg = when (errorCode) {
                            BiometricPrompt.ERROR_LOCKOUT ->
                                "Too many attempts. Try again later."
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                "Fingerprint locked. Use device PIN/password."
                            else ->
                                errString.toString()
                        }
                        onError(msg)
                    }

                    override fun onAuthenticationFailed() {
                        onError("Not recognized. Try again.")
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate")
                .setSubtitle(title)
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(promptInfo)

        }) {
            Text("Scan Fingerprint")
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is android.content.ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}

private enum class FlipDir { UP, DOWN, LEFT, RIGHT, FACE_UP, FACE_DOWN }

@Composable
fun FlipPatternBox(
    title: String,
    onRecorded: (String) -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accel = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var recording by remember { mutableStateOf(false) }
    val flips = remember { mutableStateListOf<FlipDir>() }
    var status by remember { mutableStateOf("Press Start, then flip the phone.") }

    var lastDir by remember { mutableStateOf<FlipDir?>(null) }
    var lastDirTime by remember { mutableStateOf(0L) }

    fun classify(ax: Float, ay: Float, az: Float): FlipDir? {
        val t = 7.0f
        return when {
            abs(ax) > abs(ay) && abs(ax) > abs(az) && abs(ax) > t ->
                if (ax > 0) FlipDir.RIGHT else FlipDir.LEFT

            abs(ay) > abs(ax) && abs(ay) > abs(az) && abs(ay) > t ->
                if (ay > 0) FlipDir.UP else FlipDir.DOWN

            abs(az) > abs(ax) && abs(az) > abs(ay) && abs(az) > t ->
                if (az > 0) FlipDir.FACE_UP else FlipDir.FACE_DOWN

            else -> null
        }
    }

    DisposableEffect(recording) {
        if (!recording || accel == null) return@DisposableEffect onDispose { }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                val dir = classify(ax, ay, az) ?: return
                val now = System.currentTimeMillis()

                if (dir == lastDir && (now - lastDirTime) < 350) return
                if ((now - lastDirTime) < 250) return

                if (dir != lastDir) {
                    flips.add(dir)
                    lastDir = dir
                    lastDirTime = now
                    status = "Recorded: ${flips.joinToString(" → ")}"
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        if (accel == null) {
            Text("No accelerometer found on this device.", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        Text(status)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    flips.clear()
                    lastDir = null
                    lastDirTime = 0L
                    recording = true
                    status = "Recording... flip the phone (e.g., UP → LEFT → DOWN)."
                },
                enabled = !recording
            ) { Text("Start") }

            Button(
                onClick = {
                    recording = false
                    val payload = flips.joinToString(",") { it.name }
                    status = "Stopped. Payload: $payload"
                    onRecorded(payload)
                },
                enabled = recording && flips.isNotEmpty()
            ) { Text("Stop") }

            OutlinedButton(
                onClick = {
                    flips.clear()
                    lastDir = null
                    lastDirTime = 0L
                    status = "Cleared."
                }
            ) { Text("Reset") }
        }
    }
}