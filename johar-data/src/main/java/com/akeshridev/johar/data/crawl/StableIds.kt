package com.akeshridev.johar.data.crawl

import java.security.MessageDigest

internal fun stableId(
    prefix: String,
    vararg parts: String?,
): String {
    val raw = parts.joinToString(separator = "\u001f") { it.orEmpty().trim() }
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
    return "$prefix:${digest.take(24)}"
}

internal fun normalizeText(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim()
