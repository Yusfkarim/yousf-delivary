package com.yousif.delivery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.GoodItem
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.PhoneUtils
import com.yousif.delivery.util.TextUtils

/**
 * «پرۆفایلی کەس» — a **read-only** page for one customer / shop.
 *
 * This is what «کردنەوە» on the map card opens: the whole person on one quiet
 * screen (phones, address, map preview, goods). Editing only happens when the
 * driver presses the pencil, so opening a pin can never change a record by
 * accident.
 */
@Composable
fun CustomerProfile(
    customer: Customer,
    goods: List<GoodItem>,
    distanceMeters: Double?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRoute: () -> Unit,
    onOpenMap: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val mine = goods.filter { item ->
        item.customerId == customer.id ||
            (item.customerId == null && item.customerName.equals(customer.name, ignoreCase = true))
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            /* ------------------------------------------------ top bar + person */
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftIconButton(
                        icon = AppIcons.chevronEnd(),
                        onClick = onBack,
                        contentDescription = "گەڕانەوە",
                        size = 42.dp,
                    )
                    Spacer(Modifier.weight(1f))
                    SoftIconButton(
                        icon = AppIcons.edit(),
                        onClick = onEdit,
                        contentDescription = "دەستکاریکردن",
                        size = 42.dp,
                        tint = colors.brandDeep,
                    )
                }
            }

            item {
                GlassCard(padding = 16.dp, radius = 26.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (customer.isShop) colors.successSoft else colors.brandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (customer.isShop) {
                                Icon(
                                    AppIcons.store(),
                                    contentDescription = null,
                                    tint = colors.successDeep,
                                    modifier = Modifier.size(28.dp),
                                )
                            } else {
                                Text(
                                    text = TextUtils.initials(customer.name),
                                    color = colors.brandDeep,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                color = colors.textPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassChip(
                                    text = if (customer.isShop) "فرۆشگا" else "کڕیار",
                                    icon = if (customer.isShop) AppIcons.store() else AppIcons.user(),
                                    tint = if (customer.isShop) colors.successDeep else colors.brandDeep,
                                    container = if (customer.isShop) colors.successSoft else colors.brandSoft,
                                )
                                if (distanceMeters != null) {
                                    Spacer(Modifier.width(7.dp))
                                    GlassChip(
                                        text = TextUtils.distance(distanceMeters),
                                        icon = AppIcons.locate(),
                                        tint = colors.textSecondary,
                                        container = colors.surfaceSunken,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /* ---------------------------------------------------------- phones */
            if (customer.allPhones.any { it.isNotBlank() }) {
                item {
                    GlassCard(padding = 14.dp, radius = 24.dp) {
                        Label("ژمارەکان", AppIcons.call())
                        Spacer(Modifier.height(8.dp))
                        val flags = buildList {
                            add(customer.primaryPhone to (customer.hasWhatsapp to customer.hasViber))
                            customer.extraPhones.forEach { add(it.number to (it.hasWhatsapp to it.hasViber)) }
                        }.filter { it.first.isNotBlank() }

                        flags.forEachIndexed { index, entry ->
                            if (index > 0) DividerLine()
                            val (number, apps) = entry
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = PhoneUtils.pretty(number),
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MiniAction(AppIcons.call(), "پەیوەندی", colors.brandDeep, colors.brandSoft) {
                                        IntentUtils.dial(context, number)
                                    }
                                    if (apps.first) {
                                        MiniAction(
                                            AppIcons.whatsapp(),
                                            "واتسئەپ",
                                            colors.successDeep,
                                            colors.successSoft,
                                        ) { IntentUtils.whatsapp(context, number) }
                                    }
                                    if (apps.second) {
                                        MiniAction(AppIcons.viber(), "ڤایبەر", colors.brandDeep, colors.brandSoft) {
                                            IntentUtils.viber(context, number)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /* --------------------------------------------------------- address */
            item {
                GlassCard(padding = 14.dp, radius = 24.dp) {
                    Label("شوێن", AppIcons.pin())
                    if (customer.hasCoordinates) {
                        Spacer(Modifier.height(10.dp))
                        MapThumb(
                            latitude = customer.latitude,
                            longitude = customer.longitude,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenMap() },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = TextUtils.coordinates(customer.latitude, customer.longitude),
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                        )
                    }
                    if (customer.addressNote.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = customer.addressNote,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "ڕێگا",
                            onClick = onRoute,
                            icon = AppIcons.route(),
                            height = 42.dp,
                            fontSize = 12,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(
                            label = "لە نەخشە",
                            onClick = onOpenMap,
                            icon = AppIcons.map(),
                            height = 42.dp,
                            fontSize = 12,
                            container = colors.brandSoft,
                            contentColor = colors.brandDeep,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            /* ------------------------------------------------------------ goods */
            if (mine.isNotEmpty()) {
                item {
                    GlassCard(padding = 14.dp, radius = 24.dp) {
                        Label("کاڵاکان", AppIcons.packageBox())
                        Spacer(Modifier.height(6.dp))
                        mine.forEachIndexed { index, item ->
                            if (index > 0) DividerLine()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.brand),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = TextUtils.toKurdish((index + 1).toString()),
                                        color = colors.onBrand,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = colors.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                    )
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            text = item.note,
                                            color = colors.textMuted,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    text = "×" + TextUtils.toKurdish(item.total.toString()),
                                    color = colors.brandDeep,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }

            /* ----------------------------------------------------------- delete */
            item {
                PillButton(
                    label = "سڕینەوەی ئەم کەسە",
                    onClick = onDelete,
                    icon = AppIcons.delete(),
                    height = 44.dp,
                    fontSize = 12,
                    container = colors.surfaceSunken,
                    contentColor = colors.danger,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Label(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.brand),
        )
        Spacer(Modifier.width(7.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = colors.brand,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** A compact round action used inside the phone rows. */
@Composable
private fun MiniAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
