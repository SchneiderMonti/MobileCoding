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
    PIN("pin", "Pin Code")
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

            // 1) Validate + 2) Build payload per type
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

                // Example future method:
                 AuthType.PIN -> {
                     val pin = state.firstPin
                     if (pin.length < 4) {
                         _enrollmentUiState.value = state.copy(
                             error = "PIN must be at least 4 digits.",
                             step = EnrollmentStep.DoAuth
                         )
                         return@launch
                     }
                     "PIN" to pin
                 }
            }

            // 3) Save
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



    fun createTapJingleEntry(name: String, intervals: List<Long>) {
        viewModelScope.launch {
            if (intervals.isEmpty()) {
                _tapAuthUiState.value = TapAuthUiState(message = "Not enough taps (need at least 2 taps)")
                return@launch
            }
            val payload = intervals.joinToString(",")
            repository.createEntry(
                name = name,
                type = "tap_jingle",
                payload = payload
            )
            _tapAuthUiState.value = TapAuthUiState(message = "Saved '$name' ✅")
        }
    }

    /*
    fun authenticateTapAgainstEntry(entryId: Long, attemptIntervals: List<Long>) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)

            if (entry == null) {
                _tapAuthUiState.value = TapAuthUiState(lastResult = false, message = "Entry not found")
                return@launch
            }
            if (entry.type != "tap_jingle") {
                _tapAuthUiState.value = TapAuthUiState(lastResult = false, message = "Not a tap-jingle entry")
                return@launch
            }
            if (attemptIntervals.isEmpty()) {
                _tapAuthUiState.value = TapAuthUiState(lastResult = false, message = "Not enough taps")
                return@launch
            }

            val enrolledIntervals = entry.payload.split(",").mapNotNull { it.toLongOrNull() }
            val success = tapMatches(enrolledIntervals, attemptIntervals)

            _tapAuthUiState.value = TapAuthUiState(
                lastResult = success,
                message = if (success) "✅ Authentication success" else "❌ Authentication failed"
            )
        }
    }

     */



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
}
