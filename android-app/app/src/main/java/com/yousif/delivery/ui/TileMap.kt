package com.yousif.delivery.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Kinds
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.map.TileSources
import com.yousif.delivery.ui.theme.AppColors
import com.yousif.delivery.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** A place to draw on the map. */
data class MapPoint(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val id: String = "",
    val kind: String = "",
)

private const val TILE_PX = 256
private const val MIN_ZOOM = 4.0
private const val MAX_ZOOM = 18.5
private const val FALLBACK_LAT = 35.1143044
private const val FALLBACK_LNG = 45.6829985

/**
 * The map of the whole app, drawn by the app itself.
 *
 * Why it never «jams» and never repeats a column:
 *  * **one** Canvas pass paints every tile from one geometry snapshot, so a tile
 *    slot can never be reused or drift by half a tile;
 *  * the centre/zoom live in plain state that only the *draw* phase reads, so a
 *    drag repaints pixels instead of recomposing a composable tree;
 *  * pan/pinch use a single gesture layer (transform + tap on the same node), so
 *    the finger can never be «eaten» by an invisible overlay;
 *  * tiles load from one small pump that skips what is already on screen.
 * Nothing here is an `android.view`, so the section can never block the UI.
 */
@Composable
fun TileMap(
    modifier: Modifier = Modifier,
    center: MapPoint? = null,
    zoom: Double = 15.0,
    me: MapPoint? = null,
    customer: MapPoint? = null,
    points: List<MapPoint> = emptyList(),
    selectedId: String? = null,
    interactive: Boolean = true,
    showControls: Boolean = true,
    allowStyleSwitch: Boolean = false,
    allowOfflineDownload: Boolean = false,
    initialStyle: MapStyle = MapStyle.STREET,
    onLocate: (() -> Unit)? = null,
    onTap: ((Double, Double) -> Unit)? = null,
    onPinTap: ((MapPoint) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    remember(context) { TileCache.ensure(context) }

    var style by remember { mutableStateOf(initialStyle) }

    val pins = remember(customer, points, me) {
        val list = mutableListOf<MapPoint>()
        customer?.let { list.add(it) }
        list.addAll(points)
        cleanPins(list)
    }

    val state = remember {
        val seed = center ?: pins.firstOrNull() ?: me
        MapState(
            initialLatitude = seed?.latitude ?: FALLBACK_LAT,
            initialLongitude = seed?.longitude ?: FALLBACK_LNG,
            initialZoom = zoom,
        )
    }

    // A new centre from the caller (e.g. the location picker) moves the map.
    LaunchedEffect(center?.latitude, center?.longitude) {
        val target = center ?: return@LaunchedEffect
        state.moveTo(target.latitude, target.longitude)
        state.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    val tapHandler by rememberUpdatedState(onTap)
    val pinHandler by rememberUpdatedState(onPinTap)

    val placeholder = if (colors.isDark) Color(0xFF17120E) else Color(0xFFE4DBD1)

    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    BoxWithConstraints(modifier.background(placeholder)) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        var loaded by remember { mutableStateOf(false) }

        // Every saved pin visible at once, the first time the map opens.
        LaunchedEffect(widthPx, heightPx, pins.size, center) {
            if (center != null || pins.size < 2 || widthPx <= 0 || heightPx <= 0) return@LaunchedEffect
            val fit = fitZoom(pins, widthPx, heightPx)
            state.zoom = fit.zoom
            state.moveTo(fit.latitude, fit.longitude)
        }

        // One lightweight pump: the coarse base layer plus the exact tiles, no more.
        LaunchedEffect(style, colors.isDark, widthPx, heightPx) {
            while (true) {
                if (widthPx > 0 && heightPx > 0) {
                    val geometry = state.geometry(widthPx, heightPx)
                    val layers = listOf(
                        (geometry.tileZoom - 2).coerceAtLeast(2) to geometry.tileSize * 4.0,
                        (geometry.tileZoom - 1).coerceAtLeast(2) to geometry.tileSize * 2.0,
                        geometry.tileZoom to geometry.tileSize,
                    )
                    for ((zoom, sizePx) in layers) {
                        val count = 1 shl zoom
                        val firstX = floor(geometry.leftWorld / sizePx).toInt() - 1
                        val lastX = ceil((geometry.leftWorld + widthPx) / sizePx).toInt() + 1
                        val firstY = floor(geometry.topWorld / sizePx).toInt() - 1
                        val lastY = ceil((geometry.topWorld + heightPx) / sizePx).toInt() + 1
                        for (tileY in firstY..lastY) {
                            if (tileY < 0 || tileY >= count) continue
                            for (tileX in firstX..lastX) {
                                val wrapped = ((tileX % count) + count) % count
                                val urls = TileSources.urlsFor(style, colors.isDark, zoom, wrapped, tileY)
                                for ((index, url) in urls.withIndex()) {
                                    if (TileCache.cached(url) != null) {
                                        loaded = true
                                        break
                                    }
                                    // Only ask the backup provider once the first one
                                    // has actually failed — no double downloads.
                                    if (index > 0 && !TileCache.coolingDown(urls[index - 1])) break
                                    if (!TileCache.claim(url)) break
                                    launch(Dispatchers.IO) {
                                        try {
                                            if (TileCache.load(url) != null) loaded = true
                                        } finally {
                                            TileCache.release(url)
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
                delay(70)
            }
        }

        Canvas(Modifier.fillMaxSize().clipToBounds()) {
            val geometry = state.geometry(widthPx, heightPx)
            drawRect(placeholder)
            // Reading the stamp is what makes a freshly downloaded tile appear.
            if (TileCache.stamp >= 0) {
                drawTiles(geometry, style, colors.isDark)
            }
            drawPins(geometry, pins, me, selectedId, colors)
        }

        if (interactive || onTap != null || onPinTap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, widthPx, heightPx, interactive) {
                        if (!interactive) return@pointerInput

                        fun zoomAround(factor: Double, focusX: Float, focusY: Float) {
                            if (widthPx <= 0 || heightPx <= 0) return
                            val before = state.geometry(widthPx, heightPx)
                            val fixedLat = before.latitudeAt(focusY)
                            val fixedLng = before.longitudeAt(focusX)
                            state.zoom = (state.zoom + ln(factor) / ln(2.0)).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            val after = state.geometry(widthPx, heightPx)
                            state.moveTo(
                                latitude = after.latitudeAt(heightPx / 2f + (after.yOf(fixedLat) - focusY)),
                                longitude = after.longitudeAt(widthPx / 2f + (after.xOf(fixedLng) - focusX)),
                            )
                        }

                        detectTransformGestures(panZoomLock = false) { centroid, pan, gestureZoom, _ ->
                            if (gestureZoom != 1f) zoomAround(gestureZoom.toDouble(), centroid.x, centroid.y)
                            if (pan.x != 0f || pan.y != 0f) {
                                val geometry = state.geometry(widthPx, heightPx)
                                state.moveTo(
                                    latitude = geometry.latitudeAt(heightPx / 2f - pan.y),
                                    longitude = geometry.longitudeAt(widthPx / 2f - pan.x),
                                )
                            }
                        }
                    }
                    .pointerInput(state, widthPx, heightPx, pins) {
                        detectTapGestures(
                            onTap = { offset ->
                                val geometry = state.geometry(widthPx, heightPx)
                                val hit = nearestPin(geometry, pins, offset.x, offset.y, 26.dp.toPx())
                                if (hit != null && pinHandler != null) {
                                    pinHandler?.invoke(hit)
                                } else {
                                    tapHandler?.invoke(
                                        geometry.latitudeAt(offset.y),
                                        geometry.longitudeAt(offset.x),
                                    )
                                }
                            },
                        )
                    },
            )
        }

        if (showControls && interactive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                MapControl(AppIcons.plus(), colors.isDark) {
                    state.zoom = (state.zoom + 1.0).coerceIn(MIN_ZOOM, MAX_ZOOM)
                }
                MapControl(AppIcons.minus(), colors.isDark) {
                    state.zoom = (state.zoom - 1.0).coerceIn(MIN_ZOOM, MAX_ZOOM)
                }
                if (pins.size > 1 && widthPx > 0 && heightPx > 0) {
                    MapControl(AppIcons.fit(), colors.isDark) {
                        val fit = fitZoom(pins, widthPx, heightPx)
                        state.zoom = fit.zoom
                        state.moveTo(fit.latitude, fit.longitude)
                    }
                }
                if (onLocate != null) {
                    MapControl(AppIcons.locate(), colors.isDark, tint = colors.brand, onClick = onLocate)
                }
                if (allowOfflineDownload) {
                    MapControl(
                        icon = AppIcons.download(),
                        dark = colors.isDark,
                        tint = colors.brandDeep,
                    ) {
                        if (!downloading && widthPx > 0 && heightPx > 0) {
                            val geometry = state.geometry(widthPx, heightPx)
                            // A little more than the screen, so a small pan still
                            // stays inside the saved area.
                            val padX = widthPx * 0.25f
                            val padY = heightPx * 0.25f
                            val west = geometry.longitudeAt(-padX)
                            val east = geometry.longitudeAt(widthPx + padX)
                            val north = geometry.latitudeAt(-padY)
                            val south = geometry.latitudeAt(heightPx + padY)
                            downloading = true
                            downloadProgress = 0f
                            scope.launch {
                                val saved = TileCache.downloadArea(
                                    style = style,
                                    dark = colors.isDark,
                                    zooms = (geometry.tileZoom - 1)..(geometry.tileZoom + 2),
                                    minLatitude = south,
                                    minLongitude = west,
                                    maxLatitude = north,
                                    maxLongitude = east,
                                    onProgress = { value -> downloadProgress = value },
                                )
                                downloading = false
                                downloadProgress = 1f
                                Toast.makeText(
                                    context,
                                    "ناوچەکە بۆ ئۆفلاین پاشەکەوتکرا ✓ ($saved تایڵ)",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }
                if (allowStyleSwitch) {
                    MapControl(
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
                    }
                }
            }
        }

        if (downloading) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface.copy(alpha = 0.94f))
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.download(),
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "داگرتنی ناوچە " + (downloadProgress * 100).roundToInt() + "٪",
                    color = colors.textPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (!loaded) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.cloudWait(),
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "نەخشە دێت…",
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = TileSources.attribution(style, colors.isDark),
            color = if (colors.isDark) Color(0xFFC9B9AC) else Color(0xFF6E5C4E),
            fontSize = 8.5.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
    }
}

/* ------------------------------------------------------------------ state */

/** Centre + zoom. Only the draw phase and the gestures read it, never layout. */
private class MapState(
    initialLatitude: Double,
    initialLongitude: Double,
    initialZoom: Double,
) {
    var latitude by mutableStateOf(initialLatitude)
    var longitude by mutableStateOf(initialLongitude)
    var zoom by mutableStateOf(initialZoom)

    fun geometry(widthPx: Int, heightPx: Int): MapGeometry =
        MapGeometry(latitude, longitude, zoom, widthPx, heightPx)

    fun moveTo(latitude: Double, longitude: Double) {
        this.latitude = latitude.coerceIn(-85.0, 85.0)
        this.longitude = wrapLongitude(longitude)
    }
}

/**
 * Everything needed to place a tile or a pin: the visible window expressed in
 * world pixels of a continuous zoom, plus the whole-tile grid it is drawn from.
 */
private class MapGeometry(
    centerLatitude: Double,
    centerLongitude: Double,
    val zoom: Double,
    widthPx: Int,
    heightPx: Int,
) {
    val worldPx: Double = TILE_PX * 2.0.pow(zoom)
    val leftWorld: Double = worldX(centerLongitude) - widthPx / 2.0
    val topWorld: Double = worldY(centerLatitude) - heightPx / 2.0

    /** Tiles come from the nearest whole zoom and are scaled by this size. */
    val tileZoom: Int = zoom.roundToInt().coerceIn(2, 19)
    val tileSize: Double = TILE_PX * 2.0.pow(zoom - tileZoom)

    private fun worldX(longitude: Double): Double = (longitude + 180.0) / 360.0 * worldPx

    private fun worldY(latitude: Double): Double {
        val rad = Math.toRadians(latitude.coerceIn(-85.0, 85.0))
        return (1.0 - ln(Math.tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * worldPx
    }

    fun xOf(longitude: Double): Float = (worldX(longitude) - leftWorld).toFloat()
    fun yOf(latitude: Double): Float = (worldY(latitude) - topWorld).toFloat()

    fun longitudeAt(x: Float): Double = (leftWorld + x) / worldPx * 360.0 - 180.0

    fun latitudeAt(y: Float): Double {
        val n = Math.PI * (1.0 - 2.0 * (topWorld + y) / worldPx)
        return Math.toDegrees(kotlin.math.atan(0.5 * (Math.exp(n) - Math.exp(-n))))
    }
}

/* ------------------------------------------------------------------ paint */

private fun DrawScope.drawTiles(geometry: MapGeometry, style: MapStyle, dark: Boolean) {
    // A coarse layer two zoom levels out always exists first, so panning or
    // zooming never leaves a blank cream square: the smooth parent tile is
    // stretched over the area, then the sharp tile is painted on top of it.
    drawTileLayer(
        geometry = geometry,
        style = style,
        dark = dark,
        zoom = (geometry.tileZoom - 2).coerceAtLeast(2),
        sizePx = geometry.tileSize * 4.0,
    )
    drawTileLayer(
        geometry = geometry,
        style = style,
        dark = dark,
        zoom = geometry.tileZoom,
        sizePx = geometry.tileSize,
    )
}

private fun DrawScope.drawTileLayer(
    geometry: MapGeometry,
    style: MapStyle,
    dark: Boolean,
    zoom: Int,
    sizePx: Double,
) {
    val count = 1 shl zoom
    val firstX = floor(geometry.leftWorld / sizePx).toInt()
    val lastX = ceil((geometry.leftWorld + size.width) / sizePx).toInt()
    val firstY = floor(geometry.topWorld / sizePx).toInt()
    val lastY = ceil((geometry.topWorld + size.height) / sizePx).toInt()
    val drawSize = sizePx.roundToInt() + 1

    for (tileY in firstY..lastY) {
        if (tileY < 0 || tileY >= count) continue
        for (tileX in firstX..lastX) {
            val wrapped = ((tileX % count) + count) % count
            val dst = IntOffset(
                (tileX * sizePx - geometry.leftWorld).roundToInt(),
                (tileY * sizePx - geometry.topWorld).roundToInt(),
            )
            val bitmap = firstCached(style, dark, zoom, wrapped, tileY)
            if (bitmap != null) {
                drawImage(image = bitmap, dstOffset = dst, dstSize = IntSize(drawSize, drawSize))
                continue
            }
            // Nothing at this zoom yet: paint the matching piece of an already
            // cached parent tile instead of leaving a blank square on the map.
            val ancestor = ancestorTile(style, dark, zoom, wrapped, tileY) ?: continue
            val divisions = 1 shl ancestor.level
            val sourceSize = ancestor.image.width / divisions
            if (sourceSize <= 0) continue
            drawImage(
                image = ancestor.image,
                srcOffset = IntOffset(
                    (wrapped % divisions) * sourceSize,
                    (tileY % divisions) * sourceSize,
                ),
                srcSize = IntSize(sourceSize, sourceSize),
                dstOffset = dst,
                dstSize = IntSize(drawSize, drawSize),
            )
        }
    }
}

/** A cached parent tile plus how many zoom levels above the missing one it sits. */
private class AncestorTile(val image: ImageBitmap, val level: Int)

/**
 * Walks up to four zoom levels looking for any cached tile that covers the
 * missing one. Cheap (a few map lookups) and it runs only for a hole.
 */
private fun ancestorTile(
    style: MapStyle,
    dark: Boolean,
    zoom: Int,
    x: Int,
    y: Int,
): AncestorTile? {
    for (level in 1..4) {
        val parentZoom = zoom - level
        if (parentZoom < 2) return null
        val parentX = x shr level
        val parentY = y shr level
        val image = firstCached(style, dark, parentZoom, parentX, parentY) ?: continue
        return AncestorTile(image, level)
    }
    return null
}

/** The first provider that already has this tile in memory. */
private fun firstCached(
    style: MapStyle,
    dark: Boolean,
    zoom: Int,
    x: Int,
    y: Int,
): ImageBitmap? {
    TileSources.urlsFor(style, dark, zoom, x, y).forEach { url ->
        TileCache.cached(url)?.let { return it }
    }
    return null
}

private fun DrawScope.drawPins(
    geometry: MapGeometry,
    pins: List<MapPoint>,
    me: MapPoint?,
    selectedId: String?,
    colors: AppColors,
) {
    val normalHeight = 26.dp.toPx()
    val selectedHeight = 36.dp.toPx()

    pins.forEach { pin ->
        val x = geometry.xOf(pin.longitude)
        val y = geometry.yOf(pin.latitude)
        if (x < -90f || x > size.width + 90f || y < -90f || y > size.height + 90f) return@forEach
        val selected = pin.id.isNotEmpty() && pin.id == selectedId
        val height = if (selected) selectedHeight else normalHeight
        val width = height * 0.80f
        val fill = if (pin.kind == Kinds.SHOP) colors.brandDeep else colors.brand
        if (selected) {
            drawCircle(
                color = colors.brand.copy(alpha = 0.16f),
                radius = height * 0.95f,
                center = Offset(x, y - height * 0.55f),
            )
        }
        drawPin(x, y, width, height, fill, selected)
    }

    me?.let { driver ->
        val x = geometry.xOf(driver.longitude)
        val y = geometry.yOf(driver.latitude)
        if (x < -60f || x > size.width + 60f || y < -60f || y > size.height + 60f) return@let
        val radius = 8.dp.toPx()
        val center = Offset(x, y)
        drawCircle(color = Color(0x332F6FED), radius = radius * 2.6f, center = center)
        drawCircle(color = Color.White, radius = radius + 2.5.dp.toPx(), center = center)
        drawCircle(color = Color(0xFF2F6FED), radius = radius, center = center)
    }
}

/** One hand-drawn teardrop pin — identical shape at every zoom, always crisp. */
private fun DrawScope.drawPin(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    fill: Color,
    selected: Boolean,
) {
    val radius = width / 2f
    val circleY = y - height + radius
    val leftAngle = Math.toRadians(172.0)
    val shoulderY = circleY + radius * Math.sin(leftAngle).toFloat()
    val leftX = x + radius * Math.cos(leftAngle).toFloat()

    val path = Path().apply {
        moveTo(x, y)
        cubicTo(
            x - radius * 0.30f, y - height * 0.38f,
            x - radius * 0.92f, circleY + radius * 0.78f,
            leftX, shoulderY,
        )
        arcTo(Rect(x - radius, circleY - radius, x + radius, circleY + radius), 172f, 196f, false)
        cubicTo(
            x + radius * 0.92f, circleY + radius * 0.78f,
            x + radius * 0.30f, y - height * 0.38f,
            x, y,
        )
        close()
    }

    translate(top = 2.dp.toPx()) {
        drawPath(path, color = Color(0x26000000))
    }
    drawPath(path, color = fill)
    drawPath(
        path,
        color = Color.White,
        style = Stroke(width = if (selected) 2.4.dp.toPx() else 1.6.dp.toPx()),
    )
    drawCircle(color = Color.White, radius = radius * 0.33f, center = Offset(x, circleY))
}

/* ------------------------------------------------------------------ helpers */

private fun nearestPin(
    geometry: MapGeometry,
    pins: List<MapPoint>,
    tapX: Float,
    tapY: Float,
    radiusPx: Float,
): MapPoint? {
    var best: MapPoint? = null
    var bestDistance = radiusPx
    // The head of the pin sits above the exact coordinate, so compare there.
    val headOffset = radiusPx * 0.58f
    pins.forEach { pin ->
        val x = geometry.xOf(pin.longitude)
        val y = geometry.yOf(pin.latitude) - headOffset
        val distance = hypot(tapX - x, tapY - y)
        if (distance < bestDistance) {
            bestDistance = distance
            best = pin
        }
    }
    return best
}

private class FitResult(val zoom: Double, val latitude: Double, val longitude: Double)

/** Zoom + centre that keep every pin inside 74 % of the screen. */
private fun fitZoom(pins: List<MapPoint>, widthPx: Int, heightPx: Int): FitResult {
    val valid = pins.filter { abs(it.latitude) <= 85.0 && abs(it.longitude) <= 180.0 }
    if (valid.isEmpty() || widthPx <= 0 || heightPx <= 0) {
        return FitResult(15.0, FALLBACK_LAT, FALLBACK_LNG)
    }
    val minLat = valid.minOf { it.latitude }
    val maxLat = valid.maxOf { it.latitude }
    val minLng = valid.minOf { it.longitude }
    val maxLng = valid.maxOf { it.longitude }
    val latitude = (minLat + maxLat) / 2.0
    val longitude = (minLng + maxLng) / 2.0

    val xSpan = (maxLng - minLng) / 360.0
    val ySpan = Math.abs(normalisedY(minLat) - normalisedY(maxLat))
    val limit = TILE_PX * 2.0.pow(18.0)
    val worldForX = if (xSpan > 1e-9) widthPx * 0.74 / xSpan else limit
    val worldForY = if (ySpan > 1e-9) heightPx * 0.74 / ySpan else limit
    val world = minOf(worldForX, worldForY, limit).coerceAtLeast(TILE_PX * 2.0.pow(4.0))
    val zoom = (ln(world / TILE_PX) / ln(2.0)).coerceIn(4.5, 16.5)
    return FitResult(zoom, latitude, longitude)
}

/** Mercator Y in 0..1 for a latitude (zoom independent). */
private fun normalisedY(latitude: Double): Double {
    val rad = Math.toRadians(latitude.coerceIn(-85.0, 85.0))
    return (1.0 - ln(Math.tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0
}

@Composable
private fun MapControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    dark: Boolean,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .size(46.dp)
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
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Tiles in memory (fast panning) and on disk (the map opens instantly the second
 * time), both bounded so a long drive cannot grow the app's memory.
 *
 * `stamp` is a Compose state: every finished download bumps it, and the Canvas
 * reads it to repaint — no bitmap map, no recomposition of the whole map.
 */
/**
 * Tiles in three layers: memory, **persistent disk** and the network.
 *
 * The disk store lives in `filesDir/tiles` (not `cacheDir`, which Android may
 * clear) and keeps up to 400 MB, so an area the driver has already seen stays
 * available with no internet at all — and panning or zooming shows imagery
 * immediately instead of a blank cream rectangle.
 */
private object TileCache {

    private const val MEMORY_LIMIT = 220
    private const val DISK_LIMIT_BYTES = 400L * 1024 * 1024
    private const val DISK_PRUNE_FACTOR = 0.85

    private val store = object : LinkedHashMap<String, ImageBitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean =
            size > MEMORY_LIMIT
    }

    private val inFlight = HashSet<String>()
    private val failures = HashMap<String, Long>()

    @Volatile
    private var dir: File? = null

    private var writes = 0

    var stamp by mutableStateOf(0)
        private set

    @Volatile
    private var client: OkHttpClient? = null

    fun ensure(context: android.content.Context) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            // The HTTP cache is only a fast path; the tile store below is what
            // makes the map work offline.
            val httpCache = File(context.cacheDir, "tile-http")
            runCatching { httpCache.mkdirs() }
            client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .cache(Cache(httpCache, 48L * 1024 * 1024))
                .build()
            val tiles = File(context.filesDir, "tiles")
            runCatching { tiles.mkdirs() }
            dir = tiles
        }
    }

    /* ----------------------------------------------------------- disk store */

    private fun fileFor(url: String): File? {
        val root = dir ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        val name = digest.joinToString("") { byte -> "%02x".format(byte) } + ".tile"
        return File(root, name)
    }

    private fun readFromDisk(url: String): ImageBitmap? {
        val file = fileFor(url) ?: return null
        if (!file.exists()) return null
        return try {
            if (file.length() < 64L) {
                runCatching { file.delete() }
                return null
            }
            val bytes = file.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val image = bitmap.asImageBitmap()
            synchronized(store) { store[url] = image }
            image
        } catch (error: Exception) {
            runCatching { file.delete() }
            null
        }
    }

    private fun writeToDisk(url: String, bytes: ByteArray) {
        val file = fileFor(url) ?: return
        try {
            file.writeBytes(bytes)
            file.setLastModified(System.currentTimeMillis())
            writes += 1
            if (writes % 40 == 0) prune()
        } catch (error: Exception) {
            // A full disk must never break the map — the tile is still in memory.
        }
    }

    /** Drops the oldest tiles once the store passes its budget. */
    private fun prune() {
        val root = dir ?: return
        val files = root.listFiles() ?: return
        var total = 0L
        files.forEach { file -> total += file.length() }
        if (total <= DISK_LIMIT_BYTES) return
        val target = (DISK_LIMIT_BYTES * DISK_PRUNE_FACTOR).toLong()
        files.sortedBy { file -> file.lastModified() }.forEach { file ->
            if (total <= target) return@forEach
            val length = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) total -= length
        }
    }

    /* -------------------------------------------------------------- reading */

    fun cached(url: String): ImageBitmap? = synchronized(store) { store[url] }

    /** True when this URL should be downloaded now (not cached, not busy, not failing). */
    fun claim(url: String): Boolean = synchronized(store) {
        if (inFlight.size >= 9) return false
        val failedAt = failures[url]
        if (failedAt != null && System.currentTimeMillis() - failedAt < 2_500L) return false
        if (inFlight.contains(url)) return false
        inFlight.add(url)
        true
    }

    /** True while this tile is in its short back-off after a failed download. */
    fun coolingDown(url: String): Boolean = synchronized(store) {
        val failedAt = failures[url]
        failedAt != null && System.currentTimeMillis() - failedAt < 2_500L
    }

    fun release(url: String) {
        synchronized(store) { inFlight.remove(url) }
    }

    fun load(url: String): ImageBitmap? {
        cached(url)?.let { return it }
        readFromDisk(url)?.let { return it }
        val http = client ?: return null
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) YousifDelivery/1.7")
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    synchronized(store) { failures[url] = System.currentTimeMillis() }
                    return null
                }
                val bytes = response.body?.bytes() ?: return null
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                val image = bitmap.asImageBitmap()
                synchronized(store) {
                    store[url] = image
                    failures.remove(url)
                }
                writeToDisk(url, bytes)
                stamp += 1
                image
            }
        } catch (error: Exception) {
            synchronized(store) { failures[url] = System.currentTimeMillis() }
            null
        }
    }

    /* ------------------------------------------------------- offline download */

    /**
     * Downloads every tile of a geographic box for the given zooms and keeps it
     * on disk, so the area works with no internet afterwards.
     * Returns the number of tiles that are now stored.
     */
    suspend fun downloadArea(
        style: MapStyle,
        dark: Boolean,
        zooms: IntRange,
        minLatitude: Double,
        minLongitude: Double,
        maxLatitude: Double,
        maxLongitude: Double,
        onProgress: (Float) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val urls = LinkedHashSet<String>()
        for (zoom in zooms) {
            val count = 1 shl zoom
            val x0 = Math.floor((minLongitude + 180.0) / 360.0 * count).toInt().coerceIn(0, count - 1)
            val x1 = Math.floor((maxLongitude + 180.0) / 360.0 * count).toInt().coerceIn(0, count - 1)
            val y0 = Math.floor(normalisedY(maxLatitude) * count).toInt().coerceIn(0, count - 1)
            val y1 = Math.floor(normalisedY(minLatitude) * count).toInt().coerceIn(0, count - 1)
            for (y in y0..y1) {
                for (x in x0..x1) {
                    val wrapped = ((x % count) + count) % count
                    urls += TileSources.urlsFor(style, dark, zoom, wrapped, y).first()
                }
            }
        }
        val total = urls.size
        if (total == 0) return@withContext 0
        var done = 0
        urls.chunked(6).forEach { chunk ->
            coroutineScope {
                chunk.forEach { url ->
                    launch {
                        if (cached(url) == null) load(url)
                        synchronized(store) {
                            done += 1
                            onProgress(done.toFloat() / total)
                        }
                    }
                }
            }
        }
        total
    }
}

private fun cleanPins(points: List<MapPoint>): List<MapPoint> {
    val seen = HashSet<String>()
    return points
        .filter { abs(it.latitude) <= 90.0 && abs(it.longitude) <= 180.0 }
        .filter { (it.latitude != 0.0 || it.longitude != 0.0) }
        .filter { pin ->
            val key = if (pin.id.isNotBlank()) pin.id else "${pin.latitude},${pin.longitude},${pin.title}"
            seen.add(key)
        }
        .take(400)
}

private fun wrapLongitude(value: Double): Double {
    var v = value
    while (v > 180.0) v -= 360.0
    while (v < -180.0) v += 360.0
    return v
}
