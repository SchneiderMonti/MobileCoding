package at.ustp.accessgate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.ui.theme.authMethodAccent
import at.ustp.accessgate.userinterfaces.AuthType
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.ui.theme.authTypeIcon

@Composable
fun AuthListScreen(
    viewModel: AuthViewModel,
    onAddAuthClick: () -> Unit,
    onEntryClick: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAuthClick,
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add authentication",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Your Saved Authentications",
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
                        val accent = authMethodAccent(entry.type)

                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(accent, shape = MaterialTheme.shapes.small)
                                )
                            },
                            headlineContent = { Text(entry.name) },
                            supportingContent = { Text(entry.type) },
                            trailingContent = {
                                Icon(
                                    imageVector = authTypeIcon(entry.type),
                                    contentDescription = entry.type,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .clickable { onEntryClick(entry.id) }
                                .fillMaxWidth()
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

