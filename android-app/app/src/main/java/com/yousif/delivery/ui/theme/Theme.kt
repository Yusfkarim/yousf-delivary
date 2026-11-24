package com.yousif.delivery.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AppBackground = Color(0xFF090D16)
val AppSurface = Color(0xFF111827)
val AppSurfaceHigh = Color(0xFF1B2433)
val NeonCyan = Color(0xFF06B6D4)
val MetallicGold = Color(0xFFF59E0B)
val AppTextPrimary = Color(0xFFF1F5F9)
val AppTextSecondary = Color(0xFF94A3B8)
val AppSuccess = Color(0xFF22C55E)
val AppDanger = Color(0xFFEF4444)

private val colors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF04202A),
    secondary = MetallicGold,
    onSecondary = Color(0xFF241700),
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppTextSecondary,
    error = AppDanger,
    onError = Color.White,
    outline = Color(0xFF2A3648),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YousifTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = Typography(),
        content = content,
    )
}
