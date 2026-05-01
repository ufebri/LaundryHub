package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.PackagesTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PackageRepository {

    suspend fun insert(pkg: PackageData): Boolean = dbQuery {
        val statement = PackagesTable.insertIgnore {
            it[name] = pkg.name
            it[price] = pkg.price
            it[duration] = pkg.duration
            it[unit] = pkg.unit
            it[isSynced] = false
        }
        statement.insertedCount > 0
    }

    suspend fun update(packageName: String, pkg: PackageData): Boolean = dbQuery {
        val updatedCount = PackagesTable.update({ PackagesTable.name eq packageName }) {
            it[price] = pkg.price
            it[duration] = pkg.duration
            it[unit] = pkg.unit
            it[isSynced] = false
        }
        updatedCount > 0
    }

    suspend fun delete(packageName: String): Boolean = dbQuery {
        val deletedCount = PackagesTable.deleteWhere { name eq packageName }
        deletedCount > 0
    }

    suspend fun insertAll(packages: List<PackageData>): Int = dbQuery {
        var insertedCount = 0
        for (pkg in packages) {
            val statement = PackagesTable.insertIgnore {
                it[name] = pkg.name
                it[price] = pkg.price
                it[duration] = pkg.duration
                it[unit] = pkg.unit
                it[isSynced] = false
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getAll(): List<PackageData> = dbQuery {
        PackagesTable.selectAll().map {
            PackageData(
                name = it[PackagesTable.name],
                price = it[PackagesTable.price],
                duration = it[PackagesTable.duration],
                unit = it[PackagesTable.unit]
            )
        }
    }

    suspend fun getUnsyncedPackages(): List<PackageData> = dbQuery {
        PackagesTable.select { PackagesTable.isSynced eq false }.map {
            PackageData(
                name = it[PackagesTable.name],
                price = it[PackagesTable.price],
                duration = it[PackagesTable.duration],
                unit = it[PackagesTable.unit]
            )
        }
    }

    suspend fun markAsSynced(packageNames: List<String>): Boolean = dbQuery {
        if (packageNames.isEmpty()) return@dbQuery true
        val updatedCount = PackagesTable.update({ PackagesTable.name inList packageNames }) {
            it[isSynced] = true
        }
        updatedCount > 0
    }
}
