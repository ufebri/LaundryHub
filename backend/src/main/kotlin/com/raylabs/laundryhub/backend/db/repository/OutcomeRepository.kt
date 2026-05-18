package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OutcomesTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.backend.util.parseSupportedLaundryDate
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.update

class OutcomeRepository {

    suspend fun insertWithNextId(outcome: OutcomeData): OutcomeData? = dbQuery {
        TransactionManager.current().exec("SELECT pg_advisory_xact_lock($OUTCOME_ID_ALLOCATION_LOCK_KEY)")
        val nextId = OutcomesTable
            .selectAll()
            .mapNotNull { it[OutcomesTable.id].toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?.toString()
            ?: "0"
        val createdOutcome = outcome.copy(id = nextId)
        val statement = OutcomesTable.insertIgnore {
            it[id] = createdOutcome.id
            it[date] = createdOutcome.date
            it[purpose] = createdOutcome.purpose
            it[price] = createdOutcome.price
            it[remark] = createdOutcome.remark
            it[payment] = createdOutcome.payment
            it[isSynced] = false
        }
        if (statement.insertedCount > 0) createdOutcome else null
    }

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

    suspend fun upsert(outcome: OutcomeData): Boolean = dbQuery {
        val existing = OutcomesTable.select { OutcomesTable.id eq outcome.id }.singleOrNull()
        if (existing != null) {
            val updatedCount = OutcomesTable.update({ OutcomesTable.id eq outcome.id }) {
                it[date] = outcome.date
                it[purpose] = outcome.purpose
                it[price] = outcome.price
                it[remark] = outcome.remark
                it[payment] = outcome.payment
                it[isSynced] = true
            }
            updatedCount > 0
        } else {
            val statement = OutcomesTable.insertIgnore {
                it[id] = outcome.id
                it[date] = outcome.date
                it[purpose] = outcome.purpose
                it[price] = outcome.price
                it[remark] = outcome.remark
                it[payment] = outcome.payment
                it[isSynced] = true
            }
            statement.insertedCount > 0
        }
    }

    suspend fun delete(outcomeId: String): Boolean = dbQuery {
        val deletedCount = OutcomesTable.deleteWhere { id eq outcomeId }
        deletedCount > 0
    }

    suspend fun getById(outcomeId: String): OutcomeData? = dbQuery {
        OutcomesTable
            .select { OutcomesTable.id eq outcomeId }
            .singleOrNull()
            ?.toOutcomeData()
    }

    suspend fun getLatestId(): String = dbQuery {
        OutcomesTable
            .selectAll()
            .map { it[OutcomesTable.id] }
            .maxByOrNull { it.toIntOrNull() ?: Int.MIN_VALUE }
            ?: "0"
    }

    suspend fun getNextId(): String = dbQuery {
        OutcomesTable
            .selectAll()
            .mapNotNull { it[OutcomesTable.id].toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?.toString()
            ?: "0"
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
                it[isSynced] = true
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getAll(page: Int = 1, size: Int = 50): List<OutcomeData> = dbQuery {
        val offset = ((page - 1) * size).coerceAtLeast(0)
        OutcomesTable.selectAll()
            .map { it.toOutcomeData() }
            .sortedWith(outcomeDateComparator())
            .drop(offset)
            .take(size)
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

    private fun org.jetbrains.exposed.sql.ResultRow.toOutcomeData(): OutcomeData {
        return OutcomeData(
            id = this[OutcomesTable.id],
            date = this[OutcomesTable.date],
            purpose = this[OutcomesTable.purpose],
            price = this[OutcomesTable.price],
            remark = this[OutcomesTable.remark],
            payment = this[OutcomesTable.payment]
        )
    }

    private fun outcomeDateComparator(): Comparator<OutcomeData> {
        val idDesc = compareByDescending<OutcomeData> { it.id.toIntOrNull() ?: Int.MIN_VALUE }
        return compareByDescending<OutcomeData> { parseSupportedLaundryDate(it.date)?.time ?: Long.MIN_VALUE }
            .then(idDesc)
    }
}

private const val OUTCOME_ID_ALLOCATION_LOCK_KEY = 63119042
