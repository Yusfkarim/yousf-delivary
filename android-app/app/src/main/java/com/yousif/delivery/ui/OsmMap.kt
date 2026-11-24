package com.yousif.delivery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

data class MapPoint(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
)

/**
 * Free OpenStreetMap tiles (Mapnik) through osmdroid — no API key, no CARTO,
 * no "API KEY REQUIRED" watermark. The dark look comes from osmdroid's
 * INVERT_COLORS colour matrix, and tiles are cached on disk by osmdroid.
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: MapPoint? = null,
    zoom: Double = 15.0,
    me: MapPoint? = null,
    customer: MapPoint? = null,
    onTap: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setTilesScaledToDpi(true)
            overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
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

    AndroidView(
        factory = { mapView },
        modifier = modifier,
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
            if (me != null) view.overlays.add(marker(view, me, me.title.ifBlank { "شوێنی من" }))
            if (customer != null) {
                view.overlays.add(marker(view, customer, customer.title.ifBlank { "شوێنی کڕیار" }))
            }
            view.invalidate()
        },
    )
}

private fun marker(view: MapView, point: MapPoint, title: String): Marker =
    Marker(view).apply {
        position = GeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title
        setInfoWindow(null)
    }
