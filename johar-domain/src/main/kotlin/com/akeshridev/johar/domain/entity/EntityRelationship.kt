package com.akeshridev.johar.domain.entity

data class EntityRelationship(
    val id: String,
    val fromEntityId: String,
    val toEntityId: String,
    val predicate: String,
    val sourceUrl: String,
    val publisher: String,
    val retrievedAtEpochMillis: Long,
    val evidenceText: String,
)
