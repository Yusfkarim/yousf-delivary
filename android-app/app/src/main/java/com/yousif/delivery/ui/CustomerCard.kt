package com.yousif.delivery.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSuccess
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.ui.theme.NeonCyan
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
    val context = LocalContext.current
    var extrasExpanded by remember { mutableStateOf(false) }
    val extras = customer.extraPhones
    val viberColor = Color(0xFFA78BFA)

    GlassCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(Modifier.fillMaxWidth()) {
            /* ---------- identity ---------- */
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.30f), NeonCyan.copy(alpha = 0.06f)),
                            ),
                        )
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = TextUtils.initials(customer.name),
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        color = AppTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = PhoneUtils.pretty(customer.primaryPhone),
                        color = AppTextSecondary,
                        fontSize = 12.5.sp,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(MetallicGold.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcons.pin(),
                            contentDescription = null,
                            tint = MetallicGold,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = TextUtils.shortDistance(distanceMeters),
                            color = MetallicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SyncIcon(customer.syncStatus)
                }
            }

            /* ---------- address / route ---------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonCyan.copy(alpha = 0.07f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenRoute)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.pin(),
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer.addressNote.ifBlank { "بێ ناونیشان" },
                        color = AppTextPrimary.copy(alpha = 0.9f),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = TextUtils.coordinates(customer.latitude, customer.longitude),
                        color = AppTextSecondary.copy(alpha = 0.8f),
                        fontSize = 10.5.sp,
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        AppIcons.route(),
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("ڕێگا", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            /* ---------- messenger badges ---------- */
            if (customer.hasWhatsapp || customer.hasViber || extras.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (customer.hasWhatsapp) {
                        MessengerBadge("واتسئەپ", AppSuccess, AppIcons.whatsapp())
                    }
                    if (customer.hasViber) {
                        MessengerBadge("ڤایبەر", viberColor, AppIcons.viber())
                    }
                    if (extras.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.10f))
                                .clickable { extrasExpanded = !extrasExpanded }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${TextUtils.toKurdish(extras.size.toString())}+ ژمارە",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                AppIcons.chevronDown(),
                                contentDescription = null,
                                tint = NeonCyan,
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
                Column(Modifier.padding(top = 8.dp)) {
                    extras.forEach { extra ->
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (extra.hasWhatsapp) MessengerBadge("واتسئەپ", AppSuccess, AppIcons.whatsapp())
                            if (extra.hasViber) MessengerBadge("ڤایبەر", viberColor, AppIcons.viber())
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

            Spacer(Modifier.height(12.dp))
            DividerLine()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconLabelButton("دەستکاری", AppIcons.edit(), AppTextSecondary, onClick = onEdit)
                Spacer(Modifier.width(4.dp))
                IconLabelButton("سڕینەوە", AppIcons.delete(), AppDanger, onClick = onDelete)
            }
        }
    }
}
