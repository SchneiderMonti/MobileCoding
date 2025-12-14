package at.ustp.accessgate.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MethodsListScreen(
    onTapJingleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Authentication Methods", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = onTapJingleClick) {
            Text("Tap Jingle (Knock Code)")
        }
    }
}