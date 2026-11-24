package com.yousif.delivery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
 * driver is the blue dot, and tapping a pin opens a card with the name, whether
 * it is a customer or a shop, and «کردنەوە».
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
            allowStyleSwitch = true,
            initialStyle = MapStyle.SATELLITE,
            onLocate = onLocate,
            onPinTap = { point ->
                if (point.id.isNotBlank()) selectedId = point.id
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 112.dp),
        ) {
            if (shown != null) {
                val isShop = shown.isShop
                GlassCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallAvatar(name = shown.name, size = 44)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = shown.name,
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassChip(
                                    text = if (isShop) "فرۆشگا" else "کڕیار",
                                    icon = if (isShop) AppIcons.store() else AppIcons.user(),
                                    tint = colors.brandDeep,
                                    container = colors.brandSoft,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = TextUtils.distance(distanceOf(shown)),
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        Text(
                            text = TextUtils.toKurdish(pinned.size.toString()) + "\nشوێن",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "کردنەوە",
                            onClick = { onOpenCustomer(shown) },
                            icon = AppIcons.user(),
                            container = colors.brand,
                            contentColor = colors.onBrand,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 14,
                        )
                        PillButton(
                            label = "ڕێگا",
                            onClick = { onOpenRoute(shown) },
                            icon = AppIcons.route(),
                            container = colors.brandSoft,
                            contentColor = colors.brandDeep,
                            modifier = Modifier.width(116.dp),
                            height = 48.dp,
                            fontSize = 13,
                        )
                    }
                    if (selected == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "نزیکترین شوێن · بۆ بینینی کەسێکی تر لە نەخشە پینەکەی دابگرە",
                            color = colors.textMuted,
                            fontSize = 10.5.sp,
                        )
                    }
                }
            } else {
                GlassCard(padding = 16.dp) {
                    Text(
                        text = "هیچ شوێنێکی تۆمارکراو نییە",
                        color = colors.textSecondary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
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
