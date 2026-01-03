package at.ustp.accessgate.ui.theme

import androidx.compose.ui.graphics.Color

val TapJingleColor = Color(0xFF7C4DFF)
val FingerprintColor = Color(0xFF00BFA5)
val FaceColor = Color(0xFF448AFF)
val MotionColor = Color(0xFFFF6D00)
val PinColor = Color(0xFFEF5350)



fun authMethodAccent(type: String): Color =
    when (type) {
        "tap_jingle" -> TapJingleColor
        "fingerprint" -> FingerprintColor
        "face" -> FaceColor
        "motion" -> MotionColor
        "pin" -> PinColor
        else -> BrandPrimary
    }