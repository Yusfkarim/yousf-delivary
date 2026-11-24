package com.yousif.delivery.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Turns anything the driver can get out of Google Maps into coordinates.
 *
 * The hard case is the share button: it produces a short link such as
 * `https://maps.app.goo.gl/HB8C994iDU5ACnF7A`, which carries **no** coordinates
 * at all — only a redirect. So the resolver follows the redirect chain and then
 * looks for coordinates in the final URL, and if that is still empty, in the
 * HTML body (Google embeds `!3d…!4d…` and `"latitude":…,"longitude":…`).
 *
 * Everything is cached per link, so pasting the same link twice is instant.
 */
object MapLinkResolver {

    private val urlRegex = Regex("""https?://[^\s"'<>()]+""", RegexOption.IGNORE_CASE)
    private val bodyPatterns = listOf(
        Regex("""!3d(-?\d{1,3}(?:\.\d+)?)!4d(-?\d{1,3}(?:\.\d+)?)"""),
        Regex("""@(-?\d{1,3}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)"""),
        Regex(""""latitude"\s*:\s*(-?\d{1,3}(?:\.\d+)?)\s*,\s*"longitude"\s*:\s*(-?\d{1,3}(?:\.\d+)?)"""),
        Regex("""center=(-?\d{1,3}(?:\.\d+)?)%2C(-?\d{1,3}(?:\.\d+)?)"""),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cache = mutableMapOf<String, Pair<Double, Double>?>()

    /** True when the text contains a link that still has to be resolved. */
    fun hasLink(text: String): Boolean = urlRegex.containsMatchIn(text)

    fun firstLink(text: String): String? = urlRegex.find(text)?.value

    /**
     * Blocking resolver — call it from `Dispatchers.IO`.
     * Returns null when nothing usable was found (wrong link, offline, …).
     */
    suspend fun resolve(text: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext null

        MapLinkParser.parse(trimmed)?.let { return@withContext it }

        val link = firstLink(trimmed) ?: return@withContext null

        synchronized(cache) { if (cache.containsKey(link)) return@withContext cache[link] }
        val found = follow(link)
        synchronized(cache) { cache[link] = found }
        found
    }

    private fun follow(link: String): Pair<Double, Double>? {
        val request = Request.Builder()
            .url(link)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) YousifDelivery")
            .header("Accept-Language", "en")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                MapLinkParser.parse(URLDecoder.decode(finalUrl, "UTF-8"))
                    ?: MapLinkParser.parse(finalUrl)
                    ?: run {
                        val body = response.body?.string().orEmpty()
                        parseBody(body) ?: parseBody(URLDecoder.decode(body, "UTF-8"))
                    }
            }
        } catch (error: Exception) {
            null
        }
    }

    private fun parseBody(body: String): Pair<Double, Double>? {
        if (body.isEmpty()) return null
        for (pattern in bodyPatterns) {
            val match = pattern.find(body) ?: continue
            val latitude = match.groupValues[1].toDoubleOrNull() ?: continue
            val longitude = match.groupValues[2].toDoubleOrNull() ?: continue
            if (GeoUtils.isUsable(latitude, longitude)) return latitude to longitude
        }
        return null
    }
}

/**
 * Hand-off for `ACTION_SEND`: Google Maps (or WhatsApp) shares a link into the
 * app, the activity stores it here and the UI opens the "new customer" sheet
 * with the coordinates already filled in.
 */
object ShareInbox {
    val link = MutableStateFlow<String?>(null)

    fun push(text: String?) {
        val value = text?.trim().orEmpty()
        if (value.isNotEmpty()) link.value = value
    }

    fun clear() {
        link.value = null
    }
}
