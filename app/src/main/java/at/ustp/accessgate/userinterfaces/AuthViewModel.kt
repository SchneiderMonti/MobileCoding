package at.ustp.accessgate.userinterfaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ustp.accessgate.data.AuthRepository
import at.ustp.accessgate.db.AuthEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class TapAuthUiState(
    val lastResult: Boolean? = null,
    val message: String = ""
)

enum class AuthType(val id: String, val displayName: String) {
    TAP_JINGLE("tap_jingle", "Tap Jingle"),
    PIN("pin", "Pin Code"),
    FINGERPRINT("fingerprint", "Fingerprint"),
    FLIP_PATTERN("flip_pattern", "Flip Pattern")
}

enum class EnrollmentStep {
    ChooseMethod,
    DoAuth,
    RepeatAuth,
    Name,
    ReviewAndSave
}

data class EnrollmentUiState(
    val step: EnrollmentStep = EnrollmentStep.ChooseMethod,
    val type: AuthType? = null,

    val firstIntervals: List<Long> = emptyList(),
    val repeatIntervals: List<Long> = emptyList(),

    val firstPin: String = "",
    val repeatPin: String = "",

    val firstFingerPrint: Boolean = false,
    val repeatFingerPrint: Boolean = false,

    val firstFlipPayload: String = "",
    val repeatFlipPayload: String = "",

    val name: String = "",
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    // List screen uses this
    val entries: Flow<List<AuthEntryEntity>> = repository.entries

    private val _tapAuthUiState = MutableStateFlow(TapAuthUiState())
    val tapAuthUiState: StateFlow<TapAuthUiState> = _tapAuthUiState.asStateFlow()

    // Wizard enrollment state
    private val _enrollmentUiState = MutableStateFlow(EnrollmentUiState())
    val enrollmentUiState: StateFlow<EnrollmentUiState> = _enrollmentUiState.asStateFlow()

    fun startEnrollment() {
        _enrollmentUiState.value = EnrollmentUiState()
    }

    fun selectEnrollmentMethod(type: AuthType) {
        _enrollmentUiState.value = EnrollmentUiState(
            type = type,
            step = EnrollmentStep.DoAuth
        )
    }


    fun setEnrollmentFirstPin(pin: String) {
        val cleaned = pin.trim()
        if (cleaned.length < 4) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "PIN must be at least 4 digits.",
                step = EnrollmentStep.DoAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            firstPin = cleaned,
            error = null,
            step = EnrollmentStep.RepeatAuth
        )
    }

    fun setEnrollmentRepeatPin(pin: String) {
        val cleaned = pin.trim()
        val first = _enrollmentUiState.value.firstPin

        if (cleaned.length < 4) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "PIN must be at least 4 digits.",
                step = EnrollmentStep.RepeatAuth
            )
            return
        }

        if (first.isBlank()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "First PIN missing. Please start again.",
                step = EnrollmentStep.DoAuth
            )
            return
        }

        if (cleaned != first) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                repeatPin = "",
                error = "PINs do not match. Please repeat again.",
                step = EnrollmentStep.RepeatAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            repeatPin = cleaned,
            error = null,
            step = EnrollmentStep.Name
        )
    }

    fun setEnrollmentFirstIntervals(intervals: List<Long>) {
        if (intervals.isEmpty()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "Not enough taps (need at least 2 taps).",
                step = EnrollmentStep.DoAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            firstIntervals = intervals,
            error = null,
            step = EnrollmentStep.RepeatAuth
        )
    }

    fun setEnrollmentRepeatIntervals(intervals: List<Long>) {
        val first = _enrollmentUiState.value.firstIntervals

        if (intervals.isEmpty()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "Not enough taps (need at least 2 taps).",
                step = EnrollmentStep.RepeatAuth
            )
            return
        }

        if (first.isEmpty()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "First recording missing. Please start again.",
                step = EnrollmentStep.ChooseMethod
            )
            return
        }

        // Must match the first recording reasonably well
        val matches = tapMatches(first, intervals)
        if (!matches) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                repeatIntervals = emptyList(),
                error = "Doesn't match the first attempt. Please repeat again.",
                step = EnrollmentStep.RepeatAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            repeatIntervals = intervals,
            error = null,
            step = EnrollmentStep.Name
        )
    }

    fun setEnrollmentFirstFlip(payload: String) {
        if (payload.isBlank()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "No movement recorded.",
                step = EnrollmentStep.DoAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            firstFlipPayload = payload,
            error = null,
            step = EnrollmentStep.RepeatAuth
        )
    }

    fun setEnrollmentRepeatFlip(payload: String) {
        val first = _enrollmentUiState.value.firstFlipPayload
        if (first.isBlank()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "First recording missing. Please start again.",
                step = EnrollmentStep.ChooseMethod
            )
            return
        }

        if (!flipMatches(first, payload)) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                repeatFlipPayload = "",
                error = "Doesn't match the first attempt. Please repeat again.",
                step = EnrollmentStep.RepeatAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            repeatFlipPayload = payload,
            error = null,
            step = EnrollmentStep.Name
        )
    }

    private fun flipMatches(firstPayload: String, repeatPayload: String): Boolean {
        val a = firstPayload.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val b = repeatPayload.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return a == b
    }

    fun setEnrollmentFirstFingerprint() {
        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            firstFingerPrint = true,
            error = null,
            step = EnrollmentStep.RepeatAuth
        )
    }


    fun setEnrollmentRepeatFingerPrint() {
        if (!_enrollmentUiState.value.firstFingerPrint) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                error = "First Fingerprint step missing! Please start again!",
                step = EnrollmentStep.ChooseMethod
            )
            return
        }
        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            repeatFingerPrint = true,
            error = null,
            step = EnrollmentStep.Name
        )
    }

    fun setEnrollmentName(name: String) {
        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            name = name.trim(),
            error = null,
            step = EnrollmentStep.ReviewAndSave
        )
    }

    fun saveEnrollment() {
        val state = _enrollmentUiState.value
        val type = state.type ?: return

        viewModelScope.launch {

            val (defaultName, payload) = when (type) {

                AuthType.TAP_JINGLE -> {
                    val intervals = state.firstIntervals
                    if (intervals.isEmpty()) {
                        _enrollmentUiState.value = state.copy(
                            error = "Nothing to save. Please record your authentication.",
                            step = EnrollmentStep.DoAuth
                        )
                        return@launch
                    }
                    "Tap Jingle" to intervals.joinToString(",")
                }

                AuthType.PIN -> {
                    val pin = state.firstPin.trim()
                    val repeat = state.repeatPin.trim()

                    if (pin.isBlank() || repeat.isBlank()) {
                        _enrollmentUiState.value = state.copy(
                            error = "PIN missing. Please enter and repeat your PIN.",
                            step = EnrollmentStep.DoAuth
                        )
                        return@launch
                    }

                    if (pin != repeat) {
                        _enrollmentUiState.value = state.copy(
                            error = "PINs do not match. Please repeat again.",
                            step = EnrollmentStep.RepeatAuth
                        )
                        return@launch
                    }

                    "PIN" to pin
                }

                AuthType.FINGERPRINT -> {
                    // You can’t store “which finger” — only that this entry uses biometrics.
                    if (!state.firstFingerPrint || !state.repeatFingerPrint) {
                        _enrollmentUiState.value = state.copy(
                            error = "Fingerprint enrollment not completed (do it twice).",
                            step = EnrollmentStep.DoAuth
                        )
                        return@launch
                    }

                    // minimal payload (you can switch to JSON later)
                    "Fingerprint" to "enabled"
                }

                AuthType.FLIP_PATTERN -> {
                    val p = state.firstFlipPayload
                    if (p.isBlank()) {
                        _enrollmentUiState.value = state.copy(
                            error = "Nothing to save. Please record the movement pattern.",
                            step = EnrollmentStep.DoAuth
                        )
                        return@launch
                    }
                    "Flip Pattern" to p
                }
            }

            val name = state.name.ifBlank { defaultName }

            repository.createEntry(
                name = name,
                type = type.id,
                payload = payload
            )

            _tapAuthUiState.value = TapAuthUiState(message = "Saved '$name' ✅")
            startEnrollment()
        }
    }

    // --- Existing helpers (kept) ---


    fun observeEntryById(id: Long): Flow<AuthEntryEntity?> {
        return repository.observeEntryById(id)
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntryById(entryId)
            _tapAuthUiState.value = TapAuthUiState(message = "Deleted ✅")
        }
    }

    fun clearAuthMessage() {
        _tapAuthUiState.value = TapAuthUiState(lastResult = null, message = "")
    }




    // --- Core matching logic (kept) ---
    private fun tapMatches(enrolled: List<Long>, attempt: List<Long>): Boolean {
        if (enrolled.size != attempt.size) return false

        val enrolledAvg = enrolled.average()
        val attemptAvg = attempt.average()
        if (enrolledAvg == 0.0) return false

        val scale = attemptAvg / enrolledAvg

        val minTolMs = 40.0
        val maxTolMs = 140.0
        val relTol = 0.18

        return enrolled.indices.all { i ->
            val expected = enrolled[i] * scale
            val tol = (expected * relTol).coerceIn(minTolMs, maxTolMs)
            abs(attempt[i] - expected) <= tol
        }
    }

    // ============================================================================================
    // AUTHENTICATE AGAINST ENTRY!!!!
    // ===========================================================================================

    private val _authResult = MutableStateFlow<TapAuthUiState>(TapAuthUiState())
    val authResult: StateFlow<TapAuthUiState> = _authResult.asStateFlow()

    fun authenticateAgainstEntry(entry: AuthEntryEntity, attemptPayload: String) {
        viewModelScope.launch {
            when (entry.type) {

                AuthType.TAP_JINGLE.id -> {
                    val enrolled = entry.payload.split(",").mapNotNull { it.toLongOrNull() }
                    val attempt = attemptPayload.split(",").mapNotNull { it.toLongOrNull() }

                    if (enrolled.isEmpty() || attempt.isEmpty()) {
                        _authResult.value = TapAuthUiState(false, "Missing tap data")
                        return@launch
                    }

                    val ok = tapMatches(enrolled, attempt)
                    _authResult.value = TapAuthUiState(
                        lastResult = ok,
                        message = if (ok) "✅ Tap Jingle match" else "❌ Tap Jingle mismatch"
                    )
                }

                AuthType.PIN.id -> {
                    val ok = entry.payload == attemptPayload.trim()
                    _authResult.value = TapAuthUiState(
                        lastResult = ok,
                        message = if (ok) "✅ PIN correct" else "❌ Wrong PIN"
                    )
                }

                AuthType.FINGERPRINT.id -> {
                    // For now: UI triggers BiometricPrompt and calls viewModel.setBiometricResult(...)
                    _authResult.value = TapAuthUiState(null, "Use biometric prompt in UI")
                }

                else -> {
                    _authResult.value = TapAuthUiState(false, "Unknown auth type: ${entry.type}")
                }
            }
        }
    }

    fun setBiometricResult(success: Boolean, msg: String) {
        _authResult.value = TapAuthUiState(
            lastResult = success,
            message = msg
        )
    }

    // --- Detail screen actions ---

    fun deleteEntryById(id: Long) {
        viewModelScope.launch {
            repository.deleteEntryById(id)   // <-- make sure this exists in repo/dao
        }
    }

    /** Optional helper to just show errors from UI callbacks */
    fun setAuthMessage(msg: String) {
        _tapAuthUiState.value = _tapAuthUiState.value.copy(message = msg)
    }

    fun authenticateTap(entryId: Long, attemptIntervals: List<Long>) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            if (entry == null) {
                _tapAuthUiState.value = TapAuthUiState(false, "Entry not found")
                return@launch
            }
            if (entry.type != AuthType.TAP_JINGLE.id) {
                _tapAuthUiState.value = TapAuthUiState(false, "Wrong method for this entry")
                return@launch
            }

            val enrolled = entry.payload.split(",").mapNotNull { it.toLongOrNull() }
            val ok = tapMatches(enrolled, attemptIntervals)

            _tapAuthUiState.value = TapAuthUiState(
                lastResult = ok,
                message = if (ok) "✅ Authentication success" else "❌ Authentication failed"
            )
        }
    }

    fun authenticatePin(entryId: Long, pin: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            if (entry == null) {
                _tapAuthUiState.value = TapAuthUiState(false, "Entry not found")
                return@launch
            }
            if (entry.type != AuthType.PIN.id) {
                _tapAuthUiState.value = TapAuthUiState(false, "Wrong method for this entry")
                return@launch
            }

            val ok = entry.payload == pin.trim()

            _tapAuthUiState.value = TapAuthUiState(
                lastResult = ok,
                message = if (ok) "✅ Authentication success" else "❌ Wrong PIN"
            )
        }
    }

    fun authenticateFlip(entryId: Long, attemptPayload: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            if (entry == null) {
                _tapAuthUiState.value = TapAuthUiState(false, "Entry not found")
                return@launch
            }
            if (entry.type != AuthType.FLIP_PATTERN.id) {
                _tapAuthUiState.value = TapAuthUiState(false, "Wrong method for this entry")
                return@launch
            }

            val ok = entry.payload == attemptPayload

            _tapAuthUiState.value = TapAuthUiState(
                lastResult = ok,
                message = if (ok) "✅ Authentication success" else "❌ Flip pattern mismatch"
            )
        }
    }

    /**
     * IMPORTANT LIMITATION:
     * Android biometric auth only tells you “user authenticated”, not WHICH finger/face.
     * So this can only be “success if the system says success”.
     */
    fun authenticateBiometricSuccess(entryId: Long) {
        _tapAuthUiState.value = TapAuthUiState(
            lastResult = true,
            message = "✅ Biometric authentication success"
        )
    }



}



