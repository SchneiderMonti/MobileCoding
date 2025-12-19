package at.ustp.accessgate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun TapAuthScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.tapAuthUiState.collectAsState()

    val tapTimes = remember { mutableStateListOf<Long>() }

    fun intervalsFromTapTimes(times: List<Long>): List<Long> {
        return times.zipWithNext { a, b -> b - a }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Tap Jingle Authentication",
            style = MaterialTheme.typography.headlineSmall
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.LightGray)
                .clickable {
                    tapTimes.add(System.currentTimeMillis())
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Tap here\nTaps recorded: ${tapTimes.size}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val intervals = intervalsFromTapTimes(tapTimes)
                    viewModel.createTapJingleEntry(
                        name = "My Tap Jingle",
                        intervals = intervals
                    )
                    tapTimes.clear()
                }
            ) {
                Text("Save")
            }
        }

        Text(text = state.message)

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}