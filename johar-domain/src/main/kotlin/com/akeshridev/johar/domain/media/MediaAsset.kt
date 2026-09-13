package com.akeshridev.johar.domain.media

data class MediaAsset(
    val id: String,
    val entityId: String,
    val type: MediaType,
    val sourceUrl: String,
    val mediaUrl: String,
    val previewUrl: String? = null,
    val title: String? = null,
    val description: String? = null,
    val creator: String? = null,
    val attributionText: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
)

enum class MediaType {
    IMAGE,
    VIDEO,
}
