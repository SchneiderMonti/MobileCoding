package at.ustp.accessgate.userinterfaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ustp.accessgate.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class TapAuthUiState(
    val isEnrolled: Boolean = false,
    val lastResult: Boolean? = null,  // null = no attempt yet
    val message: String = ""
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _tapAuthUiState = MutableStateFlow(TapAuthUiState())
    val tapAuthUiState: StateFlow<TapAuthUiState> = _tapAuthUiState.asStateFlow()

    init {
        // On startup: check if enrollment exists
        viewModelScope.launch {
            val enrolled = repository.loadTapEnrollment() != null
            _tapAuthUiState.value = _tapAuthUiState.value.copy(
                isEnrolled = enrolled,
                message = if (enrolled) "Tap jingle enrolled" else "Not enrolled yet"
            )
        }
    }

    fun enrollTap(intervals: List<Long>) {
        viewModelScope.launch {
            if (intervals.isEmpty()) {
                _tapAuthUiState.value = _tapAuthUiState.value.copy(
                    message = "Not enough taps (need at least 2 taps)"
                )
                return@launch
            }

            repository.saveTapEnrollment(intervals)

            _tapAuthUiState.value = _tapAuthUiState.value.copy(
                isEnrolled = true,
                lastResult = null,
                message = "Enrolled tap jingle!"
            )
        }
    }

    fun clearTapEnrollment() {
        viewModelScope.launch {
            repository.clearTapEnrollment()
            _tapAuthUiState.value = _tapAuthUiState.value.copy(
                isEnrolled = false,
                lastResult = null,
                message = "Enrollment cleared"
            )
        }
    }

    fun authenticateTap(intervals: List<Long>) {
        viewModelScope.launch {
            val enrolled = repository.loadTapEnrollment()

            if (enrolled == null) {
                _tapAuthUiState.value = _tapAuthUiState.value.copy(
                    lastResult = false,
                    message = "No enrollment saved yet"
                )
                return@launch
            }

            if (intervals.isEmpty()) {
                _tapAuthUiState.value = _tapAuthUiState.value.copy(
                    lastResult = false,
                    message = "Not enough taps (need at least 2 taps)"
                )
                return@launch
            }

            val success = tapMatches(enrolled, intervals)

            _tapAuthUiState.value = _tapAuthUiState.value.copy(
                lastResult = success,
                message = if (success) "✅ Authentication success" else "❌ Authentication failed"
            )
        }
    }

    // --- Core matching logic (simple + good enough) ---
    private fun tapMatches(enrolled: List<Long>, attempt: List<Long>): Boolean {
        if (enrolled.size != attempt.size) return false

        val enrolledAvg = enrolled.average()
        val attemptAvg = attempt.average()

        if (enrolledAvg == 0.0) return false

        val scale = attemptAvg / enrolledAvg
        val toleranceMs = 120.0

        return enrolled.indices.all { i ->
            val expected = enrolled[i] * scale
            abs(attempt[i] - expected) <= toleranceMs
        }
    }
}