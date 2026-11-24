package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.SyncStatus
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSuccess
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppSurfaceHigh
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.ui.theme.NeonCyan

/** Soft glass card with a subtle vertical gradient + hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF141D2C), Color(0xFF0E1521))),
            )
            .border(1.dp, Color(0x1F94A3B8), shape),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun GpsDot(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .background(if (active) AppSuccess else AppDanger, CircleShape),
    )
}

/** Green cloud = everything on the server, amber = waiting for internet. */
@Composable
fun SyncIcon(status: SyncStatus, modifier: Modifier = Modifier) {
    val tint = when (status) {
        SyncStatus.SYNCED -> AppSuccess
        SyncStatus.PENDING_UPSERT, SyncStatus.PENDING_DELETE -> MetallicGold
    }
    val icon = when (status) {
        SyncStatus.SYNCED -> AppIcons.cloudDone()
        SyncStatus.PENDING_UPSERT, SyncStatus.PENDING_DELETE -> AppIcons.cloudWait()
    }
    Icon(icon, contentDescription = "دۆخی هەڵگرتن", tint = tint, modifier = modifier.size(18.dp))
}

@Composable
fun MessengerBadge(label: String, color: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AppTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Three big one-tap buttons: call / WhatsApp / Viber. */
@Composable
fun PhoneActionRow(
    number: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onViber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhoneActionButton("پەیوەندی", AppIcons.call(), NeonCyan, Modifier.weight(1f), highlighted = true, onClick = onCall)
        PhoneActionButton("واتسئەپ", AppIcons.whatsapp(), AppSuccess, Modifier.weight(1f), onClick = onWhatsApp)
        PhoneActionButton("ڤایبەر", AppIcons.viber(), Color(0xFFA78BFA), Modifier.weight(1f), onClick = onViber)
    }
    Text(
        text = number,
        color = AppTextSecondary,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    )
}

@Composable
private fun PhoneActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(tint.copy(alpha = if (highlighted) 0.20f else 0.11f))
            .border(1.dp, tint.copy(alpha = if (highlighted) 0.55f else 0.20f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Pill-shaped sort / filter toggle. */
@Composable
fun SortPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val tint = if (selected) NeonCyan else AppTextSecondary
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) NeonCyan.copy(alpha = 0.14f) else AppSurface)
            .border(1.dp, if (selected) NeonCyan.copy(alpha = 0.5f) else Color(0x1A94A3B8), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) AppTextPrimary else AppTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Small text-only action with a line icon, used for edit / delete rows. */
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(NeonCyan.copy(alpha = 0.22f), NeonCyan.copy(alpha = 0.03f)),
                    ),
                )
                .border(1.dp, NeonCyan.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.map(), contentDescription = null, tint = NeonCyan, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            hint,
            color = AppTextSecondary.copy(alpha = 0.75f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppSurfaceHigh.copy(alpha = 0.7f)),
    )
}
