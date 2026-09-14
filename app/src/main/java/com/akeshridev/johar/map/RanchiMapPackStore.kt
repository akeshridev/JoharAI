package com.akeshridev.johar.map

import android.content.Context
import java.io.File

/** Installs the bundled Ranchi PMTiles archive into app-private storage for random-access reads. */
class RanchiMapPackStore(private val context: Context) {
    private val appContext = context.applicationContext

    val mapFile: File
        get() = File(File(appContext.filesDir, MAP_DIR), MAP_FILE)

    fun ensureInstalled(): File? {
        val target = mapFile
        if (target.exists() && target.length() > 0L) return target

        return try {
            target.parentFile?.mkdirs()
            appContext.assets.open(ASSET_PATH).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.takeIf { it.exists() && it.length() > 0L }
        } catch (_: Exception) {
            // The developer build may intentionally omit the large PMTiles asset.
            target.delete()
            null
        }
    }

    companion object {
        private const val MAP_DIR = "maps"
        private const val MAP_FILE = "ranchi.pmtiles"
        private const val ASSET_PATH = "maps/ranchi.pmtiles"
    }
}
