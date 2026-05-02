package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.GrossTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class GrossRepository {

    suspend fun insert(gross: GrossData): Boolean = dbQuery {
        val statement = GrossTable.insertIgnore {
            it[month] = gross.month
            it[totalNominal] = gross.totalNominal
            it[orderCount] = gross.orderCount
            it[tax] = gross.tax
            it[isSynced] = false
        }
        statement.insertedCount > 0
    }

    suspend fun update(monthId: String, gross: GrossData): Boolean = dbQuery {
        val updatedCount = GrossTable.update({ GrossTable.month eq monthId }) {
            it[totalNominal] = gross.totalNominal
            it[orderCount] = gross.orderCount
            it[tax] = gross.tax
            it[isSynced] = false
        }
        updatedCount > 0
    }

    suspend fun delete(monthId: String): Boolean = dbQuery {
        val deletedCount = GrossTable.deleteWhere { month eq monthId }
        deletedCount > 0
    }

    suspend fun insertAll(grossList: List<GrossData>): Int = dbQuery {
        var insertedCount = 0
        for (gross in grossList) {
            val statement = GrossTable.insertIgnore {
                it[month] = gross.month
                it[totalNominal] = gross.totalNominal
                it[orderCount] = gross.orderCount
                it[tax] = gross.tax
                it[isSynced] = false
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getAll(page: Int = 1, size: Int = 50): List<GrossData> = dbQuery {
        val offset = ((page - 1) * size).toLong()
        GrossTable.selectAll()
            .orderBy(GrossTable.month to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(size, offset = offset)
            .map {
                GrossData(
                    month = it[GrossTable.month],
                    totalNominal = it[GrossTable.totalNominal],
                    orderCount = it[GrossTable.orderCount],
                    tax = it[GrossTable.tax]
                )
            }
    }

    suspend fun getUnsyncedGross(): List<GrossData> = dbQuery {
        GrossTable.select { GrossTable.isSynced eq false }.map {
            GrossData(
                month = it[GrossTable.month],
                totalNominal = it[GrossTable.totalNominal],
                orderCount = it[GrossTable.orderCount],
                tax = it[GrossTable.tax]
            )
        }
    }

    suspend fun markAsSynced(months: List<String>): Boolean = dbQuery {
        if (months.isEmpty()) return@dbQuery true
        val updatedCount = GrossTable.update({ GrossTable.month inList months }) {
            it[isSynced] = true
        }
        updatedCount > 0
    }
}
