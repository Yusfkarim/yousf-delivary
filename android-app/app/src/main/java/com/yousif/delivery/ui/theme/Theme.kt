package com.yousif.delivery.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * One palette object for the whole app. Two instances ship: a warm cream one for
 * daylight and a deep espresso one for night — both use the same warm orange
 * brand so the app never feels like two different products.
 */
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceSunken: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val brand: Color,
    val brandDeep: Color,
    val brandSoft: Color,
    val onBrand: Color,
    val success: Color,
    val successDeep: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val gold: Color,
    val goldSoft: Color,
)

val LightColors = AppColors(
    isDark = false,
    background = Color(0xFFFFF8F3),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFFFF1E6),
    surfaceSunken = Color(0xFFFDF3EC),
    border = Color(0xFFF2E2D6),
    textPrimary = Color(0xFF2A1A11),
    textSecondary = Color(0xFF8B7768),
    textMuted = Color(0xFFBBA99B),
    brand = Color(0xFFF97316),
    brandDeep = Color(0xFFC2410C),
    brandSoft = Color(0xFFFFEADB),
    onBrand = Color(0xFFFFFFFF),
    success = Color(0xFF22C55E),
    successDeep = Color(0xFF15803D),
    successSoft = Color(0xFFE6F9EE),
    danger = Color(0xFFEF4444),
    dangerSoft = Color(0xFFFDECEC),
    gold = Color(0xFFF59E0B),
    goldSoft = Color(0xFFFFF5E3),
)

val DarkColors = AppColors(
    isDark = true,
    background = Color(0xFF15100D),
    surface = Color(0xFF201914),
    surfaceAlt = Color(0xFF2B211A),
    surfaceSunken = Color(0xFF1A1410),
    border = Color(0xFF372A21),
    textPrimary = Color(0xFFF9F1EA),
    textSecondary = Color(0xFFB5A296),
    textMuted = Color(0xFF7E6D61),
    brand = Color(0xFFFB8C3C),
    brandDeep = Color(0xFFFFC08A),
    brandSoft = Color(0xFF3A2415),
    onBrand = Color(0xFF2A1508),
    success = Color(0xFF4ADE80),
    successDeep = Color(0xFF86EFAC),
    successSoft = Color(0xFF152F1F),
    danger = Color(0xFFF87171),
    dangerSoft = Color(0xFF3A1C1C),
    gold = Color(0xFFFBBF24),
    goldSoft = Color(0xFF3A2A12),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YousifTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.brand,
            onPrimary = palette.onBrand,
            secondary = palette.success,
            onSecondary = palette.onBrand,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = palette.brand,
            onPrimary = palette.onBrand,
            secondary = palette.success,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
            onError = Color.White,
        )
    }

    CompositionLocalProvider(LocalAppColors provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = AppShapes,
            typography = Typography(),
            content = content,
        )
    }
}
