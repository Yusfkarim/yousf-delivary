package com.yousif.delivery.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Turns a Google Maps share link into coordinates.
 *
 * A short link (`https://maps.app.goo.gl/…`) carries no numbers at all, only a
 * redirect. Google answers with a `302` whose `Location` already contains the
 * latitude/longitude (`…/dir/35.1127834,45.6806275/…`), so the redirects are
 * followed **by hand** and every `Location` is parsed as it arrives. Following
 * them blindly (the default) throws that information away, which is exactly why
 * the link used to end up "not found".
 */
object MapLinkResolver {

    private val urlRegex = Regex("""https?://[^\s"'<>()]+""", RegexOption.IGNORE_CASE)
    private val bodyPatterns = listOf(
        Regex("""!3d(-?\d{1,3}(?:\.\d+)?)!4d(-?\d{1,3}(?:\.\d+)?)"""),
        Regex("""@(-?\d{1,3}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)"""),
        Regex("""\[(-?\d{1,3}\.\d{3,}),\s*(-?\d{1,3}\.\d{3,})\]"""),
        Regex(""""latitude"\s*:\s*(-?\d{1,3}(?:\.\d+)?)\s*,\s*"longitude"\s*:\s*(-?\d{1,3}(?:\.\d+)?)"""),
        Regex("""center=(-?\d{1,3}(?:\.\d+)?)(?:,|%2C)(-?\d{1,3}(?:\.\d+)?)"""),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val cache = mutableMapOf<String, Pair<Double, Double>?>()

    fun hasLink(text: String): Boolean = urlRegex.containsMatchIn(text)

    fun firstLink(text: String): String? = urlRegex.find(text)?.value

    /** Blocking resolver — call it from a coroutine, never on the main thread. */
    suspend fun resolve(text: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext null

        MapLinkParser.parse(trimmed)?.let { return@withContext it }

        val link = firstLink(trimmed) ?: return@withContext null
        synchronized(cache) { cache[link]?.let { return@withContext it } }

        val found = follow(link)
        synchronized(cache) { cache[link] = found }
        found
    }

    private fun follow(link: String): Pair<Double, Double>? {
        var current = link
        repeat(6) {
            decode(current)?.let { decoded -> MapLinkParser.parse(decoded)?.let { return it } }
            MapLinkParser.parse(current)?.let { return it }

            val url = current.toHttpUrlOrNull() ?: return null
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; YousifDelivery)")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en")
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val location = response.header("Location")
                    if (response.isRedirect && !location.isNullOrBlank()) {
                        val next = url.resolve(location)?.toString() ?: return null
                        // Google puts the coordinates in the first hop.
                        decode(next)?.let { MapLinkParser.parse(it)?.let { found -> return found } }
                        MapLinkParser.parse(next)?.let { return it }
                        current = next
                        return@use
                    }

                    val length = response.body?.contentLength() ?: 0L
                    if (length in 1..3_000_000) {
                        val body = response.body?.string().orEmpty()
                        parseBody(body)?.let { return it }
                        // Last resort: any maps URL embedded in the page (og:image,
                        // canonical, JS bootstrap state).
                        for (match in urlRegex.findAll(body).take(40)) {
                            val candidate = match.value.replace("\\u0026", "&")
                            MapLinkParser.parse(decode(candidate) ?: candidate)?.let { return it }
                        }
                    }
                    return null
                }
            } catch (error: Exception) {
                return null
            }
        }
        return null
    }

    private fun decode(value: String): String? = runCatching {
        URLDecoder.decode(value, "UTF-8")
    }.getOrNull()

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
