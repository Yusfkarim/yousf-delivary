package com.yousif.delivery.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.util.concurrent.TimeUnit

/** The looks the driver can pick from inside the map. */
enum class MapStyle(val label: String) {
    STREET("شەقام"),
    LIGHT("ڕووناک"),
    DARK("شەو"),
    ;

    companion object {
        fun of(value: String?): MapStyle = entries.firstOrNull { it.name == value } ?: STREET
    }
}

/**
 * A tile source assembled from a `{z}/{x}/{y}` template.
 *
 * All providers here are keyless and allow caching: Google's raster tiles would
 * need a billed API key and forbid offline caching, which this app depends on.
 */
class TemplateTileSource(
    name: String,
    private val template: String,
    private val copyright: String,
    minZoom: Int = 3,
    maxZoom: Int = 19,
) : OnlineTileSourceBase(name, minZoom, maxZoom, 256, ".png", arrayOf(""), copyright) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return template
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }
}

object TileSources {

    /** Classic OpenStreetMap — the most detailed and the most widely reachable. */
    private val osm = TemplateTileSource(
        name = "OsmStandard",
        template = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        copyright = "© OpenStreetMap contributors",
    )

    /** CARTO's pale basemap — the "clean" light look of the design. */
    private val cartoLight = TemplateTileSource(
        name = "CartoLight",
        template = "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
        copyright = "© OpenStreetMap · © CARTO",
    )

    private val cartoDark = TemplateTileSource(
        name = "CartoDark",
        template = "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        copyright = "© OpenStreetMap · © CARTO",
    )

    /** Providers tried in order for the street style. */
    private val streetChain = listOf(osm, cartoLight)

    @Volatile
    private var preferredStreet: OnlineTileSourceBase = osm

    fun currentStreet(): OnlineTileSourceBase = preferredStreet

    fun forStyle(style: MapStyle, dark: Boolean): OnlineTileSourceBase = when (style) {
        MapStyle.DARK -> cartoDark
        MapStyle.LIGHT -> cartoLight
        MapStyle.STREET -> if (dark) cartoDark else preferredStreet
    }

    fun attribution(style: MapStyle, dark: Boolean): String = when (style) {
        MapStyle.DARK -> "© OpenStreetMap · © CARTO"
        MapStyle.LIGHT -> "© OpenStreetMap · © CARTO"
        MapStyle.STREET -> if (dark) "© OpenStreetMap · © CARTO" else "© OpenStreetMap contributors"
    }

    @Volatile
    private var ready = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Checks the providers with a real tile and keeps the first one that answers.
     * Some ISPs block one CDN but not the other, so a hard-coded host is not an
     * option — a single unreachable host is what made the map look empty.
     */
    suspend fun ensureReady(): Boolean = withContext(Dispatchers.IO) {
        if (ready) return@withContext true
        for (candidate in streetChain) {
            if (probe(candidate)) {
                preferredStreet = candidate
                ready = true
                break
            }
        }
        ready
    }

    fun probe(source: OnlineTileSourceBase): Boolean = try {
        val url = source.getTileURLString(MapTileIndex.getTileIndex(4, 9, 6))
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "YousifDelivery/1.4 (Android)")
            .build()
        client.newCall(request).execute().use { response ->
            val length = response.body?.contentLength() ?: 0L
            response.isSuccessful && length > 512L
        }
    } catch (error: Exception) {
        false
    }
}
