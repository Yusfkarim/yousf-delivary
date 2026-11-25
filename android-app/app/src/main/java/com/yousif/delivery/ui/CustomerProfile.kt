package com.yousif.delivery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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
 * One clean column: identity, phones, address, goods. Nothing overlaps, every
 * action sits on its own line, and editing only happens from the pencil — so
 * opening a pin can never change a record by accident.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        /* ------------------------------------------------------------ top bar */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftIconButton(
                icon = AppIcons.chevronEnd(),
                onClick = onBack,
                contentDescription = "گەڕانەوە",
                size = 40.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    text = "پرۆفایل",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = if (customer.isShop) "فرۆشگا" else "کڕیار",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )
            }
            SoftIconButton(
                icon = AppIcons.edit(),
                onClick = onEdit,
                contentDescription = "دەستکاریکردن",
                size = 40.dp,
                tint = colors.brandDeep,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            /* ---------------------------------------------------------- identity */
            item {
                GlassCard(padding = 14.dp, radius = 24.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (customer.isShop) colors.successSoft else colors.brandSoft,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (customer.isShop) {
                                Icon(
                                    AppIcons.store(),
                                    contentDescription = null,
                                    tint = colors.successDeep,
                                    modifier = Modifier.size(26.dp),
                                )
                            } else {
                                Text(
                                    text = TextUtils.initials(customer.name),
                                    color = colors.brandDeep,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlassChip(
                                    text = if (customer.isShop) "فرۆشگا" else "کڕیار",
                                    icon = if (customer.isShop) AppIcons.store() else AppIcons.user(),
                                    tint = if (customer.isShop) colors.successDeep else colors.brandDeep,
                                    container = if (customer.isShop) colors.successSoft else colors.brandSoft,
                                )
                                if (distanceMeters != null) {
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

            /* ------------------------------------------------------------ phones */
            val phones = buildList {
                add(Triple(customer.primaryPhone, customer.hasWhatsapp, customer.hasViber))
                customer.extraPhones.forEach { extra ->
                    add(Triple(extra.number, extra.hasWhatsapp, extra.hasViber))
                }
            }.filter { it.first.isNotBlank() }

            if (phones.isNotEmpty()) {
                item {
                    GlassCard(padding = 14.dp, radius = 24.dp) {
                        SectionLabel("ژمارەکان", AppIcons.call())
                        Spacer(Modifier.height(6.dp))
                        phones.forEachIndexed { index, phone ->
                            if (index > 0) DividerLine()
                            PhoneBlock(
                                number = phone.first,
                                whatsapp = phone.second,
                                viber = phone.third,
                            )
                        }
                    }
                }
            }

            /* ----------------------------------------------------------- address */
            item {
                GlassCard(padding = 14.dp, radius = 24.dp) {
                    SectionLabel("شوێن", AppIcons.pin())
                    if (customer.hasCoordinates) {
                        Spacer(Modifier.height(10.dp))
                        MapThumb(
                            latitude = customer.latitude,
                            longitude = customer.longitude,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(146.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenMap() },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = TextUtils.coordinatesDisplay(customer.latitude, customer.longitude),
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "شوێن دیاری نەکراوە",
                            color = colors.textMuted,
                            fontSize = 12.sp,
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

            /* ------------------------------------------------------------- goods */
            if (mine.isNotEmpty()) {
                item {
                    GlassCard(padding = 14.dp, radius = 24.dp) {
                        SectionLabel("کاڵاکان", AppIcons.packageBox())
                        Spacer(Modifier.height(4.dp))
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

            /* ------------------------------------------------------------ delete */
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

/* --------------------------------------------------------------- building blocks */

/** Number on its own line, actions on the next one — never squeezed. */
@Composable
private fun PhoneBlock(number: String, whatsapp: Boolean, viber: Boolean) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = PhoneUtils.pretty(number),
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ActionTile(
                icon = AppIcons.call(),
                label = "پەیوەندی",
                brand = colors.brand,
                modifier = Modifier.weight(1f),
            ) { IntentUtils.dial(context, number) }
            if (whatsapp) {
                ActionTile(
                    icon = AppIcons.whatsapp(),
                    label = "واتسئەپ",
                    brand = WhatsAppGreen,
                    modifier = Modifier.weight(1f),
                ) { IntentUtils.whatsapp(context, number) }
            }
            if (viber) {
                ActionTile(
                    icon = AppIcons.viber(),
                    label = "ڤایبەر",
                    brand = ViberPurple,
                    modifier = Modifier.weight(1f),
                ) { IntentUtils.viber(context, number) }
            }
        }
    }
}

/** Official brand colours, so the real logos read instantly. */
private val WhatsAppGreen = Color(0xFF25D366)
private val ViberPurple = Color(0xFF7360F2)

/**
 * One contact action: the real brand mark in a solid brand-coloured disc,
 * with a quiet label underneath. Three of them sit side by side, equal width.
 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    brand: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    val soft = if (colors.isDark) brand.copy(alpha = 0.18f) else brand.copy(alpha = 0.09f)
    Column(
        modifier = modifier
            .height(68.dp)
            .shadow(9.dp, shape, clip = false, ambientColor = brand.copy(alpha = 0.30f), spotColor = brand.copy(alpha = 0.30f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(soft, Color.White, 0.30f), soft)))
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .shadow(6.dp, CircleShape, clip = false, ambientColor = brand.copy(alpha = 0.6f), spotColor = brand.copy(alpha = 0.6f))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(lerp(brand, Color.White, 0.28f), brand, lerp(brand, Color.Black, 0.12f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(colors.brand, colors.brandDeep))),
        )
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.brandDeep,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

