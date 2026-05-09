package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object SummaryTable : Table("summary") {
    val id = integer("id").autoIncrement()
    val key = varchar("key", 100).uniqueIndex()
    val value = varchar("value", 255)
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(id)
}
