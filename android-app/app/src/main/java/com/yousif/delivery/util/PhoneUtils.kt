package com.yousif.delivery.util

object PhoneUtils {

    fun digits(raw: String): String = raw.filter { it.isDigit() }

    /**
     * Normalises Iraqi numbers to the international form used by WhatsApp/Viber:
     * `07xx xxx xxxx` → `9647xxxxxxxxx`, `+964 7xx…` → `9647xx…`, `00964…` → `964…`.
     */
    fun normalize(raw: String): String {
        var value = digits(raw)
        if (value.startsWith("00")) value = value.substring(2)
        return when {
            value.startsWith("964") -> value
            value.startsWith("0") -> "964" + value.substring(1)
            value.length == 10 && value.startsWith("7") -> "964$value"
            else -> value
        }
    }

    /** Local dialling form: 9647xx… → 07xx…. */
    fun local(raw: String): String {
        val normalized = normalize(raw)
        return if (normalized.startsWith("964")) "0" + normalized.substring(3) else digits(raw)
    }

    /** Pretty grouping for display: 0750 123 4567 */
    fun pretty(raw: String): String {
        val localForm = local(raw)
        if (localForm.length < 8) return raw
        return localForm.chunked(4).joinToString(" ")
    }
}
