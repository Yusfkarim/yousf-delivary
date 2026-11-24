package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yousif.delivery.R
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.map.TileSources
import com.yousif.delivery.ui.theme.LocalAppColors
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

data class MapPoint(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
)

private enum class TileState { LOADING, READY, FAILED }

/**
 * OpenStreetMap through osmdroid — no API key, no watermark, tiles cached on the
 * phone for offline use.
 *
 * Two rules keep it from ever freezing or going blank:
 *  1. a brand-new [MapView] every time the composable enters (re-using a detached
 *     view is what made the map render as an empty cream rectangle);
 *  2. overlays/centre only change when the values really change, so scrolling a
 *     parent never rebuilds the map on every frame.
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: MapPoint? = null,
    zoom: Double = 15.0,
    me: MapPoint? = null,
    customer: MapPoint? = null,
    points: List<MapPoint> = emptyList(),
    showControls: Boolean = true,
    interactive: Boolean = true,
    allowStyleSwitch: Boolean = false,
    initialStyle: MapStyle = MapStyle.STREET,
    onLocate: (() -> Unit)? = null,
    onTap: ((Double, Double) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    var style by remember { mutableStateOf(initialStyle) }
    var source by remember { mutableStateOf<OnlineTileSourceBase>(TileSources.forStyle(initialStyle, colors.isDark)) }
    var tileState by remember { mutableStateOf(TileState.LOADING) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val state = remember { MapState() }
    val background = if (colors.isDark) 0xFF15100D.toInt() else 0xFFFFF8F3.toInt()

    // Pick a provider that actually answers from this phone / ISP.
    LaunchedEffect(Unit) {
        val ready = TileSources.ensureReady()
        tileState = if (ready) TileState.READY else TileState.FAILED
        source = TileSources.forStyle(style, colors.isDark)
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(source)
                    setMultiTouchControls(interactive)
                    setUseDataConnection(true)
                    setTilesScaledToDpi(true)
                    setBuiltInZoomControls(false)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    controller.setZoom(zoom)
                    if (!interactive) {
                        setOnTouchListener { _, _ -> true }
                    }
                    onResume()
                    mapView = this
                }
            },
            update = { view ->
                if (state.source !== source) {
                    state.source = source
                    view.setTileSource(source)
                    view.tileProvider.clearTileCache()
                    view.invalidate()
                }
                view.overlayManager.tilesOverlay.setLoadingBackgroundColor(background)
                view.overlayManager.tilesOverlay.setLoadingLineColor(background)

                // Explicit centre wins; otherwise frame every pin; a lone GPS fix
                // is only used when the map has nothing else to show.
                val target = center ?: if (points.isEmpty()) (me ?: customer) else null
                if (target != null) {
                    val key = "${target.latitude}/${target.longitude}/$zoom"
                    if (state.centerKey != key) {
                        state.centerKey = key
                        view.controller.setZoom(zoom)
                        view.controller.setCenter(GeoPoint(target.latitude, target.longitude))
                    }
                }

                val signature = buildString {
                    append(customer?.latitude).append(',').append(customer?.longitude).append('|')
                    append(me?.latitude).append(',').append(me?.longitude).append('|')
                    points.forEach { point -> append(point.latitude).append(':').append(point.longitude).append(';') }
                    append('|').append(onTap != null)
                }

                if (state.signature != signature) {
                    state.signature = signature
                    view.overlays.clear()
                    if (onTap != null) {
                        view.overlays.add(
                            MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    onTap(p.latitude, p.longitude)
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint): Boolean = false
                            }),
                        )
                    }
                    if (customer != null) {
                        view.overlays.add(
                            marker(view, customer, customer.title.ifBlank { "شوێنی هەڵبژێردراو" }, R.drawable.ic_map_pin),
                        )
                    }
                    points.forEach { point ->
                        view.overlays.add(marker(view, point, point.title, R.drawable.ic_map_pin))
                    }
                    if (me != null) {
                        view.overlays.add(
                            marker(view, me, me.title.ifBlank { "شوێنی من" }, R.drawable.ic_map_me),
                        )
                    }
                    view.invalidate()
                }

                if (center == null && points.size == 1 && state.centerKey == null) {
                    val only = points.first()
                    state.centerKey = "single/${only.latitude}/${only.longitude}/$zoom"
                    view.controller.setZoom(zoom)
                    view.controller.setCenter(GeoPoint(only.latitude, only.longitude))
                }

                // Frame every stop once, so the whole round is visible.
                if (center == null && target == null && !state.fitted && points.size > 1) {
                    state.fitted = true
                    val latitudes = points.map { it.latitude }
                    val longitudes = points.map { it.longitude }
                    val box = BoundingBox(
                        latitudes.maxOrNull() ?: 0.0,
                        longitudes.maxOrNull() ?: 0.0,
                        latitudes.minOrNull() ?: 0.0,
                        longitudes.minOrNull() ?: 0.0,
                    )
                    view.zoomToBoundingBox(box, false, 90)
                }
            },
            onRelease = { view ->
                view.onPause()
                view.onDetach()
                mapView = null
            },
        )

        if (showControls && interactive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                MapButton(AppIcons.plus(), colors.isDark) {
                    mapView?.controller?.setZoom((mapView?.zoomLevelDouble ?: zoom) + 1.0)
                }
                MapButton(AppIcons.minus(), colors.isDark) {
                    mapView?.controller?.setZoom((mapView?.zoomLevelDouble ?: zoom) - 1.0)
                }
                if (onLocate != null) {
                    MapButton(AppIcons.locate(), colors.isDark, tint = colors.brand, onClick = onLocate)
                }
                if (allowStyleSwitch) {
                    MapButton(
                        icon = when (style) {
                            MapStyle.DARK -> AppIcons.moon()
                            MapStyle.SATELLITE -> AppIcons.sun()
                            MapStyle.STREET -> AppIcons.map()
                        },
                        dark = colors.isDark,
                        tint = colors.brandDeep,
                    ) {
                        style = when (style) {
                            MapStyle.STREET -> MapStyle.SATELLITE
                            MapStyle.SATELLITE -> MapStyle.DARK
                            MapStyle.DARK -> MapStyle.STREET
                        }
                        source = TileSources.forStyle(style, colors.isDark)
                    }
                }
            }
        }

        if (tileState == TileState.FAILED) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface)
                    .border(1.dp, colors.danger.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.cloudWait(),
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "نەخشە نەهات — ئینتەرنێت بپشکنە",
                    color = colors.danger,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = TileSources.attribution(style, colors.isDark),
            color = if (colors.isDark) Color(0xFF9C8A7C) else Color(0xFF8B7768),
            fontSize = 8.5.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
    }
}

private class MapState {
    var source: OnlineTileSourceBase? = null
    var centerKey: String? = null
    var signature: String? = null
    var fitted = false
}

@Composable
private fun MapButton(
    icon: ImageVector,
    dark: Boolean,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .background(if (dark) Color(0xFFE6201A15) else Color(0xFFFFFFFF))
            .border(1.dp, if (dark) Color(0xFF3A2D24) else Color(0xFFF2E2D6), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint ?: if (dark) Color(0xFFF0E7DF) else Color(0xFF2A1A11),
            modifier = Modifier.size(23.dp),
        )
    }
}

private fun marker(view: MapView, point: MapPoint, title: String, iconRes: Int): Marker =
    Marker(view).apply {
        position = GeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        val drawable = ContextCompat.getDrawable(view.context, iconRes)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        icon = drawable
        this.title = title
        setInfoWindow(null)
    }
