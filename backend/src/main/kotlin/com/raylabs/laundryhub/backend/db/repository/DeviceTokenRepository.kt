package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.DeviceTokensTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

class DeviceTokenRepository {

    suspend fun registerToken(tokenString: String): Boolean = dbQuery {
        val existing = DeviceTokensTable.select { DeviceTokensTable.token eq tokenString }.singleOrNull()
        if (existing != null) {
            val updatedCount = DeviceTokensTable.update({ DeviceTokensTable.token eq tokenString }) {
                it[updatedAt] = LocalDateTime.now().toString()
            }
            updatedCount > 0
        } else {
            val statement = DeviceTokensTable.insertIgnore {
                it[token] = tokenString
                it[updatedAt] = LocalDateTime.now().toString()
            }
            statement.insertedCount > 0
        }
    }

    suspend fun getAllTokens(): List<String> = dbQuery {
        DeviceTokensTable.selectAll().map { it[DeviceTokensTable.token] }
    }
}
