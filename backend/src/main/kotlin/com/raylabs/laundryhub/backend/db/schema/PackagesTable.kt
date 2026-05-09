package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object PackagesTable : Table("packages") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255).uniqueIndex()
    val price = varchar("price", 50)
    val duration = varchar("duration", 50)
    val unit = varchar("unit", 50)
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(id)
}
