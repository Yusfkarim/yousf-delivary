package com.yousif.delivery.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.ClipboardUtils
import com.yousif.delivery.util.MapLinkParser
import com.yousif.delivery.util.MapLinkResolver
import com.yousif.delivery.util.TextUtils
import kotlinx.coroutines.launch

/**
 * The second, explicit way to set a location: paste a Google Maps link and the
 * coordinates are extracted from it. Kept separate from "شوێنی ئێستام" (GPS) so
 * the two can never be confused with each other.
 */
@Composable
fun MapLinkDialog(
    initialText: String = "",
    onDismiss: () -> Unit,
    onPick: (Double, Double) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(initialText) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun extract(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) {
            status = "لینکەکە بەتاڵە"
            return
        }
        scope.launch {
            busy = true
            status = "لینک دەخوێندرێتەوە…"
            val parsed = MapLinkParser.parse(value) ?: MapLinkResolver.resolve(value)
            busy = false
            result = parsed
            status = if (parsed != null) {
                "شوێن دۆزرایەوە ✓"
            } else {
                "شوێن نەدۆزرایەوە — دڵنیابە لینکەکە تەواوە"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.brandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        AppIcons.pin(),
                        contentDescription = null,
                        tint = colors.brandDeep,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "لینکی نەخشە",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "لینکی Google Maps بلکێنە — شوێن خۆکار دەردەهێنرێت",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    text = value
                    status = null
                    result = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("https://maps.app.goo.gl/…", fontSize = 13.sp, color = colors.textMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(fontSize = 13.5.sp, color = colors.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceSunken,
                    unfocusedContainerColor = colors.surfaceSunken,
                    focusedBorderColor = colors.brand,
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.brand,
                ),
            )

            Spacer(Modifier.height(10.dp))

            val searchIcon = if (busy) null else AppIcons.search()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label = if (busy) "…" else "لێکدانەوە",
                    onClick = { extract(text) },
                    icon = searchIcon,
                    container = colors.brand,
                    contentColor = colors.onBrand,
                    modifier = Modifier.weight(1f),
                    height = 46.dp,
                    fontSize = 13,
                )
                PillButton(
                    label = "کلیپبۆرد",
                    onClick = {
                        val clip = ClipboardUtils.text(context)
                        if (clip != null) {
                            text = clip
                            extract(clip)
                        } else {
                            status = "کلیپبۆرد بەتاڵە"
                        }
                    },
                    icon = AppIcons.copy(),
                    container = colors.brandSoft,
                    contentColor = colors.brandDeep,
                    modifier = Modifier.weight(1f),
                    height = 46.dp,
                    fontSize = 13,
                )
            }

            status?.let { message ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.brand,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = message,
                        color = if (message.contains("✓")) colors.successDeep else colors.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            val point = result
            if (point != null) {
                Spacer(Modifier.height(12.dp))
                MapThumb(
                    latitude = point.first,
                    longitude = point.second,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = TextUtils.coordinatesDisplay(point.first, point.second),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label = "بەکارهێنان",
                    onClick = {
                        val applied = result
                        if (applied != null) onPick(applied.first, applied.second)
                    },
                    icon = AppIcons.check(),
                    container = if (result != null) colors.brand else colors.surfaceAlt,
                    contentColor = if (result != null) colors.onBrand else colors.textMuted,
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                    fontSize = 14,
                )
                PillButton(
                    label = "پاشگەزبوونەوە",
                    onClick = onDismiss,
                    container = colors.surfaceAlt,
                    contentColor = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                    fontSize = 13,
                )
            }
        }
    }
}
