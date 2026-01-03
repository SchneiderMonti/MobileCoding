@file:OptIn(ExperimentalMaterial3Api::class)

package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.userinterfaces.AuthViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DetailAuthScreen(
    entryId: Long,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onUpdate: (Long) -> Unit
) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)

    LaunchedEffect(entryId) {
        viewModel.clearAuthMessage()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (entry == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Loading...")
            }
            return@Scaffold
        }

        val e = entry!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Name: ${e.name}", style = MaterialTheme.typography.titleMedium)
            Text("Hint: ${e.hint}", style = MaterialTheme.typography.titleMedium)
            Text("Type: ${e.type}", style = MaterialTheme.typography.bodyMedium)
            Text("Created At: ${formatTimestamp(e.createdAt)}", style = MaterialTheme.typography.bodyMedium)
            Text("Updated At: ${formatTimestamp(e.updatedAt)}", style = MaterialTheme.typography.bodyMedium)

            Divider()

            Text("Payload: ${e.payload}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        viewModel.deleteEntry(entryId)
                        viewModel.clearAuthMessage()
                        onDeleted()
                    }
                ) { Text("Delete") }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        viewModel.startEditEnrollment(entryId)
                        viewModel.clearAuthMessage()
                        onUpdate(entryId)
                    }
                ) { Text("Update") }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    return formatter.format(Instant.ofEpochMilli(timestamp))
}