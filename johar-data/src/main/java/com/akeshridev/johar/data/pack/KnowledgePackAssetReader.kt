package com.akeshridev.johar.data.pack

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

internal object KnowledgePackAssetReader {
    fun read(context: Context): JSONObject {
        val encoded = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val compressed = Base64.getMimeDecoder().decode(encoded)
        val json = GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader().use { it.readText() }
        return JSONObject(json)
    }

    private const val ASSET_PATH = "johar/johar-knowledge-2026.09-mega-v1.2.pack64"
}
