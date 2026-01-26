package br.com.opensheets.companion.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// OpenSheets Brand Colors - Terracotta Orange
val Primary = Color(0xFFE87040)
val PrimaryVariant = Color(0xFFD45D30)
val Secondary = Color(0xFF8B7355)
val SecondaryVariant = Color(0xFF6B5A45)

val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFDC2626)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE8DB),
    onPrimaryContainer = Color(0xFF3D1F0F),
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E0D5),
    onSecondaryContainer = Color(0xFF2D2620),
    tertiary = Color(0xFFB87333),
    onTertiary = Color.White,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF7F4F0),
    onBackground = Color(0xFF2D2926),
    surface = Color.White,
    onSurface = Color(0xFF2D2926),
    surfaceVariant = Color(0xFFF0ECE8),
    onSurfaceVariant = Color(0xFF6B635C),
    outline = Color(0xFFD9D4CE),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE87040),
    onPrimary = Color(0xFF2B2B2B),
    primaryContainer = Color(0xFF5C2D1A),
    onPrimaryContainer = Color(0xFFFDE8DB),
    secondary = Color(0xFFA08B70),
    onSecondary = Color(0xFF2D2620),
    secondaryContainer = Color(0xFF4A3D30),
    onSecondaryContainer = Color(0xFFE8E0D5),
    tertiary = Color(0xFFD4915C),
    onTertiary = Color(0xFF2E1A0A),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF2B2B2B),
    onBackground = Color(0xFFE8E4DC),
    surface = Color(0xFF3A3836),
    onSurface = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF4A4643),
    onSurfaceVariant = Color(0xFFB0A99F),
    outline = Color(0xFF5C5650),
)

@Composable
fun OpenSheetsCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
