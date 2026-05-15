package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object DeviceTokensTable : Table("device_tokens") {
    val token = varchar("token", 512)
    val updatedAt = varchar("updated_at", 255)

    override val primaryKey = PrimaryKey(token)
}
