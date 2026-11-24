package com.yousif.delivery.util

/**
 * Extracts `lat,lng` from anything the driver might paste:
 * Google Maps share links (`…/@37.1,45.2,17z`), `?q=`/`destination=` links,
 * `!3d…!4d…` payloads, or plain "37.123456, 45.123456".
 */
object MapLinkParser {

    private val patterns = listOf(
        Regex("@(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("[?&](?:q|query|destination|ll|center)=(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("!3d(-?\\d{1,3}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("(-?\\d{1,3}(?:\\.\\d+)?)\\s*[,،;]\\s*(-?\\d{1,3}(?:\\.\\d+)?)"),
    )

    fun parse(input: String): Pair<Double, Double>? {
        val text = input.trim()
        if (text.isEmpty()) return null
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val latitude = match.groupValues[1].toDoubleOrNull() ?: continue
            val longitude = match.groupValues[2].toDoubleOrNull() ?: continue
            if (GeoUtils.isUsable(latitude, longitude)) return latitude to longitude
        }
        return null
    }
}
