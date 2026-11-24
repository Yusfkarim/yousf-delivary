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
    SATELLITE("سەتەلایت"),
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
            .replace("{s}", ((x + y) % 4).toString())
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
    /** Satellite imagery (the style the driver asked for on the map). */
    private val satellite = TemplateTileSource(
        name = "GoogleSatellite",
        template = "https://mt{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
        copyright = "© Google",
    )

    /**
     * Esri's World Imagery — the backup that keeps the map filled when Google's
     * hosts answer slowly or refuse a tile. Different host, different CDN.
     */
    private val esriSatellite = TemplateTileSource(
        name = "EsriWorldImagery",
        template = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        copyright = "© Esri, Maxar, Earthstar Geographics",
    )

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

    /** Single tile URL — the map draws tiles itself instead of using a MapView. */
    fun urlFor(style: MapStyle, dark: Boolean, zoom: Int, x: Int, y: Int): String =
        forStyle(style, dark).getTileURLString(MapTileIndex.getTileIndex(zoom, x, y))

    /**
     * Every provider that can serve this tile, best first.
     *
     * Satellite has two independent hosts, so a tile that Google refuses is
     * fetched from Esri instead of leaving a blank cream square on the map.
     */
    fun urlsFor(style: MapStyle, dark: Boolean, zoom: Int, x: Int, y: Int): List<String> {
        val index = MapTileIndex.getTileIndex(zoom, x, y)
        return when {
            style == MapStyle.SATELLITE -> listOf(
                satellite.getTileURLString(index),
                esriSatellite.getTileURLString(index),
            )
            else -> listOf(forStyle(style, dark).getTileURLString(index))
        }
    }

    fun forStyle(style: MapStyle, dark: Boolean): OnlineTileSourceBase = when (style) {
        MapStyle.DARK -> cartoDark
        MapStyle.SATELLITE -> satellite
        MapStyle.STREET -> if (dark) cartoDark else preferredStreet
    }

    fun attribution(style: MapStyle, dark: Boolean): String = when (style) {
        MapStyle.DARK -> "© OpenStreetMap · © CARTO"
        MapStyle.SATELLITE -> "© Google · © Esri Imagery"
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
