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

enum class EnrollmentMode {
    CREATE,
    EDIT
}

data class EnrollmentUiState(
    val mode: EnrollmentMode = EnrollmentMode.CREATE,
    val editingEntryId: Long? = null,

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
    val hint: String = "",

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

    // ---------------------------------------------------------------------------------------------
    // Enrollment flow
    // ---------------------------------------------------------------------------------------------

    fun startEnrollment() {
        _enrollmentUiState.value = EnrollmentUiState(mode = EnrollmentMode.CREATE)
    }

    fun startEditEnrollment(entryId: Long) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: run {
                _tapAuthUiState.value = TapAuthUiState(false, "Entry not found")
                return@launch
            }

            val type = AuthType.values().firstOrNull { it.id == entry.type } ?: run {
                _tapAuthUiState.value = TapAuthUiState(false, "Unknown auth type: ${entry.type}")
                return@launch
            }

            // We don't prefill secret/pattern values into input UI, but we do prefill name + hint
            _enrollmentUiState.value = EnrollmentUiState(
                mode = EnrollmentMode.EDIT,
                editingEntryId = entryId,
                type = type,
                step = EnrollmentStep.DoAuth, // editing: jump directly to re-record / re-enter
                name = entry.name,
                hint = entry.hint ?: "",
                error = null
            )
        }
    }

    fun cancelEnrollment() {
        // bring wizard back to safe defaults
        startEnrollment()
    }

    fun backEnrollmentStep() {
        val s = _enrollmentUiState.value

        val prev = when (s.step) {
            EnrollmentStep.ChooseMethod -> EnrollmentStep.ChooseMethod
            EnrollmentStep.DoAuth -> {
                if (s.mode == EnrollmentMode.EDIT) EnrollmentStep.DoAuth else EnrollmentStep.ChooseMethod
            }
            EnrollmentStep.RepeatAuth -> EnrollmentStep.DoAuth
            EnrollmentStep.Name -> EnrollmentStep.RepeatAuth
            EnrollmentStep.ReviewAndSave -> EnrollmentStep.Name
        }

        _enrollmentUiState.value = s.copy(step = prev, error = null)
    }

    fun selectEnrollmentMethod(type: AuthType) {
        _enrollmentUiState.value = EnrollmentUiState(
            mode = EnrollmentMode.CREATE,
            type = type,
            step = EnrollmentStep.DoAuth
        )
    }

    fun setEnrollmentHint(hint: String) {
        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            hint = hint.trim(),
            error = null
        )
    }

    fun setEnrollmentName(name: String) {
        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            name = name.trim(),
            error = null,
            step = EnrollmentStep.ReviewAndSave
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
                step = EnrollmentStep.DoAuth
            )
            return
        }

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
                step = EnrollmentStep.DoAuth
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
                step = EnrollmentStep.DoAuth
            )
            return
        }

        _enrollmentUiState.value = _enrollmentUiState.value.copy(
            repeatFingerPrint = true,
            error = null,
            step = EnrollmentStep.Name
        )
    }

    fun saveEnrollment() {
        val state = _enrollmentUiState.value
        val type = state.type ?: return

        viewModelScope.launch {

            val (defaultName, payload) = when (type) {

                AuthType.TAP_JINGLE -> {
                    val intervals = state.firstIntervals
                    if (intervals.isEmpty() || state.repeatIntervals.isEmpty()) {
                        _enrollmentUiState.value = state.copy(
                            error = "Nothing to save. Please record and repeat your authentication.",
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
                    if (!state.firstFingerPrint || !state.repeatFingerPrint) {
                        _enrollmentUiState.value = state.copy(
                            error = "Fingerprint enrollment not completed (do it twice).",
                            step = EnrollmentStep.DoAuth
                        )
                        return@launch
                    }

                    "Fingerprint" to "enabled"
                }

                AuthType.FLIP_PATTERN -> {
                    val p = state.firstFlipPayload
                    if (p.isBlank() || state.repeatFlipPayload.isBlank()) {
                        _enrollmentUiState.value = state.copy(
                            error = "Nothing to save. Please record and repeat the movement pattern.",
                            step = EnrollmentStep.DoAuth
                        )
                        return@launch
                    }
                    "Flip Pattern" to p
                }
            }

            val name = state.name.ifBlank { defaultName }
            val hint = state.hint.trim()
            val now = System.currentTimeMillis()

            if (state.mode == EnrollmentMode.EDIT) {
                val id = state.editingEntryId ?: return@launch
                val existing = repository.getEntryById(id) ?: return@launch

                repository.updateEntry(
                    existing.copy(
                        name = name,
                        hint = hint,
                        payload = payload,
                        updatedAt = now
                    )
                )

                _tapAuthUiState.value = TapAuthUiState(message = "Updated '$name' ✅")
            } else {
                repository.createEntry(
                    name = name,
                    type = type.id,
                    payload = payload,
                    hint = hint
                )
                _tapAuthUiState.value = TapAuthUiState(message = "Saved '$name' ✅")
            }

            startEnrollment()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Entry helpers
    // ---------------------------------------------------------------------------------------------

    fun observeEntryById(id: Long): Flow<AuthEntryEntity?> = repository.observeEntryById(id)

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntryById(entryId)
            _tapAuthUiState.value = TapAuthUiState(message = "Deleted ✅")
        }
    }

    fun clearAuthMessage() {
        _tapAuthUiState.value = TapAuthUiState(lastResult = null, message = "")
    }

    fun setAuthMessage(msg: String) {
        _tapAuthUiState.value = _tapAuthUiState.value.copy(message = msg)
    }

    // ---------------------------------------------------------------------------------------------
    // Authentication helpers (Gate screen)
    // ---------------------------------------------------------------------------------------------

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

    fun authenticateBiometricSuccess(entryId: Long) {
        _tapAuthUiState.value = TapAuthUiState(
            lastResult = true,
            message = "✅ Biometric authentication success"
        )
    }
}