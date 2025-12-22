package at.ustp.accessgate.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.ustp.accessgate.userinterfaces.AuthViewModel

@Composable
fun DetailAuthScreen(entryId: Long, viewModel: AuthViewModel) {
    val entry by viewModel.observeEntryById(entryId).collectAsState(initial = null)

    if (entry == null) {
        Text("Loading...")
    } else {
        Text("Name: ${entry!!.name}\nType: ${entry!!.type}\nPayload: ${entry!!.payload}")
    }
}
