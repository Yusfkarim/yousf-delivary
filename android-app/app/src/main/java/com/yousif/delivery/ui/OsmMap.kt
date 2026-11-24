package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
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

/**
 * OpenStreetMap tiles through osmdroid — no API key, no watermark. The palette
 * follows the app theme: CARTO `light_all` by day, `dark_all` at night, so the
 * map never looks pasted in. Tiles are cached on disk by osmdroid.
 */
private fun tileSource(dark: Boolean): XYTileSource {
    val name = if (dark) "CartoDark" else "CartoLight"
    val style = if (dark) "dark_all" else "light_all"
    return XYTileSource(
        name,
        3,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/$style/",
            "https://b.basemaps.cartocdn.com/$style/",
            "https://c.basemaps.cartocdn.com/$style/",
            "https://d.basemaps.cartocdn.com/$style/",
        ),
        "© OpenStreetMap · © CARTO",
    )
}

private val LightTiles: XYTileSource by lazy { tileSource(false) }
private val DarkTiles: XYTileSource by lazy { tileSource(true) }

@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: MapPoint? = null,
    zoom: Double = 15.0,
    me: MapPoint? = null,
    customer: MapPoint? = null,
    points: List<MapPoint> = emptyList(),
    showControls: Boolean = true,
    onLocate: (() -> Unit)? = null,
    onTap: ((Double, Double) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val source = if (colors.isDark) DarkTiles else LightTiles
    val background = if (colors.isDark) 0xFF15100D.toInt() else 0xFFFFF8F3.toInt()

    val mapView = remember {
        MapView(context).apply {
            setTileSource(source)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            setTilesScaledToDpi(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(zoom)
        }
    }

    val state = remember { MapState() }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (state.source !== source) {
                    state.source = source
                    view.setTileSource(source)
                }
                view.overlayManager.tilesOverlay.setLoadingBackgroundColor(background)
                view.overlayManager.tilesOverlay.setLoadingLineColor(background)

                val target = center ?: me ?: customer
                if (target != null && state.center !== target) {
                    state.center = target
                    view.controller.setZoom(zoom)
                    view.controller.animateTo(GeoPoint(target.latitude, target.longitude))
                }

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
                    view.overlays.add(marker(view, customer, customer.title.ifBlank { "شوێنی کڕیار" }))
                }
                points.forEach { point ->
                    view.overlays.add(marker(view, point, point.title))
                }
                if (me != null) {
                    view.overlays.add(marker(view, me, me.title.ifBlank { "شوێنی من" }))
                }

                // Frame every customer once, so the whole delivery round is visible.
                if (target == null && !state.fitted && points.size > 1) {
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
                view.invalidate()
            },
        )

        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(AppIcons.plus(), colors.isDark) {
                    mapView.controller.setZoom(mapView.zoomLevelDouble + 1.0)
                }
                MapButton(AppIcons.minus(), colors.isDark) {
                    mapView.controller.setZoom(mapView.zoomLevelDouble - 1.0)
                }
                if (onLocate != null) {
                    MapButton(AppIcons.locate(), colors.isDark, tint = colors.brand, onClick = onLocate)
                }
            }
        }

        Text(
            text = "© OpenStreetMap · © CARTO",
            color = if (colors.isDark) Color(0xFF9C8A7C) else Color(0xFF9C8A7C),
            fontSize = 8.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
    }
}

private class MapState {
    var source: XYTileSource? = null
    var center: MapPoint? = null
    var fitted = false
}

@Composable
private fun MapButton(
    icon: ImageVector,
    dark: Boolean,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, shape, clip = false)
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
            modifier = Modifier.size(19.dp),
        )
    }
}

private fun marker(view: MapView, point: MapPoint, title: String): Marker =
    Marker(view).apply {
        position = GeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title
        setInfoWindow(null)
    }
