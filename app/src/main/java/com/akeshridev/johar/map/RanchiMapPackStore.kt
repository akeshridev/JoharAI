package com.akeshridev.johar.map

import android.content.Context
import java.io.File

/** Installs the bundled Ranchi PMTiles archive into app-private storage for random-access reads. */
class RanchiMapPackStore(private val context: Context) {
    private val appContext = context.applicationContext

    val mapFile: File
        get() = File(File(appContext.filesDir, MAP_DIR), MAP_FILE)

    fun ensureInstalled(): File? = synchronized(INSTALL_LOCK) {
        val target = mapFile
        if (target.exists() && target.length() > 0L) return@synchronized target
        val temporary = File(target.parentFile, "${target.name}.installing")
        try {
            target.parentFile?.mkdirs()
            appContext.assets.open(ASSET_PATH).use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            check(temporary.length() > 0L && temporary.renameTo(target))
            target
        } catch (_: Exception) {
            temporary.delete()
            null
        }
    }

    companion object {
        private val INSTALL_LOCK = Any()
        private const val MAP_DIR = "maps"
        private const val MAP_FILE = "ranchi.pmtiles"
        private const val ASSET_PATH = "maps/ranchi.pmtiles"
    }
}
