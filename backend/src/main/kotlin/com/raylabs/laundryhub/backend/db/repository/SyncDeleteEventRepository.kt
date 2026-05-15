package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.SyncDeleteEventsTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SyncDeleteEvent(
    val id: Int,
    val entityType: String,
    val entityId: String
)

class SyncDeleteEventRepository {

    suspend fun record(entityType: String, entityId: String): Boolean = dbQuery {
        if (entityId.isBlank()) return@dbQuery false

        val statement = SyncDeleteEventsTable.insertIgnore {
            it[SyncDeleteEventsTable.entityType] = entityType
            it[SyncDeleteEventsTable.entityId] = entityId
            it[createdAt] = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
        statement.insertedCount > 0
    }

    suspend fun getPending(): List<SyncDeleteEvent> = dbQuery {
        SyncDeleteEventsTable
            .selectAll()
            .map { it.toSyncDeleteEvent() }
    }

    suspend fun markProcessed(eventIds: List<Int>): Boolean = dbQuery {
        if (eventIds.isEmpty()) return@dbQuery true

        val deletedCount = SyncDeleteEventsTable.deleteWhere {
            SyncDeleteEventsTable.id inList eventIds
        }
        deletedCount > 0
    }

    suspend fun delete(entityType: String, entityId: String): Boolean = dbQuery {
        val deletedCount = SyncDeleteEventsTable.deleteWhere {
            (SyncDeleteEventsTable.entityType eq entityType) and
                (SyncDeleteEventsTable.entityId eq entityId)
        }
        deletedCount > 0
    }

    private fun ResultRow.toSyncDeleteEvent(): SyncDeleteEvent {
        return SyncDeleteEvent(
            id = this[SyncDeleteEventsTable.id],
            entityType = this[SyncDeleteEventsTable.entityType],
            entityId = this[SyncDeleteEventsTable.entityId]
        )
    }
}

object SyncEntityType {
    const val ORDER = "ORDER"
    const val OUTCOME = "OUTCOME"
    const val PACKAGE = "PACKAGE"
    const val GROSS = "GROSS"
    const val SUMMARY = "SUMMARY"
}
