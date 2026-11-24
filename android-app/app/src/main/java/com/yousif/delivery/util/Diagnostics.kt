package com.yousif.delivery.util

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Step-by-step server check used by the lock screen so the driver (and whoever
 * supports him) can see exactly where the connection dies: DNS, TCP, TLS or HTTP.
 */
object Diagnostics {

    fun run(baseUrl: String): List<String> {
        val lines = mutableListOf<String>()
        val trimmed = baseUrl.trim().trimEnd('/')

        val host = try {
            URI(trimmed).host
        } catch (error: Exception) {
            null
        }
        if (host.isNullOrBlank()) {
            lines += "ناونیشانەکە دروست نییە: $trimmed"
            return lines
        }
        lines += "ناونیشان: $trimmed"
        lines += "Host: $host"

        try {
            val addresses = InetAddress.getAllByName(host)
            lines += "DNS ✓  " + addresses.joinToString(", ") { it.hostAddress ?: "?" }
        } catch (error: Exception) {
            lines += "DNS ✗  " + (error.message ?: error.javaClass.simpleName)
            lines += "واتە: ئینتەرنێت یان DNS کار ناکات"
            return lines
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, 443), 8000)
                lines += "TCP 443 ✓"
            }
        } catch (error: Exception) {
            lines += "TCP 443 ✗  " + (error.message ?: error.javaClass.simpleName)
            lines += "واتە: پەیوەندی بە سێرڤەر قەدەغەیە (فلتەر / VPN)"
            return lines
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        try {
            val request = Request
                .Builder()
                .url("$trimmed/api/health")
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty().replace("\n", " ").trim().take(180)
                val location = response.header("Location").orEmpty()
                lines += "HTTP ${response.code}"
                if (location.isNotBlank()) {
                    lines += "Redirect → $location"
                    lines += "واتە: Vercel Deployment Protection چالاکە"
                }
                lines += if (body.isBlank()) "بەشی وەڵام بەتاڵە" else "بەشی وەڵام: $body"
            }
        } catch (error: Exception) {
            lines += "HTTP ✗  " + (error.message ?: error.javaClass.simpleName)
        }

        return lines
    }
}
