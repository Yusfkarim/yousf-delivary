package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.SyncStatus
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.GeoUtils
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.PhoneUtils
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.viewmodel.LocationFix

/**
 * Full-screen customer card, laid out like the reference delivery app:
 * orange status hero, grouped info cards, a map card with the two action pills
 * and a delivery timeline.
 */
@Composable
fun RouteDialog(
    customer: Customer,
    myLocation: LocationFix?,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val distance = if (myLocation != null && customer.hasCoordinates) {
        GeoUtils.distanceMeters(
            myLocation.latitude,
            myLocation.longitude,
            customer.latitude,
            customer.longitude,
        )
    } else {
        null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            DetailTopBar(title = if (customer.isShop) "زانیاری فرۆشگا" else "زانیاری کڕیار", onBack = onDismiss)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
            ) {
                StatusHero(
                    title = when {
                        customer.hasCoordinates && customer.isShop -> "شوێنی فرۆشگا تۆمارکراوە"
                        customer.hasCoordinates -> "شوێنی کڕیار تۆمارکراوە"
                        else -> "شوێن تۆمار نەکراوە"
                    },
                    subtitle = TextUtils.distance(distance),
                    time = TextUtils.relativeTime(customer.updatedAt),
                )

                SectionHeader(text = if (customer.isShop) "زانیاری فرۆشگا" else "زانیاری کڕیار")
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallAvatar(name = customer.name, size = 46)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = PhoneUtils.pretty(customer.primaryPhone),
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                        StatPill(
                            text = if (customer.syncStatus == SyncStatus.SYNCED) "هاوکاتکراوە" else "چاوەڕوانە",
                            icon = AppIcons.cloudDone(),
                            tint = if (customer.syncStatus == SyncStatus.SYNCED) colors.successDeep else colors.gold,
                            soft = if (customer.syncStatus == SyncStatus.SYNCED) colors.successSoft else colors.goldSoft,
                        )
                    }
                    DividerLine(Modifier.padding(vertical = 10.dp))
                    InfoRow("ژمارەی مۆبایل", PhoneUtils.pretty(customer.primaryPhone))
                    InfoRow(
                        "ناونیشان",
                        customer.addressNote.ifBlank { "بێ ناونیشان" },
                    )
                    InfoRow(
                        "کۆردینات",
                        TextUtils.coordinatesDisplay(customer.latitude, customer.longitude),
                        valueColor = colors.textSecondary,
                        valueWeight = FontWeight.Medium,
                    )
                    if (customer.extraPhones.isNotEmpty()) {
                        InfoRow(
                            "ژمارەی تر",
                            TextUtils.toKurdish(customer.extraPhones.size.toString()) + " ژمارە",
                        )
                    }
                }

                SectionHeader(text = "شوێن و ڕێگا")
                AppCard(padding = 12.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    ) {
                        TileMap(
                            modifier = Modifier.fillMaxSize(),
                            center = when {
                                customer.hasCoordinates -> MapPoint(customer.latitude, customer.longitude)
                                myLocation != null -> MapPoint(myLocation.latitude, myLocation.longitude)
                                else -> null
                            },
                            zoom = 15.5,
                            interactive = false,
                            me = myLocation?.let { MapPoint(it.latitude, it.longitude, "شوێنی من") },
                            customer = if (customer.hasCoordinates) {
                                MapPoint(customer.latitude, customer.longitude, customer.name)
                            } else {
                                null
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "پەیوەندی",
                            onClick = { IntentUtils.dial(context, customer.primaryPhone) },
                            icon = AppIcons.call(),
                            container = colors.success,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(
                            label = "ناوبردن",
                            onClick = {
                                IntentUtils.googleNavigation(context, customer.latitude, customer.longitude)
                            },
                            icon = AppIcons.navigate(),
                            container = colors.brand,
                            contentColor = colors.onBrand,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "واتسئەپ",
                            onClick = { IntentUtils.whatsapp(context, customer.primaryPhone) },
                            icon = AppIcons.whatsapp(),
                            container = colors.successSoft,
                            contentColor = colors.successDeep,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(
                            label = "کۆپیکردنی شوێن",
                            onClick = {
                                IntentUtils.copyToClipboard(
                                    context,
                                    "coordinates",
                                    TextUtils.coordinatesDisplay(customer.latitude, customer.longitude),
                                )
                                IntentUtils.toast(context, "کۆردینات کۆپی کرا")
                            },
                            icon = AppIcons.copy(),
                            container = colors.surfaceAlt,
                            contentColor = colors.brandDeep,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                SectionHeader(text = "دۆخی گەیاندن")
                AppCard {
                    TimelineStep(
                        title = if (customer.isShop) "فرۆشگا لە لیستەکەدا تۆمارکراوە" else "کڕیار لە لیستەکەدا تۆمارکراوە",
                        caption = TextUtils.relativeTime(customer.createdAt),
                        state = StepState.DONE,
                        isLast = false,
                    )
                    TimelineStep(
                        title = "شوێن و ژمارە پشتڕاستکراونەتەوە",
                        caption = if (customer.hasCoordinates) "شوێن تۆمارکراوە" else "چاوەڕوانی شوێن",
                        state = if (customer.hasCoordinates) StepState.DONE else StepState.PENDING,
                        isLast = false,
                    )
                    TimelineStep(
                        title = "ئامادەیە بۆ گەیاندن",
                        caption = if (customer.syncStatus == SyncStatus.SYNCED) "لە سێرڤەر پارێزراوە" else "چاوەڕوانی هاوکاتکردن",
                        state = if (customer.syncStatus == SyncStatus.SYNCED) StepState.CURRENT else StepState.PENDING,
                        isLast = true,
                    )
                }

                SectionHeader(text = "کورتەی گەشت")
                AppCard {
                    InfoRow("دووری لە تۆوە", TextUtils.distance(distance), valueColor = colors.brandDeep)
                    DividerLine(Modifier.padding(vertical = 6.dp))
                    InfoRow("دوایین نوێکردنەوە", TextUtils.relativeTime(customer.updatedAt))
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoftIconButton(
            icon = AppIcons.chevronEnd(),
            onClick = onBack,
            contentDescription = "گەڕانەوە",
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun StatusHero(title: String, subtitle: String, time: String) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.brand, Color(0xFFFB923C))))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.checkCircle(),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = Color(0xE6FFFFFF),
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x26FFFFFF))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.clock(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(text = time, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private enum class StepState { DONE, CURRENT, PENDING }

@Composable
private fun TimelineStep(
    title: String,
    caption: String,
    state: StepState,
    isLast: Boolean,
) {
    val colors = LocalAppColors.current
    val active = state != StepState.PENDING
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (active) colors.brand else colors.surfaceAlt)
                    .border(1.dp, if (active) colors.brand else colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state == StepState.DONE) AppIcons.check() else AppIcons.clock(),
                    contentDescription = null,
                    tint = if (active) Color.White else colors.textMuted,
                    modifier = Modifier.size(13.dp),
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(if (active) colors.brand.copy(alpha = 0.35f) else colors.border),
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp, bottom = if (isLast) 0.dp else 8.dp),
        ) {
            Text(
                text = title,
                color = if (active) colors.textPrimary else colors.textSecondary,
                fontSize = 12.5.sp,
                fontWeight = if (state == StepState.CURRENT) FontWeight.Bold else FontWeight.Medium,
            )
            Text(
                text = caption,
                color = colors.textMuted,
                fontSize = 10.5.sp,
            )
        }
    }
}
