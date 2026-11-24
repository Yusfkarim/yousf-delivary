package com.yousif.delivery.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.R
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.map.TileSources
import com.yousif.delivery.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
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

private data class VisibleTile(val url: String, val x: Int, val y: Int)

/**
 * The map of the whole app, drawn by the app itself.
 *
 * Tiles are decoded on IO and painted in **one** Canvas pass, so their positions
 * are exact: no seam, no duplicated column, no half-tile drift while panning.
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
    interactive: Boolean = true,
    showControls: Boolean = true,
    allowStyleSwitch: Boolean = false,
    initialStyle: MapStyle = MapStyle.STREET,
    onLocate: (() -> Unit)? = null,
    onTap: ((Double, Double) -> Unit)? = null,
    onPinTap: ((MapPoint) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val density = LocalDensity.current

    remember(context) { TileCache.ensure(context) }

    var style by remember { mutableStateOf(initialStyle) }
    var zoomLevel by remember { mutableStateOf(zoom) }

    val pins = remember(customer, points, me) {
        val list = mutableListOf<MapPoint>()
        customer?.let { list.add(it) }
        list.addAll(points)
        sortAndClean(list)
    }

    val fallbackLat = pins.firstOrNull()?.latitude ?: me?.latitude ?: 35.1143044
    val fallbackLng = pins.firstOrNull()?.longitude ?: me?.longitude ?: 45.6829985

    var centerLat by remember { mutableStateOf(center?.latitude ?: fallbackLat) }
    var centerLng by remember { mutableStateOf(center?.longitude ?: fallbackLng) }

    LaunchedEffect(center?.latitude, center?.longitude) {
        val target = center ?: return@LaunchedEffect
        centerLat = target.latitude
        centerLng = target.longitude
    }

    var tilesLoaded by remember(style) { mutableStateOf(false) }
    val bitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }

    val placeholder = if (colors.isDark) Color(0xFF15100D) else Color(0xFFEFE6DC)

    BoxWithConstraints(
        modifier
            .background(placeholder),
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val zInt = zoomLevel.roundToInt().coerceIn(3, 19)

        // Frame every saved pin once, when the tab is opened.
        LaunchedEffect(widthPx, heightPx, pins.size, center) {
            if (center != null || pins.size < 2 || widthPx <= 0 || heightPx <= 0) return@LaunchedEffect
            val minLat = pins.minOf { it.latitude }
            val maxLat = pins.maxOf { it.latitude }
            val minLng = pins.minOf { it.longitude }
            val maxLng = pins.maxOf { it.longitude }
            var z = 18
            while (z > 4) {
                val spanY = (latToTileY(minLat, z) - latToTileY(maxLat, z)) * TILE_PX
                val spanX = (lngToTileX(maxLng, z) - lngToTileX(minLng, z)) * TILE_PX
                if (spanY <= heightPx * 0.70 && spanX <= widthPx * 0.70) break
                z -= 1
            }
            zoomLevel = z.toDouble()
            centerLat = (minLat + maxLat) / 2.0
            centerLng = (minLng + maxLng) / 2.0
        }

        val geometry = remember(zInt, centerLat, centerLng, widthPx, heightPx) {
            MapGeometry(centerLat, centerLng, zInt, widthPx, heightPx)
        }

        val tiles = remember(geometry, style, colors.isDark) {
            if (widthPx <= 0 || heightPx <= 0) return@remember emptyList()
            val list = mutableListOf<VisibleTile>()
            for (ty in geometry.firstY..geometry.lastY) {
                if (ty < 0 || ty >= geometry.tileCount) continue
                for (tx in geometry.firstX..geometry.lastX) {
                    val wrappedX = ((tx % geometry.tileCount) + geometry.tileCount) % geometry.tileCount
                    list.add(
                        VisibleTile(
                            url = TileSources.urlFor(style, colors.isDark, geometry.zoom, wrappedX, ty),
                            x = geometry.offsetX(tx),
                            y = geometry.offsetY(ty),
                        ),
                    )
                }
            }
            list
        }

        // Fetch only what is missing; each bitmap is remembered for the session.
        LaunchedEffect(tiles) {
            tiles.forEach { tile ->
                if (bitmaps.containsKey(tile.url)) return@forEach
                launch(Dispatchers.IO) {
                    val loaded = TileCache.load(tile.url)
                    if (loaded != null) {
                        bitmaps[tile.url] = loaded
                        if (!tilesLoaded) tilesLoaded = true
                    }
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            drawRect(placeholder)
            tiles.forEach { tile ->
                val bitmap = bitmaps[tile.url]
                if (bitmap != null) {
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(tile.x, tile.y),
                        dstSize = IntSize(TILE_PX, TILE_PX),
                    )
                }
            }
        }

        pins.forEach { pin ->
            val px = (geometry.xOf(pin.longitude) - 18f)
            val py = (geometry.yOf(pin.latitude) - 36f)
            if (px > -80f && px < widthPx + 80f && py > -80f && py < heightPx + 80f) {
                Icon(
                    painter = painterResource(R.drawable.ic_map_pin),
                    contentDescription = pin.title.takeIf { it.isNotBlank() },
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .size(36.dp),
                )
            }
        }

        me?.let { driver ->
            val px = geometry.xOf(driver.longitude) - with(density) { 15.dp.toPx() }
            val py = geometry.yOf(driver.latitude) - with(density) { 15.dp.toPx() }
            if (px > -80f && px < widthPx + 80f && py > -80f && py < heightPx + 80f) {
                Icon(
                    painter = painterResource(R.drawable.ic_map_me),
                    contentDescription = "شوێنی من",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .size(30.dp),
                )
            }
        }

        if (interactive) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(zInt, widthPx, heightPx) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val scale = 360.0 / (TILE_PX * (1 shl zInt))
                            centerLng = wrapLongitude(centerLng - drag.x * scale)
                            centerLat = clampLatitude(
                                centerLat + drag.y * scale * cos(Math.toRadians(centerLat)),
                            )
                        }
                    },
            )
        }

        if (onTap != null || onPinTap != null) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(zInt, widthPx, heightPx, centerLat, centerLng, pins) {
                        detectTapGestures { offset ->
                            if (onPinTap != null) {
                                val hit = nearestPin(geometry, pins, offset.x, offset.y)
                                if (hit != null) {
                                    onPinTap(hit)
                                    return@detectTapGestures
                                }
                            }
                            onTap?.let { callback ->
                                val lng = tileXToLng(geometry.leftTile + offset.x / TILE_PX, zInt)
                                val lat = tileYToLat(geometry.topTile + offset.y / TILE_PX, zInt)
                                if (abs(lat) <= 85.0) callback(lat, wrapLongitude(lng))
                            }
                        }
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
                    zoomLevel = (zoomLevel + 1.0).coerceIn(3.0, 19.0)
                }
                MapControl(AppIcons.minus(), colors.isDark) {
                    zoomLevel = (zoomLevel - 1.0).coerceIn(3.0, 19.0)
                }
                if (onLocate != null) {
                    MapControl(AppIcons.locate(), colors.isDark, tint = colors.brand, onClick = onLocate)
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

        if (!tilesLoaded) {
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

/**
 * Everything needed to place a tile or a pin: the visible window in tile units
 * plus the pixel origin of the first whole tile.
 */
