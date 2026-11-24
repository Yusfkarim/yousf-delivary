package com.yousif.delivery.util

object PhoneUtils {

    /** Arabic-Indic (٠-٩ / ۰-۹) digits typed on a Kurdish keyboard → ASCII. */
    private fun ascii(raw: String): String = buildString {
        for (ch in raw) {
            when (ch) {
                in '\u0660'..'\u0669' -> append('0' + (ch - '\u0660'))
                in '\u06F0'..'\u06F9' -> append('0' + (ch - '\u06F0'))
                else -> append(ch)
            }
        }
    }

    fun digits(raw: String): String = ascii(raw).filter { it.isDigit() }

    /**
     * What the phone field accepts while typing: digits, a single leading `+`
     * and spaces (so `+964750 272 4374` stays exactly as typed). Everything else
     * — including the bidi marks a paste can carry — is dropped.
     */
    fun sanitizeInput(raw: String): String {
        val text = ascii(raw).trimStart()
        val builder = StringBuilder(text.length)
        text.forEachIndexed { index, ch ->
            when {
                ch.isDigit() -> builder.append(ch)
                ch == '+' && index == 0 && !builder.contains('+') -> builder.append('+')
                ch == ' ' || ch == '-' -> builder.append(' ')
            }
        }
        return builder.toString()
    }

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

    /** Pretty grouping for display: `0750 272 4374` (kept left-to-right). */
    fun pretty(raw: String): String {
        val localForm = local(raw)
        if (localForm.length < 8) return TextUtils.ltr(raw.trim())
        val grouped = if (localForm.length == 11) {
            "${localForm.substring(0, 4)} ${localForm.substring(4, 7)} ${localForm.substring(7)}"
        } else {
            localForm.chunked(4).joinToString(" ")
        }
        return TextUtils.ltr(grouped)
    }

    /** Same grouping, but keeping the international prefix: `+964 750 272 4374`. */
    fun prettyInternational(raw: String): String {
        val normalized = normalize(raw)
        if (!normalized.startsWith("964") || normalized.length < 13) return TextUtils.ltr(raw.trim())
        val rest = normalized.substring(3)
        return TextUtils.ltr("+964 ${rest.chunked(3).joinToString(" ")}".trim())
    }
}
