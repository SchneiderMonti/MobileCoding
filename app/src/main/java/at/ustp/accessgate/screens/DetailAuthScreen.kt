package at.ustp.accessgate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun DetailAuthScreen(
    entryId: Long,
    viewModel: AuthViewModel,
    onDeleted: () -> Unit = {} // call this to navigate back
) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)

    if (entry == null) {
        Text("Loading...")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Name: ${entry!!.name}\nType: ${entry!!.type}\nPayload: ${entry!!.payload}")

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.deleteEntry(entryId)
                onDeleted()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete")
        }
    }
}