private class MapGeometry(
    centerLat: Double,
    centerLng: Double,
    val zoom: Int,
    private val widthPx: Int,
    private val heightPx: Int,
) {
    val tileCount: Int = 1 shl zoom
    val leftTile: Double = lngToTileX(centerLng, zoom) - widthPx / 2.0 / TILE_PX
    val topTile: Double = latToTileY(centerLat, zoom) - heightPx / 2.0 / TILE_PX
    val firstX: Int = floor(leftTile).toInt()
    val lastX: Int = floor(leftTile + widthPx.toDouble() / TILE_PX).toInt()
    val firstY: Int = floor(topTile).toInt()
    val lastY: Int = floor(topTile + heightPx.toDouble() / TILE_PX).toInt()

    fun offsetX(tileX: Int): Int = ((tileX - leftTile) * TILE_PX).roundToInt()
    fun offsetY(tileY: Int): Int = ((tileY - topTile) * TILE_PX).roundToInt()

    fun xOf(longitude: Double): Float = ((lngToTileX(longitude, zoom) - leftTile) * TILE_PX).toFloat()
    fun yOf(latitude: Double): Float = ((latToTileY(latitude, zoom) - topTile) * TILE_PX).toFloat()
}

private fun nearestPin(
    geometry: MapGeometry,
    pins: List<MapPoint>,
    tapX: Float,
    tapY: Float,
): MapPoint? {
    var best: MapPoint? = null
    var bestDistance = 46f
    pins.forEach { pin ->
        val x = geometry.xOf(pin.longitude)
        val y = geometry.yOf(pin.latitude) - 18f
        val distance = hypot(tapX - x, tapY - y)
        if (distance < bestDistance) {
            bestDistance = distance
            best = pin
        }
    }
    return best
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
 */
private object TileCache {

    private val store = object : LinkedHashMap<String, ImageBitmap>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean =
            size > 64
    }

    private var client: OkHttpClient? = null

    fun ensure(context: android.content.Context) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            val cacheDir = File(context.cacheDir, "tile-cache")
            runCatching { cacheDir.mkdirs() }
            client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .cache(Cache(cacheDir, 24L * 1024 * 1024))
                .build()
        }
    }

    fun cached(url: String): ImageBitmap? = synchronized(store) { store[url] }

    fun load(url: String): ImageBitmap? {
        cached(url)?.let { return it }
        val http = client ?: return null
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) YousifDelivery/1.6")
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                val image = bitmap.asImageBitmap()
                synchronized(store) { store[url] = image }
                image
            }
        } catch (error: Exception) {
            null
        }
    }
}

private fun sortAndClean(points: List<MapPoint>): List<MapPoint> =
    points.filter { abs(it.latitude) <= 90.0 && abs(it.longitude) <= 180.0 }.take(300)

private fun clampLatitude(value: Double): Double = value.coerceIn(-85.0, 85.0)

private fun wrapLongitude(value: Double): Double {
    var v = value
    while (v > 180.0) v -= 360.0
    while (v < -180.0) v += 360.0
    return v
}

private fun lngToTileX(longitude: Double, zoom: Int): Double =
    (longitude + 180.0) / 360.0 * (1 shl zoom)

private fun latToTileY(latitude: Double, zoom: Int): Double {
    val rad = Math.toRadians(clampLatitude(latitude))
    return (1.0 - ln(Math.tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * (1 shl zoom)
}

private fun tileXToLng(x: Double, zoom: Int): Double = x / (1 shl zoom) * 360.0 - 180.0

private fun tileYToLat(y: Double, zoom: Int): Double {
    val n = Math.PI - 2.0 * Math.PI * y / (1 shl zoom)
    return Math.toDegrees(kotlin.math.atan(0.5 * (Math.exp(n) - Math.exp(-n))))
}
