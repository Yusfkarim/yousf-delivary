package com.yousif.delivery.util

import java.util.Locale
import kotlin.math.abs

object TextUtils {

    private val western = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val kurdish = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** 1234 → ١٢٣٤ */
    fun toKurdish(input: String): String {
        val builder = StringBuilder(input.length)
        for (character in input) {
            val index = western.indexOf(character)
            builder.append(if (index >= 0) kurdish[index] else character)
        }
        return builder.toString()
    }

    fun distance(meters: Double?): String {
        if (meters == null || meters.isNaN()) return "شوێنەکە نەزانراوە"
        return if (meters < 1_000) {
            "${toKurdish(String.format(Locale.US, "%.0f", meters))} م دوورە"
        } else {
            "${toKurdish(String.format(Locale.US, "%.1f", meters / 1_000))} کم دوورە"
        }
    }

    fun shortDistance(meters: Double?): String {
        if (meters == null || meters.isNaN()) return "—"
        return if (meters < 1_000) {
            "${toKurdish(String.format(Locale.US, "%.0f", meters))} م"
        } else {
            "${toKurdish(String.format(Locale.US, "%.1f", meters / 1_000))} کم"
        }
    }

    fun coordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

    /**
     * Kurdish is right-to-left, so a bare "35.1, 45.6" is re-ordered by bidi and
     * reads as if latitude and longitude were swapped. Isolating the run keeps
     * numbers left-to-right, exactly as they are typed.
     */
    fun ltr(text: String): String = "\u202A$text\u202C"

    fun coordinatesDisplay(latitude: Double, longitude: Double): String =
        ltr(coordinates(latitude, longitude))

    fun relativeTime(timestamp: Long): String {
        val diff = abs(System.currentTimeMillis() - timestamp)
        return when {
            diff < 60_000 -> "ئێستا"
            diff < 3_600_000 -> "${toKurdish((diff / 60_000).toString())} خولەک لەمەوپێش"
            diff < 86_400_000 -> "${toKurdish((diff / 3_600_000).toString())} کاتژمێر لەمەوپێش"
            else -> "${toKurdish((diff / 86_400_000).toString())} ڕۆژ لەمەوپێش"
        }
    }

    fun initials(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "؟"
        return trimmed.take(1)
    }
}
