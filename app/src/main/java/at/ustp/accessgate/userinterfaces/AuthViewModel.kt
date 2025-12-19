package at.ustp.accessgate.userinterfaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ustp.accessgate.data.AuthRepository
import at.ustp.accessgate.db.AuthEntryEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

data class TapAuthUiState(
    val lastResult: Boolean? = null,
    val message: String = ""
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    // ✅ List screen uses this
    val entries: Flow<List<AuthEntryEntity>> = repository.entries

    private val _tapAuthUiState = MutableStateFlow(TapAuthUiState())
    val tapAuthUiState: StateFlow<TapAuthUiState> = _tapAuthUiState.asStateFlow()

    // --- TEMP helper until you build a proper Detail screen ---
    // Enroll tap jingle by creating a DB entry with a name.
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

    // Verify a tap attempt against an existing entry (by id).
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

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.deleteEntry(id) }
    }

    // --- Core matching logic (keep this) ---
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