package at.ustp.accessgate.data

data class AuthMethod (
    val id: String,          // "tap_jingle"
    val displayName: String, // "Tap Jingle"
    val enrolled: Boolean
)