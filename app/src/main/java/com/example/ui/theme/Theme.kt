package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CalculatorPalette(
    val displayContainer: Color,
    val displayBorder: Color,
    val digitKeyContainer: Color,
    val digitKeyContent: Color,
    val utilityKeyContainer: Color,
    val utilityKeyContent: Color,
    val clearKeyContainer: Color,
    val clearKeyContent: Color,
    val operatorKeyContainer: Color,
    val operatorKeyContent: Color,
    val scientificKeyContainer: Color,
    val scientificKeyContent: Color,
    val equalsKeyContainer: Color,
    val equalsKeyContent: Color,
    val keyShadowBorder: Color,
    val subtleText: Color,
)

private val LightCalculatorPalette = CalculatorPalette(
    displayContainer = WarmPaperSurface,
    displayBorder = HairlineWarm,
    digitKeyContainer = PureIvoryKey,
    digitKeyContent = CharcoalInk,
    utilityKeyContainer = StoneUtilityKey,
    utilityKeyContent = CharcoalInk,
    clearKeyContainer = Color(0xFFF3DDD6),
    clearKeyContent = Color(0xFFB53B14),
    operatorKeyContainer = SlateOperatorKey,
    operatorKeyContent = DeepSlateBlue,
    scientificKeyContainer = Color(0xFFE7E3DA),
    scientificKeyContent = MutedGraphite,
    equalsKeyContainer = TerracottaAccent,
    equalsKeyContent = Color.White,
    keyShadowBorder = Color(0x18000000),
    subtleText = MutedGraphite,
)

private val DarkCalculatorPalette = CalculatorPalette(
    displayContainer = CarbonSurface,
    displayBorder = HairlineDark,
    digitKeyContainer = GraphiteDigitKey,
    digitKeyContent = WarmSnowInk,
    utilityKeyContainer = CharcoalUtilityKey,
    utilityKeyContent = WarmSnowInk,
    clearKeyContainer = Color(0xFF3A2422),
    clearKeyContent = Color(0xFFFF8F73),
    operatorKeyContainer = SteelOperatorKey,
    operatorKeyContent = IceSteelBlue,
    scientificKeyContainer = Color(0xFF1F2229),
    scientificKeyContent = SilverMutedInk,
    equalsKeyContainer = CoralAccent,
    equalsKeyContent = Color(0xFF14110F),
    keyShadowBorder = Color(0x28FFFFFF),
    subtleText = SilverMutedInk,
)

val LocalCalculatorPalette = staticCompositionLocalOf { LightCalculatorPalette }

private val DarkColorScheme = darkColorScheme(
    primary = CoralAccent,
    onPrimary = Color(0xFF14110F),
    secondary = IceSteelBlue,
    onSecondary = Color(0xFF121A24),
    tertiary = CharcoalUtilityKey,
    background = ObsidianBackground,
    onBackground = WarmSnowInk,
    surface = CarbonSurface,
    onSurface = WarmSnowInk,
    surfaceVariant = GraphiteDigitKey,
    onSurfaceVariant = SilverMutedInk,
    outline = HairlineDark,
    error = SoftErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaAccent,
    onPrimary = Color.White,
    secondary = DeepSlateBlue,
    onSecondary = Color.White,
    tertiary = StoneUtilityKey,
    background = AlabasterBackground,
    onBackground = CharcoalInk,
    surface = WarmPaperSurface,
    onSurface = CharcoalInk,
    surfaceVariant = PureIvoryKey,
    onSurfaceVariant = MutedGraphite,
    outline = HairlineWarm,
    error = SoftErrorLight,
)

@Composable
fun CalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkCalculatorPalette else LightCalculatorPalette

    CompositionLocalProvider(LocalCalculatorPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CalculatorTypography,
            content = content,
        )
    }
}
