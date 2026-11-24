package com.yousif.delivery.util

/**
 * The backend address and the password are the two things that must never sit on
 * the screen where someone could photograph them. The app keeps the real values
 * in encrypted storage and only ever *shows* a label.
 */
object ServerPrivacy {

    /** «سێرڤەری پارێزراو ✓» / «بەستراوە نییە». */
    fun status(url: String): String =
        if (url.isBlank()) "سێرڤەری نەبەستراوە" else "سێرڤەری پارێزراو ✓"

    /**
     * A harmless hint for maintenance screens: only the ending of the host,
     * never the project name (`•••.app`).
     */
    fun hint(url: String): String {
        val host = url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
        if (host.isBlank()) return "•••"
        val suffix = host.substringAfterLast('.', "")
        return if (suffix.isBlank()) "•••" else "•••.$suffix"
    }

    /** Last two digits of the password, so the driver knows which one he set. */
    fun passwordTail(password: String): String =
        if (password.length < 4) "••••" else "••••" + password.takeLast(2)
}
