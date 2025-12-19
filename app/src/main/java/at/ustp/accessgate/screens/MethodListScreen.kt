package at.ustp.accessgate.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api


@Composable
fun MethodsListScreen(
    onTapJingleClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Text("Choose Authentication Method:")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTapJingleClick)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tap Jingle", style = MaterialTheme.typography.titleMedium)
                    Text("Authenticate by tapping a rhythm", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // placeholders for later:
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Fingerprint", style = MaterialTheme.typography.titleMedium)
                    Text("Coming soon", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Face", style = MaterialTheme.typography.titleMedium)
                    Text("Coming soon", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}