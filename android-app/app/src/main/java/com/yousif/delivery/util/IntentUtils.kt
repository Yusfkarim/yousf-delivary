package com.yousif.delivery.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * All external intents used by the app.
 * Every launcher returns `false` instead of crashing when the target app is missing.
 */
object IntentUtils {

    fun dial(context: Context, rawNumber: String): Boolean {
        val uri = Uri.parse("tel:${PhoneUtils.local(rawNumber)}")
        return launch(context, Intent(Intent.ACTION_DIAL, uri))
    }

    fun whatsapp(context: Context, rawNumber: String): Boolean {
        val number = PhoneUtils.normalize(rawNumber)
        return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")))
    }

    /**
     * Viber deep links are fragile:
     *  - `viber://add?number=<digits>` (no plus sign) is the one that actually works
     *  - `viber://contact?number=%2B<digits>` is the legacy fallback
     */
    fun viber(context: Context, rawNumber: String): Boolean {
        val digits = PhoneUtils.normalize(rawNumber)
        if (digits.isEmpty()) return false
        if (launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("viber://add?number=$digits")))) return true
        if (launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("viber://contact?number=%2B$digits")))) return true
        toast(context, "ڤایبەر لەم مۆبایلەدا دامەزراو نییە")
        return false
    }

    fun googleNavigation(context: Context, latitude: Double, longitude: Double): Boolean {
        val withMaps = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$latitude,$longitude"),
        ).setPackage("com.google.android.apps.maps")
        if (launch(context, withMaps)) return true

        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"),
        )
        if (launch(context, web)) return true

        toast(context, "ئەپی نەخشە نەدۆزرایەوە")
        return false
    }

    fun copyToClipboard(context: Context, label: String, value: String): Boolean {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        manager.setPrimaryClip(ClipData.newPlainText(label, value))
        return true
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun toastLong(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun launch(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) == null) return false
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            false
        }
    }
}
