package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.ui.theme.LocalAppColors

/**
 * The creamy «glass» surface used by the goods section: a milky gradient over the
 * cream background, a hairline highlight border and a warm, very soft shadow.
 * At night it turns into smoked espresso glass instead of white.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    radius: Dp = 26.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(radius)
    val fill = if (colors.isDark) {
        Brush.linearGradient(listOf(Color(0xFF2A211C), Color(0xFF1D1714)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFF4E9)))
    }
    val line = if (colors.isDark) Color(0xFF3E3028) else Color(0xFFFFFFFF)
    val glow = if (colors.isDark) Color(0x33000000) else Color(0x1FA57A54)

    val base = modifier
        .fillMaxWidth()
        .shadow(12.dp, shape, clip = false, ambientColor = glow, spotColor = glow)
        .clip(shape)
        .background(fill)
        .border(1.dp, line, shape)

    Column((if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding)) {
        content()
    }
}

/** Round frosted button — used for the quantity stepper. */
@Composable
fun GlassStepButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 34.dp,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(50)
    val bg = if (colors.isDark) Color(0xFF3A2E27) else Color(0xFFFFEFE2)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (colors.isDark) Color(0xFF4A3A31) else Color(0xFFFFFFFF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint ?: colors.brandDeep,
            modifier = Modifier.size(size / 2.1f),
        )
    }
}

/** Small milky chip with an icon and a label. */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
    container: Color? = null,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(container ?: if (colors.isDark) Color(0xFF33281F) else Color(0xFFFFF1E5))
            .border(1.dp, if (colors.isDark) Color(0xFF46362C) else Color(0xFFFFFFFF), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint ?: colors.brandDeep,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = tint ?: colors.textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The empty state of the goods list — a soft glass medallion and two lines. */
@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 30.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(14.dp, RoundedCornerShape(50), clip = false, ambientColor = Color(0x22A57A54), spotColor = Color(0x22A57A54))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        if (colors.isDark) listOf(Color(0xFF332821), Color(0xFF251C17))
                        else listOf(Color(0xFFFFFFFF), Color(0xFFFFEFE0)),
                    ),
                )
                .border(1.dp, if (colors.isDark) Color(0xFF46362C) else Color.White, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.brand, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = hint,
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
        )
    }
}
