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
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.NeonCyan
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
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
 * Dark CARTO street tiles (labels + roads visible) through osmdroid — no API
 * key, no watermark. Tiles are cached on disk by osmdroid, so the map keeps
 * working after the first load even with a weak connection.
 */
private val DarkTiles: XYTileSource by lazy {
    XYTileSource(
        "CartoDark",
        3,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
            "https://d.basemaps.cartocdn.com/dark_all/",
        ),
        "© OpenStreetMap · © CARTO",
    )
}

private val MAP_BACKGROUND = 0xFF0F1622.toInt()

@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: MapPoint? = null,
    zoom: Double = 15.0,
    me: MapPoint? = null,
    customer: MapPoint? = null,
    showControls: Boolean = true,
    onLocate: (() -> Unit)? = null,
    onTap: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(DarkTiles)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            setTilesScaledToDpi(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            overlayManager.tilesOverlay.setLoadingBackgroundColor(MAP_BACKGROUND)
            overlayManager.tilesOverlay.setLoadingLineColor(MAP_BACKGROUND)
            controller.setZoom(zoom)
        }
    }

    val lastCenter = remember { arrayOfNulls<MapPoint>(1) }

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
                val target = center ?: me ?: customer
                if (target != null && lastCenter[0] != target) {
                    lastCenter[0] = target
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
                if (me != null) {
                    view.overlays.add(marker(view, me, me.title.ifBlank { "شوێنی من" }))
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
                MapButton(AppIcons.plus()) {
                    mapView.controller.setZoom(mapView.zoomLevelDouble + 1.0)
                }
                MapButton(AppIcons.minus()) {
                    mapView.controller.setZoom(mapView.zoomLevelDouble - 1.0)
                }
                if (onLocate != null) {
                    MapButton(AppIcons.locate(), tint = NeonCyan, onClick = onLocate)
                }
            }
        }

        Text(
            text = "© OpenStreetMap · © CARTO",
            color = AppTextSecondary.copy(alpha = 0.7f),
            fontSize = 8.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun MapButton(
    icon: ImageVector,
    tint: Color = Color(0xFFE2E8F0),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(Color(0xF2101725))
            .border(1.dp, Color(0x3394A3B8), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

private fun marker(view: MapView, point: MapPoint, title: String): Marker =
    Marker(view).apply {
        position = GeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title
        setInfoWindow(null)
    }
