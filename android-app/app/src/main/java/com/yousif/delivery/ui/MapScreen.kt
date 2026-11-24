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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.viewmodel.LocationFix

/**
 * Live map of the whole delivery round: every customer is pinned, the driver is
 * the blue dot, and the closest stop is offered as a one-tap card.
 */
@Composable
fun MapScreen(
    customers: List<Customer>,
    myLocation: LocationFix?,
    distanceOf: (Customer) -> Double?,
    onOpenRoute: (Customer) -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val pinned = customers.filter { it.hasCoordinates }
    val closest = pinned
        .mapNotNull { customer -> distanceOf(customer)?.let { customer to it } }
        .minByOrNull { it.second }
        ?.first

    Box(modifier.fillMaxSize()) {
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            center = if (pinned.size == 1) {
                pinned.first().let { MapPoint(it.latitude, it.longitude) }
            } else {
                null
            },
            zoom = 15.0,
            me = myLocation?.let { MapPoint(it.latitude, it.longitude, "شوێنی من") },
            points = pinned.map { MapPoint(it.latitude, it.longitude, it.name) },
            allowStyleSwitch = true,
            initialStyle = MapStyle.SATELLITE,
            onLocate = onLocate,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 112.dp),
        ) {
            if (closest != null) {
                AppCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallAvatar(name = closest.name, size = 42)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = closest.name,
                                color = colors.textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            )
                            Text(
                                text = TextUtils.distance(distanceOf(closest)),
                                color = colors.textSecondary,
                                fontSize = 12.5.sp,
                            )
                        }
                        StatPill(
                            text = TextUtils.toKurdish(pinned.size.toString()) + " شوێن",
                            icon = AppIcons.pin(),
                            tint = colors.onBrand,
                            soft = colors.brand,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "کردنەوەی ڕێگا",
                            onClick = { onOpenRoute(closest) },
                            icon = AppIcons.route(),
                            container = colors.brand,
                            contentColor = colors.onBrand,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                AppCard(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .padding(4.dp),
                        ) {
                            Text(
                                text = "هیچ شوێنێکی تۆمارکراو نییە",
                                color = colors.textSecondary,
                                fontSize = 13.5.sp,
                            )
                        }
                    }
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
