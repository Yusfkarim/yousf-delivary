package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.SyncStatus
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors

private val CardShape = RoundedCornerShape(22.dp)

/** White (or espresso, at night) card with a hairline border and a soft shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.surface,
    borderColor: Color = LocalAppColors.current.border,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .shadow(elevation = 6.dp, shape = CardShape, clip = false, ambientColor = Color(0x14A0785A), spotColor = Color(0x14A0785A))
        .clip(CardShape)
        .background(color)
        .border(1.dp, borderColor, CardShape)
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(clickable.padding(padding)) { content() }
}

/** Small grey caption that opens a group of cards, exactly like the reference app. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
        )
        trailing?.invoke()
    }
}

/** Orange rounded-square brand mark (crown replacement for the delivery app). */
@Composable
fun BrandBadge(
    size: Dp = 46.dp,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(size / 3)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.brand, Color(0xFFFB923C)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon ?: AppIcons.packageBox(),
            contentDescription = null,
            tint = colors.onBrand,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

/** White rounded-square icon button used in the app bar and card corners. */
@Composable
fun SoftIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color? = null,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(size / 3)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = shape, clip = false, ambientColor = Color(0x12A0785A), spotColor = Color(0x12A0785A))
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textPrimary,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/** Fully rounded action button (the orange / green pills of the reference app). */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = LocalAppColors.current.brand,
    contentColor: Color = LocalAppColors.current.onBrand,
    height: Dp = 46.dp,
    fontSize: Int = 13,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, color = contentColor, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}

/** Tiny pill with an icon + value, used for distance / pending counters. */
@Composable
fun StatPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = LocalAppColors.current.brandDeep,
    soft: Color = LocalAppColors.current.brandSoft,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(soft)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** label on the start side, value on the end side — the row style of the reference cards. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalAppColors.current.textPrimary,
    valueWeight: FontWeight = FontWeight.SemiBold,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = valueWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun GpsDot(active: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .size(8.dp)
            .background(if (active) colors.success else colors.textMuted, CircleShape),
    )
}

/** Green cloud = everything on the server, amber = waiting for internet. */
@Composable
fun SyncIcon(status: SyncStatus, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val synced = status == SyncStatus.SYNCED
    Icon(
        imageVector = if (synced) AppIcons.cloudDone() else AppIcons.cloudWait(),
        contentDescription = "دۆخی هەڵگرتن",
        tint = if (synced) colors.success else colors.gold,
        modifier = modifier.size(18.dp),
    )
}

@Composable
fun MessengerBadge(label: String, color: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IconLabelButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Three one-tap pills under every customer: call / WhatsApp / Viber. */
@Composable
fun PhoneActionRow(
    number: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onViber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillButton(
            label = "پەیوەندی",
            onClick = onCall,
            icon = AppIcons.call(),
            container = colors.success,
            contentColor = Color.White,
            modifier = Modifier.weight(1f),
            height = 44.dp,
            fontSize = 12,
        )
        PillButton(
            label = "واتسئەپ",
            onClick = onWhatsApp,
            icon = AppIcons.whatsapp(),
            container = colors.successSoft,
            contentColor = colors.successDeep,
            modifier = Modifier.weight(1f),
            height = 44.dp,
            fontSize = 12,
        )
        PillButton(
            label = "ڤایبەر",
            onClick = onViber,
            icon = AppIcons.viber(),
            container = colors.brandSoft,
            contentColor = colors.brandDeep,
            modifier = Modifier.weight(1f),
            height = 44.dp,
            fontSize = 12,
        )
    }
    Text(
        text = number,
        color = LocalAppColors.current.textSecondary,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    )
}

/** Friendly empty state with a hand-drawn style list card (no bitmap assets). */
@Composable
fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(colors.brandSoft, colors.brandSoft.copy(alpha = 0.25f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 118.dp, height = 140.dp)
                    .shadow(10.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x1AA0785A), spotColor = Color(0x1AA0785A))
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(12.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandBadge(size = 18.dp, icon = AppIcons.packageBox())
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .height(4.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(colors.brandSoft),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    repeat(4) { index ->
                        Row(
                            modifier = Modifier.padding(bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (index % 2 == 0) colors.brandSoft else colors.surfaceAlt),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (index % 2 == 0) AppIcons.pin() else AppIcons.list(),
                                    contentDescription = null,
                                    tint = colors.brand,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Box(
                                Modifier
                                    .height(4.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surfaceAlt),
                            )
                            Spacer(Modifier.width(7.dp))
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.brandSoft),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            hint,
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalAppColors.current.border),
    )
}

/** Segmented switch (system / day / night, filters, …). */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceAlt)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) colors.surface else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    color = if (selected) colors.brandDeep else colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
