package com.yousif.delivery.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yousif.delivery.R
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.map.MapStyle
import com.yousif.delivery.ui.map.TileSources
import com.yousif.delivery.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.util.MapTileIndex
import java.util.concurrent.TimeUnit

/**
 * A lightweight static map preview (one tile + a pin) used inside the sheets.
 *
 * A real [android.view.View]-based map inside a scrolling sheet fights the parent
 * for drag gestures — this is what the app used to freeze on. A tile bitmap has
 * no gestures at all, so the sheet stays smooth and the driver still sees where
 * the pin is.
 */
private val thumbClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .build()

private val thumbCache = object : LinkedHashMap<String, Bitmap>(24, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean = size > 24
}

@Composable
fun MapThumb(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Int = 16,
) {
    val colors = LocalAppColors.current
    val style = if (colors.isDark) MapStyle.DARK else MapStyle.STREET
    val source = TileSources.forStyle(style, colors.isDark)

    val tileX = ((longitude + 180.0) / 360.0 * (1 shl zoom)).toInt()
    val latRad = Math.toRadians(latitude)
    val tileY = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
    val url = source.getTileURLString(MapTileIndex.getTileIndex(zoom, tileX, tileY))

    var bitmap by remember(url) { mutableStateOf<Bitmap?>(thumbCache[url]) }

    LaunchedEffect(url) {
        if (bitmap != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "YousifDelivery/1.4 (Android)")
                    .build()
                thumbClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else response.body?.byteStream()?.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
        if (loaded != null) {
            synchronized(thumbCache) { thumbCache[url] = loaded }
            bitmap = loaded
        }
    }

    Box(modifier.clip(RoundedCornerShape(18.dp)).background(colors.surfaceAlt)) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "نەخشەی شوێن",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_map_pin),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp),
            )
        } else {
            Icon(
                AppIcons.map(),
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )
        }
    }
}
