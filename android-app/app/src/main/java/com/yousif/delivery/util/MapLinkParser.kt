package com.yousif.delivery.util

/**
 * Extracts `lat,lng` from anything the driver might paste:
 * Google Maps share links (`…/@37.1,45.2,17z`), `?q=`/`destination=` links,
 * `!3d…!4d…` payloads, directions links, or plain "37.123456, 45.123456".
 *
 * Order matters. A shared *directions* link looks like
 * `…/maps/dir/<my position>/<the shop>/data=…` — the first pair is where the
 * driver already stands, the second one is the destination. Taking the first
 * pair is what used to make every pasted link look like "my current location",
 * so `/dir/…` is handled first and the **last** pair wins.
 */
object MapLinkParser {

    private val segmentPattern = Regex("""^(-?\d{1,3}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)$""")

    private val patterns = listOf(
        // The pin Google actually resolved: /place/Name/@…/data=!3d35.1!4d45.2
        Regex("!3d(-?\\d{1,3}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)"),
        // Explicit query parameters (destination of a directions link, search pin…)
        Regex("[?&](?:q|query|destination|daddr|saddr|ll|sll|center|mlat|mlon)=(-?\\d{1,3}(?:\\.\\d+)?)(?:,|%2C|\\s+)(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("(?:geo:|loc:)(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)"),
        // Viewport notation — the last resort before a bare pair.
        Regex("@(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("(-?\\d{1,3}(?:\\.\\d+)?)\\s*[,،;]\\s*(-?\\d{1,3}(?:\\.\\d+)?)"),
    )

    fun parse(input: String): Pair<Double, Double>? {
        val text = normalize(input)
        if (text.isEmpty()) return null

        val directions = text.contains("/dir/")
        if (directions) {
            destinationOf(text)?.let { return it }
        }

        for ((index, pattern) in patterns.withIndex()) {
            if (index == patterns.lastIndex && directions) {
                // Never let the generic pair grab the origin of a directions link.
                continue
            }
            val match = pattern.find(text) ?: continue
            val latitude = match.groupValues[1].toDoubleOrNull() ?: continue
            val longitude = match.groupValues[2].toDoubleOrNull() ?: continue
            if (GeoUtils.isUsable(latitude, longitude)) return latitude to longitude
        }
        return null
    }

    /**
     * `…/dir/35.1106463,45.6877402/35.1143044,45.6829985/data=…` → the second pair.
     */
    private fun destinationOf(text: String): Pair<Double, Double>? {
        val marker = text.indexOf("/dir/")
        if (marker < 0) return null
        val rest = text.substring(marker + "/dir/".length).substringBefore('?')
        var found: Pair<Double, Double>? = null
        for (segment in rest.split('/')) {
            val match = segmentPattern.matchEntire(segment.trim()) ?: continue
            val latitude = match.groupValues[1].toDoubleOrNull() ?: continue
            val longitude = match.groupValues[2].toDoubleOrNull() ?: continue
            if (GeoUtils.isUsable(latitude, longitude)) found = latitude to longitude
        }
        return found
    }

    private fun normalize(value: String): String = value
        .trim()
        .replace("%2C", ",")
        .replace("%2c", ",")
}
