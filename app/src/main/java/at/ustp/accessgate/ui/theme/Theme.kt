package at.ustp.accessgate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext



private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    background = BaseBackground,
    surface = BaseSurface,
    outline = BaseOutline,
    error = ErrorRed,
    tertiary = SuccessGreen
)

@Composable
fun AccessGateTheme(
    darkTheme: Boolean = false,   // lock light theme for now
    dynamicColor: Boolean = false, // disable dynamic colors
    content: @Composable () -> Unit
) {
    val colorScheme = LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}