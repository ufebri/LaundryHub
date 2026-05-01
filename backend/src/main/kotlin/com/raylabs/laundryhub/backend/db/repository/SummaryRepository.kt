package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.SummaryTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class SummaryRepository {

    suspend fun insert(summary: SpreadsheetData): Boolean = dbQuery {
        val statement = SummaryTable.insertIgnore {
            it[key] = summary.key
            it[value] = summary.value
            it[isSynced] = false
        }
        statement.insertedCount > 0
    }

    suspend fun update(summaryKey: String, summary: SpreadsheetData): Boolean = dbQuery {
        val updatedCount = SummaryTable.update({ SummaryTable.key eq summaryKey }) {
            it[value] = summary.value
            it[isSynced] = false
        }
        updatedCount > 0
    }

    suspend fun delete(summaryKey: String): Boolean = dbQuery {
        val deletedCount = SummaryTable.deleteWhere { key eq summaryKey }
        deletedCount > 0
    }

    suspend fun insertAll(summaries: List<SpreadsheetData>): Int = dbQuery {
        var insertedCount = 0
        for (summary in summaries) {
            val statement = SummaryTable.insertIgnore {
                it[key] = summary.key
                it[value] = summary.value
                it[isSynced] = false
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getAll(): List<SpreadsheetData> = dbQuery {
        SummaryTable.selectAll().map {
            SpreadsheetData(
                key = it[SummaryTable.key],
                value = it[SummaryTable.value]
            )
        }
    }

    suspend fun getUnsyncedSummaries(): List<SpreadsheetData> = dbQuery {
        SummaryTable.select { SummaryTable.isSynced eq false }.map {
            SpreadsheetData(
                key = it[SummaryTable.key],
                value = it[SummaryTable.value]
            )
        }
    }

    suspend fun markAsSynced(keys: List<String>): Boolean = dbQuery {
        if (keys.isEmpty()) return@dbQuery true
        val updatedCount = SummaryTable.update({ SummaryTable.key inList keys }) {
            it[isSynced] = true
        }
        updatedCount > 0
    }
}
