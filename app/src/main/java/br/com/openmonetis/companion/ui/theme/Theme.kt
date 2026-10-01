package br.com.openmonetis.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.openmonetis.companion.R

// sRGB conversions of apps/web/src/styles/app.css in the OpenMonetis project.
// primary = --brand, secondary = --brand-strong, tertiary = --info.
val BrandOrange = Color(0xFFFC941D)
private val Charcoal = Color(0xFF3F2508)
private val LightBackground = Color(0xFFF6F4F1)
private val DarkBackground = Color(0xFF111110)
private val DarkForeground = Color(0xFFF7F4F0)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Charcoal,
    primaryContainer = Color(0xFFF7E5CF),
    onPrimaryContainer = Charcoal,
    secondary = Color(0xFF945712),
    onSecondary = LightBackground,
    secondaryContainer = Color(0xFFEFE9DF),
    onSecondaryContainer = Charcoal,
    tertiary = Color(0xFF3950AD),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCDDE7),
    onTertiaryContainer = Color(0xFF3950AD),
    error = Color(0xFFD72204),
    onError = Color.White,
    errorContainer = Color(0xFFF2DBD5),
    onErrorContainer = Charcoal,
    background = LightBackground,
    onBackground = Charcoal,
    surface = LightBackground,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFEFE9DF),
    onSurfaceVariant = Color(0xFF7A6753),
    surfaceTint = Color.Transparent,
    inverseSurface = DarkBackground,
    inverseOnSurface = DarkForeground,
    inversePrimary = BrandOrange,
    outline = Color(0xFFBFB6AB),
    outlineVariant = Color(0xFFCCCAC8),
    scrim = Charcoal,
    surfaceBright = LightBackground,
    surfaceDim = Color(0xFFEFE9DF),
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightBackground,
    surfaceContainerHigh = LightBackground,
    surfaceContainerHighest = Color(0xFFEFE9DF),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Charcoal,
    primaryContainer = Color(0xFF40311F),
    onPrimaryContainer = DarkForeground,
    secondary = BrandOrange,
    onSecondary = Charcoal,
    secondaryContainer = Color(0xFF2A2A29),
    onSecondaryContainer = DarkForeground,
    tertiary = Color(0xFF91A7FF),
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF232631),
    onTertiaryContainer = Color(0xFF91A7FF),
    error = Color(0xFFFF786F),
    onError = DarkBackground,
    errorContainer = Color(0xFF2E1D1B),
    onErrorContainer = Color(0xFFFF786F),
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = Color(0xFF1A1A19),
    onSurface = DarkForeground,
    surfaceVariant = Color(0xFF262524),
    onSurfaceVariant = Color(0xFFADABA8),
    surfaceTint = Color.Transparent,
    inverseSurface = LightBackground,
    inverseOnSurface = Charcoal,
    inversePrimary = Color(0xFF945712),
    outline = Color(0xFF4A4A48),
    outlineVariant = Color(0xFF282826),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A2A29),
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = Color(0xFF1A1A19),
    surfaceContainer = Color(0xFF212120),
    surfaceContainerHigh = Color(0xFF262524),
    surfaceContainerHighest = Color(0xFF2A2A29),
)

// Status tokens follow the active app theme, including inside dialogs.
val ColorScheme.success: Color
    get() = if (background.luminance() > 0.5f) Color(0xFF287A50) else Color(0xFF6ACB93)

val ColorScheme.warning: Color
    get() = if (background.luminance() > 0.5f) Color(0xFFC37317) else Color(0xFFF5B56B)

private val BrandFontFamily = FontFamily(
    Font(R.font.gt_america_regular, FontWeight.Normal),
    Font(R.font.gt_america_medium, FontWeight.Medium),
    Font(R.font.gt_america_bold, FontWeight.Bold),
)

private val DefaultTypography = Typography()
private val BrandTypography = with(DefaultTypography) {
    copy(
        displayLarge = displayLarge.copy(fontFamily = BrandFontFamily),
        displayMedium = displayMedium.copy(fontFamily = BrandFontFamily),
        displaySmall = displaySmall.copy(fontFamily = BrandFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = BrandFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = BrandFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = BrandFontFamily),
        titleLarge = titleLarge.copy(fontFamily = BrandFontFamily),
        titleMedium = titleMedium.copy(fontFamily = BrandFontFamily),
        titleSmall = titleSmall.copy(fontFamily = BrandFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = BrandFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = BrandFontFamily),
        bodySmall = bodySmall.copy(fontFamily = BrandFontFamily),
        labelLarge = labelLarge.copy(fontFamily = BrandFontFamily),
        labelMedium = labelMedium.copy(fontFamily = BrandFontFamily),
        labelSmall = labelSmall.copy(fontFamily = BrandFontFamily),
    )
}

private val BrandShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun OpenMonetisCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = BrandTypography,
        shapes = BrandShapes,
        content = content
    )
}
