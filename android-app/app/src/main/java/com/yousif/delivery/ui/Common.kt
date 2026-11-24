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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.SyncStatus
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppSurfaceHigh
import com.yousif.delivery.ui.theme.AppSuccess
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.ui.theme.NeonCyan

/** Frosted card used by every list row / dialog. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(22.dp)),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(Modifier.padding(14.dp)) { content() }
    }
}

@Composable
fun GpsDot(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(9.dp)
            .background(if (active) AppSuccess else AppDanger, CircleShape),
    )
}

/** Green cloud = everything on the server, amber = waiting for internet. */
@Composable
fun SyncIcon(status: SyncStatus, modifier: Modifier = Modifier) {
    val (icon: ImageVector, tint: Color) = when (status) {
        SyncStatus.SYNCED -> Icons.Filled.CloudDone to AppSuccess
        SyncStatus.PENDING_UPSERT -> Icons.Filled.CloudQueue to MetallicGold
        SyncStatus.PENDING_DELETE -> Icons.Filled.CloudQueue to MetallicGold
    }
    Icon(icon, contentDescription = "دۆخی هەڵگرتن", tint = tint, modifier = modifier.size(16.dp))
}

@Composable
fun MessengerBadge(label: String, color: Color) {
    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AppTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp),
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
        PhoneActionButton("پەیوەندی", Icons.Filled.Phone, NeonCyan, Modifier.weight(1f), onCall)
        PhoneActionButton("واتسئەپ", Icons.Filled.Chat, AppSuccess, Modifier.weight(1f), onWhatsApp)
        PhoneActionButton("ڤایبەر", Icons.Filled.Send, Color(0xFF8B5CF6), Modifier.weight(1f), onViber)
    }
    Text(
        text = number,
        color = AppTextSecondary,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

@Composable
private fun PhoneActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptyState(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = AppTextSecondary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(hint, color = AppTextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppSurfaceHigh),
    )
}
