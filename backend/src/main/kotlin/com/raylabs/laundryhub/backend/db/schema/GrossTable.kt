package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object GrossTable : Table("gross") {
    val id = integer("id").autoIncrement()
    val month = varchar("month", 50).uniqueIndex()
    val totalNominal = varchar("total_nominal", 50)
    val orderCount = varchar("order_count", 50)
    val tax = varchar("tax", 50)
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(id)
}
