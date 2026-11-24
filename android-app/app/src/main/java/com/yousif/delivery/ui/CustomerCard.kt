package com.yousif.delivery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.PhoneUtils
import com.yousif.delivery.util.TextUtils

@Composable
fun CustomerCard(
    customer: Customer,
    distanceMeters: Double?,
    onOpenRoute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var extrasExpanded by remember { mutableStateOf(false) }
    val extras = customer.extraPhones

    AppCard(
        modifier = Modifier.padding(bottom = 12.dp),
        onClick = onOpenRoute,
        padding = 14.dp,
    ) {
        /* ---------- headline ---------- */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = TextUtils.initials(customer.name),
                    color = colors.brandDeep,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PhoneUtils.pretty(customer.primaryPhone),
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    StatPill(
                        text = TextUtils.shortDistance(distanceMeters),
                        icon = AppIcons.pin(),
                        tint = colors.brandDeep,
                        soft = colors.brandSoft,
                    )
                }
            }

            SyncIcon(customer.syncStatus)
            Spacer(Modifier.width(2.dp))
            Icon(
                AppIcons.chevronDown(),
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }

        if (customer.addressNote.isNotBlank() || customer.hasCoordinates) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceAlt)
                    .clickable(onClick = onOpenRoute)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.pin(),
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer.addressNote.ifBlank { "بێ ناونیشان" },
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = TextUtils.coordinates(customer.latitude, customer.longitude),
                        color = colors.textMuted,
                        fontSize = 10.sp,
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.brand)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        AppIcons.route(),
                        contentDescription = null,
                        tint = colors.onBrand,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("ڕێگا", color = colors.onBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (customer.hasWhatsapp || customer.hasViber || extras.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (customer.hasWhatsapp) MessengerBadge("واتسئەپ", colors.successDeep, AppIcons.whatsapp())
                if (customer.hasViber) MessengerBadge("ڤایبەر", colors.brandDeep, AppIcons.viber())
                if (extras.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.brandSoft)
                            .clickable { extrasExpanded = !extrasExpanded }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${TextUtils.toKurdish(extras.size.toString())}+ ژمارە",
                            color = colors.brandDeep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            AppIcons.chevronDown(),
                            contentDescription = null,
                            tint = colors.brandDeep,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PhoneActionRow(
            number = PhoneUtils.pretty(customer.primaryPhone),
            onCall = { IntentUtils.dial(context, customer.primaryPhone) },
            onWhatsApp = { IntentUtils.whatsapp(context, customer.primaryPhone) },
            onViber = { IntentUtils.viber(context, customer.primaryPhone) },
        )

        AnimatedVisibility(visible = extrasExpanded) {
            Column(Modifier.padding(top = 6.dp)) {
                extras.forEach { extra ->
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (extra.hasWhatsapp) MessengerBadge("واتسئەپ", colors.successDeep, AppIcons.whatsapp())
                        if (extra.hasViber) MessengerBadge("ڤایبەر", colors.brandDeep, AppIcons.viber())
                    }
                    PhoneActionRow(
                        number = PhoneUtils.pretty(extra.number),
                        onCall = { IntentUtils.dial(context, extra.number) },
                        onWhatsApp = { IntentUtils.whatsapp(context, extra.number) },
                        onViber = { IntentUtils.viber(context, extra.number) },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconLabelButton("دەستکاری", AppIcons.edit(), colors.textSecondary, onClick = onEdit)
            Spacer(Modifier.width(2.dp))
            IconLabelButton("سڕینەوە", AppIcons.delete(), colors.danger, onClick = onDelete)
        }
    }
}

@Composable
fun SmallAvatar(name: String, size: Int = 42) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(colors.brandSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = TextUtils.initials(name),
            color = colors.brandDeep,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.36f).sp,
        )
    }
}

@Composable
fun ColorDot(color: Color, size: Int = 8) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}
