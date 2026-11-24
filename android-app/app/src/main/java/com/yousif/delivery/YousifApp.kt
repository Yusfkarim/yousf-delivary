package com.yousif.delivery

import android.app.Application
import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

class YousifApp : Application() {
    override fun onCreate() {
        super.onCreate()
        configureOsmdroid(this)
    }
}

/**
 * osmdroid needs a User-Agent (OSM tile policy) and a writable cache directory
 * before the first map is created.
 */
private fun configureOsmdroid(context: Context) {
    val configuration = Configuration.getInstance()
    try {
        configuration.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    } catch (error: Exception) {
        // keep defaults
    }
    configuration.userAgentValue = context.packageName
    val base = File(context.cacheDir, "osmdroid")
    if (!base.exists()) base.mkdirs()
    configuration.osmdroidBasePath = base
    configuration.osmdroidTileCache = File(base, "tiles")
}
