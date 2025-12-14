package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun AuthListScreen(
    viewModel: AuthViewModel,
    onAddAuthenticationClick: () -> Unit,
    onTapJingleClick: () -> Unit
) {
    val enrollments by viewModel.enrollments.collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAuthenticationClick) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Authentication Methods", style = MaterialTheme.typography.headlineSmall)

            if (enrollments.isEmpty()) {
                Text("No enrollments yet.")
            } else {
                Text("Enrollments in DB: ${enrollments.size}")
                // Later you’ll list them properly with LazyColumn
            }

            // For now your demo method button
            Button(onClick = onTapJingleClick) {
                Text("Tap Jingle (Knock Code)")
            }
        }
    }
}