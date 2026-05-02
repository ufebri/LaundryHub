package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OutcomesTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class OutcomeRepository {

    suspend fun insert(outcome: OutcomeData): Boolean = dbQuery {
        val statement = OutcomesTable.insertIgnore {
            it[id] = outcome.id
            it[date] = outcome.date
            it[purpose] = outcome.purpose
            it[price] = outcome.price
            it[remark] = outcome.remark
            it[payment] = outcome.payment
            it[isSynced] = false
        }
        statement.insertedCount > 0
    }

    suspend fun update(outcomeId: String, outcome: OutcomeData): Boolean = dbQuery {
        val updatedCount = OutcomesTable.update({ OutcomesTable.id eq outcomeId }) {
            it[date] = outcome.date
            it[purpose] = outcome.purpose
            it[price] = outcome.price
            it[remark] = outcome.remark
            it[payment] = outcome.payment
            it[isSynced] = false
        }
        updatedCount > 0
    }

    suspend fun delete(outcomeId: String): Boolean = dbQuery {
        val deletedCount = OutcomesTable.deleteWhere { id eq outcomeId }
        deletedCount > 0
    }

    suspend fun insertAll(outcomes: List<OutcomeData>): Int = dbQuery {
        var insertedCount = 0
        for (outcome in outcomes) {
            val statement = OutcomesTable.insertIgnore {
                it[id] = outcome.id
                it[date] = outcome.date
                it[purpose] = outcome.purpose
                it[price] = outcome.price
                it[remark] = outcome.remark
                it[payment] = outcome.payment
                it[isSynced] = false
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getAll(page: Int = 1, size: Int = 50): List<OutcomeData> = dbQuery {
        val offset = ((page - 1) * size).toLong()
        OutcomesTable.selectAll()
            .orderBy(OutcomesTable.id to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(size, offset = offset)
            .map {
                OutcomeData(
                    id = it[OutcomesTable.id],
                    date = it[OutcomesTable.date],
                    purpose = it[OutcomesTable.purpose],
                    price = it[OutcomesTable.price],
                    remark = it[OutcomesTable.remark],
                    payment = it[OutcomesTable.payment]
                )
            }
    }

    suspend fun getUnsyncedOutcomes(): List<OutcomeData> = dbQuery {
        OutcomesTable.select { OutcomesTable.isSynced eq false }.map {
            OutcomeData(
                id = it[OutcomesTable.id],
                date = it[OutcomesTable.date],
                purpose = it[OutcomesTable.purpose],
                price = it[OutcomesTable.price],
                remark = it[OutcomesTable.remark],
                payment = it[OutcomesTable.payment]
            )
        }
    }

    suspend fun markAsSynced(outcomeIds: List<String>): Boolean = dbQuery {
        if (outcomeIds.isEmpty()) return@dbQuery true
        val updatedCount = OutcomesTable.update({ OutcomesTable.id inList outcomeIds }) {
            it[isSynced] = true
        }
        updatedCount > 0
    }
}
