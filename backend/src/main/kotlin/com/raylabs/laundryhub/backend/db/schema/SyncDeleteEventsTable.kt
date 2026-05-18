package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object SyncDeleteEventsTable : Table("sync_delete_events") {
    val id = integer("id").autoIncrement()
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 255)
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(entityType, entityId)
    }
}
