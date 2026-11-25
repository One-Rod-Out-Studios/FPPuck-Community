package org.fppuck.community.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class FppuckAppearance(val label: String) {
    LIGHT("Light"), DARK("Dark"), SYSTEM("System")
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA0A7),
    onPrimary = Color(0xFF420910),
    primaryContainer = Color(0xFF643039),
    onPrimaryContainer = Color(0xFFFFDADD),
    secondaryContainer = Color(0xFF39252A),
    onSecondaryContainer = Color(0xFFFFDADD),
    tertiary = Color(0xFFF64A59),
    background = Color(0xFF0D1117),
    surface = Color(0xFF121B23),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9AA6B2),
    outline = Color(0xFF8393A1),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFA7192C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADD),
    onPrimaryContainer = Color(0xFF410009),
    secondaryContainer = Color(0xFFF6DDE0),
    onSecondaryContainer = Color(0xFF3B171C),
    tertiary = Color(0xFFA7192C),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    onBackground = Color(0xFF19232D),
    onSurface = Color(0xFF19232D),
    onSurfaceVariant = Color(0xFF52616F),
)

@Composable
fun FPHLTheme(appearance: FppuckAppearance, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        FppuckAppearance.LIGHT -> false
        FppuckAppearance.DARK -> true
        FppuckAppearance.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography, content = content)
}

internal val FphlBackground: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
internal val FphlCard: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
internal val FphlText: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onBackground
internal val FphlMutedText: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val FphlRed: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
