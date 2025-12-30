package at.ustp.accessgate.screens.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.math.abs

@Composable
fun TapInputBox(
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
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray)
                .border(
                    width = 2.dp,
                    color = Color(0xFF7C4DFF),
                    shape = RoundedCornerShape(16.dp)
                )
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
fun PinInputBox(
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

            // ✅ HERE: allow fingerprint + device PIN/pattern/password fallback
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
                        // Soft failure – keep prompt open
                        onError("Not recognized. Try again.")
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate")
                .setSubtitle(title)
                // ✅ MUST match the same authenticators
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(promptInfo)

        }) {
            Text("Scan Fingerprint")
        }
    }
}


/** Walk up Context wrappers until we find a FragmentActivity */
private tailrec fun android.content.Context.findFragmentActivity(): FragmentActivity? {
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

    // simple debounce so we don't record the same orientation 100 times
    var lastDir by remember { mutableStateOf<FlipDir?>(null) }
    var lastDirTime by remember { mutableStateOf(0L) }

    fun classify(ax: Float, ay: Float, az: Float): FlipDir? {
        // We detect the dominant axis; tweak thresholds if needed
        val t = 7.0f // ~0.7g
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

                // debounce + don't repeat same direction back-to-back too quickly
                if (dir == lastDir && (now - lastDirTime) < 350) return
                if ((now - lastDirTime) < 250) return

                // record only changes (prevents noise)
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

        onDispose {
            sensorManager.unregisterListener(listener)
        }
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

