package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object SummaryTable : Table("summary") {
    val key = varchar("key", 100)
    val value = varchar("value", 255)
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(key)
}
