package at.ustp.accessgate.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import at.ustp.accessgate.userinterfaces.AuthType


@Composable
fun authTypeIcon(type: String) = when (type) {
    AuthType.TAP_JINGLE.id -> Icons.Default.TouchApp
    AuthType.PIN.id -> Icons.Default.Pin
    AuthType.FINGERPRINT.id -> Icons.Default.Fingerprint
    AuthType.FLIP_PATTERN.id -> Icons.Default.ScreenRotation
    else -> Icons.Default.Lock
}