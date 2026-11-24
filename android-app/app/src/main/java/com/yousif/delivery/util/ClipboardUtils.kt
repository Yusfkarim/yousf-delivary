package com.yousif.delivery.util

import android.content.ClipboardManager
import android.content.Context

/** Small wrapper around the system clipboard — never throws. */
object ClipboardUtils {

    fun text(context: Context): String? = try {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = manager?.primaryClip
        if (clip == null || clip.itemCount == 0) {
            null
        } else {
            clip.getItemAt(0)?.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
        }
    } catch (error: Exception) {
        null
    }

    fun coordinates(context: Context): Pair<Double, Double>? {
        val text = text(context) ?: return null
        return MapLinkParser.parse(text)
    }
}
