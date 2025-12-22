package at.ustp.accessgate.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun AuthListScreen(
    viewModel: AuthViewModel,
    onAddAuthClick: () -> Unit,
    onEntryClick: (Long) -> Unit
) {

    val entries by viewModel.entries.collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAuthClick) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Authentication Methods",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text("No enrollments yet")
            } else {
                LazyColumn {
                    items(
                        items = entries,
                        key = { it.id }
                    ) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.name) },
                            supportingContent = { Text(entry.type) },
                            modifier = Modifier.clickable {
                                onEntryClick(entry.id)
                            }
                        )
                    }
                }
            }
        }
    }
}