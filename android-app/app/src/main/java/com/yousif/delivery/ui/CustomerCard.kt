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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSuccess
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.ui.theme.NeonCyan
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.PhoneUtils
import com.yousif.delivery.util.TextUtils
import androidx.compose.ui.graphics.Color

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

    GlassCard(modifier = Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NeonCyan.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = TextUtils.initials(customer.name),
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        color = AppTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = PhoneUtils.pretty(customer.primaryPhone),
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MetallicGold.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MetallicGold,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = TextUtils.shortDistance(distanceMeters),
                            color = MetallicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))
                SyncIcon(customer.syncStatus)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable(onClick = onOpenRoute),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = customer.addressNote.ifBlank {
                        TextUtils.coordinates(customer.latitude, customer.longitude)
                    },
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("ڕێگا", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (customer.hasWhatsapp || customer.hasViber || extras.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (customer.hasWhatsapp) MessengerBadge("WA", AppSuccess)
                    if (customer.hasViber) MessengerBadge("VB", Color(0xFF8B5CF6))
                    if (extras.isNotEmpty()) {
                        Text(
                            text = "+${TextUtils.toKurdish(extras.size.toString())} ژمارەی تر",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { extrasExpanded = !extrasExpanded }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            PhoneActionRow(
                number = PhoneUtils.pretty(customer.primaryPhone),
                onCall = { IntentUtils.dial(context, customer.primaryPhone) },
                onWhatsApp = { IntentUtils.whatsapp(context, customer.primaryPhone) },
                onViber = { IntentUtils.viber(context, customer.primaryPhone) },
            )

            AnimatedVisibility(visible = extrasExpanded) {
                Column(Modifier.padding(top = 10.dp)) {
                    extras.forEach { extra ->
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (extra.hasWhatsapp) MessengerBadge("WA", AppSuccess)
                            if (extra.hasViber) MessengerBadge("VB", Color(0xFF8B5CF6))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = AppTextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("دەستکاری", color = AppTextSecondary, fontSize = 12.sp)
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = AppDanger,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("سڕینەوە", color = AppDanger, fontSize = 12.sp)
                }
            }
        }
    }
}
