package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object PackagesTable : Table("packages") {
    // Packages might not have a strong string ID in the old system, we can use name as ID or create an auto-incrementing ID.
    // Looking at PackageData: price, name, duration, unit, sheetRowIndex.
    // Since "name" is unique in packages (usually), let's use it as primary key.
    val name = varchar("name", 255)
    val price = varchar("price", 50)
    val duration = varchar("duration", 50)
    val unit = varchar("unit", 50)
    val isSynced = bool("is_synced").default(false)

    override val primaryKey = PrimaryKey(name)
}
