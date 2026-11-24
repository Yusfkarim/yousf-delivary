package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.viewmodel.LocationFix

/**
 * Live map of the whole delivery round: every customer and shop is pinned, the
 * driver is the blue dot, and tapping a pin opens a slim glass card with the
 * name, whether it is a customer or a shop, and «کردنەوە».
 *
 * The map itself is the app's own [TileMap]: dragging, pinching and tapping all
 * go through one gesture layer, so none of them can be blocked by another.
 */
@Composable
fun MapScreen(
    customers: List<Customer>,
    myLocation: LocationFix?,
    distanceOf: (Customer) -> Double?,
    onOpenRoute: (Customer) -> Unit,
    onOpenCustomer: (Customer) -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val pinned = customers.filter { it.hasCoordinates }
    val closest = pinned
        .mapNotNull { customer -> distanceOf(customer)?.let { customer to it } }
        .minByOrNull { it.second }
        ?.first

    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = pinned.firstOrNull { it.id == selectedId }
    val shown = selected ?: closest

    Box(modifier.fillMaxSize()) {
        TileMap(
            modifier = Modifier.fillMaxSize(),
            center = if (pinned.size == 1) {
                pinned.first().let { MapPoint(it.latitude, it.longitude) }
            } else {
                null
            },
            zoom = 15.0,
            me = myLocation?.let { MapPoint(it.latitude, it.longitude, "شوێنی من") },
            points = pinned.map {
                MapPoint(it.latitude, it.longitude, it.name, it.id, it.kind)
            },
            selectedId = selected?.id,
            allowStyleSwitch = true,
            initialStyle = MapStyle.SATELLITE,
            onLocate = onLocate,
            onPinTap = { point ->
                if (point.id.isNotBlank()) selectedId = point.id
            },
            onTap = { _, _ -> selectedId = null },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 102.dp),
        ) {
            if (shown != null) {
                val isShop = shown.isShop
                val distance = distanceOf(shown)
                GlassCard(padding = 12.dp, radius = 24.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(colors.brandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = TextUtils.initials(shown.name),
                                color = colors.brandDeep,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = shown.name,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassChip(
                                    text = if (isShop) "فرۆشگا" else "کڕیار",
                                    icon = if (isShop) AppIcons.store() else AppIcons.user(),
                                    tint = colors.brandDeep,
                                    container = colors.brandSoft,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = TextUtils.shortDistance(distance),
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (selected != null) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surfaceSunken)
                                    .clickable { selectedId = null },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    AppIcons.close(),
                                    contentDescription = "داخستن",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "کردنەوە",
                            onClick = { onOpenCustomer(shown) },
                            icon = AppIcons.user(),
                            container = colors.brand,
                            contentColor = colors.onBrand,
                            modifier = Modifier.weight(1f),
                            height = 44.dp,
                            fontSize = 13,
                        )
                        PillButton(
                            label = "ڕێگا",
                            onClick = { onOpenRoute(shown) },
                            icon = AppIcons.route(),
                            container = colors.brandSoft,
                            contentColor = colors.brandDeep,
                            modifier = Modifier.width(104.dp),
                            height = 44.dp,
                            fontSize = 13,
                        )
                    }
                }
            } else {
                GlassCard(padding = 14.dp) {
                    Text(
                        text = "هیچ شوێنێکی تۆمارکراو نییە",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "لە کارتی کڕیار شوێن دیاری بکە بۆ ئەوەی لێرە دەربکەوێت.",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
