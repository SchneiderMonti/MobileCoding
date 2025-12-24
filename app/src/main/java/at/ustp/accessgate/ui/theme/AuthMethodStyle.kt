package at.ustp.accessgate.ui.theme

import androidx.compose.ui.graphics.Color

// Method accent colors (tweak freely)
val TapJingleColor = Color(0xFF7C4DFF)
val FingerprintColor = Color(0xFF00BFA5)
val FaceColor = Color(0xFF448AFF)
val MotionColor = Color(0xFFFF6D00)
val PinColor = Color(0xFFEF5350)

// Brand colors (you already have BrandPrimary/BrandSecondary in your theme)
// If you already defined BrandPrimary elsewhere, remove this line.
// val BrandPrimary = Color(0xFF6750A4)

fun authMethodAccent(type: String): Color =
    when (type) {
        "tap_jingle" -> TapJingleColor
        "fingerprint" -> FingerprintColor
        "face" -> FaceColor
        "motion" -> MotionColor
        "pin" -> PinColor
        else -> BrandPrimary
    }