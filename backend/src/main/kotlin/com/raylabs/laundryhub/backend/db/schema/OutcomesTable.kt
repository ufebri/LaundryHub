package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object OutcomesTable : Table("outcomes") {
    val id = varchar("id", 100)
    val date = varchar("date", 50)
    val purpose = varchar("purpose", 255)
    val price = varchar("price", 50)
    val remark = text("remark")
    val payment = varchar("payment", 50)
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(id)
}
