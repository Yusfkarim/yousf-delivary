package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils

/**
 * Full-screen map picker. It exists separately from the add-customer sheet on
 * purpose: an osmdroid view nested inside a scrolling sheet fights for the drag
 * gesture and locks the UI up. Here the map owns the whole screen, so panning
 * and tapping are smooth.
 */
@Composable
fun LocationPickerDialog(
    initial: MapPoint?,
    me: MapPoint?,
    onRequestGps: ((Double, Double) -> Unit) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
) {
    val colors = LocalAppColors.current
    var showLink by remember { mutableStateOf(false) }
    var picked by remember {
        mutableStateOf(
            initial ?: me,
        )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SoftIconButton(
                    icon = AppIcons.chevronEnd(),
                    onClick = onDismiss,
                    contentDescription = "گەڕانەوە",
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "دیاریکردنی شوێن",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(44.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                TileMap(
                    modifier = Modifier.fillMaxSize(),
                    center = picked,
                    zoom = 16.0,
                    me = me,
                    customer = picked?.let { MapPoint(it.latitude, it.longitude, "شوێنی هەڵبژێردراو") },
                    allowStyleSwitch = true,
                    initialStyle = MapStyle.SATELLITE,
                    onTap = { lat, lng -> picked = MapPoint(lat, lng) },
                    onLocate = me?.let {
                        { picked = MapPoint(it.latitude, it.longitude) }
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color(0x1FA0785A), spotColor = Color(0x1FA0785A))
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.surface)
                    .padding(14.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "لە نەخشە دەست بنێ بۆ هەڵبژاردنی شوێن — یان دوگمەی شوێنی من دابگرە",
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = picked?.let { TextUtils.coordinatesDisplay(it.latitude, it.longitude) } ?: "—",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "شوێنی ئێستام",
                            onClick = {
                                onRequestGps { lat, lng -> picked = MapPoint(lat, lng) }
                            },
                            icon = AppIcons.locate(),
                            container = colors.brandSoft,
                            contentColor = colors.brandDeep,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 13,
                        )
                        PillButton(
                            label = "لینکی نەخشە",
                            onClick = { showLink = true },
                            icon = AppIcons.pin(),
                            container = colors.brandSoft,
                            contentColor = colors.brandDeep,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 13,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "پاشەکەوتکردن",
                            onClick = {
                                picked?.let { onConfirm(it.latitude, it.longitude) }
                            },
                            icon = AppIcons.check(),
                            container = colors.brand,
                            contentColor = colors.onBrand,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                        )
                        PillButton(
                            label = "پاشگەزبوونەوە",
                            onClick = onDismiss,
                            container = colors.surfaceAlt,
                            contentColor = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                        )
                    }
                }
            }
        }

        if (showLink) {
            MapLinkDialog(
                onDismiss = { showLink = false },
                onPick = { lat, lng ->
                    picked = MapPoint(lat, lng)
                    showLink = false
                },
            )
        }
    }
}
