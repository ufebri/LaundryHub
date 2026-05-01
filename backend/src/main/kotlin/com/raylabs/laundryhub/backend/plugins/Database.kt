package com.raylabs.laundryhub.backend.plugins

import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Application.configureDatabase() {
    val jdbcUrl = environment.config.propertyOrNull("storage.jdbcUrl")?.getString()
    val username = environment.config.propertyOrNull("storage.username")?.getString()
    
    println("DEBUG_DB: jdbcUrl=$jdbcUrl, user=$username")

    if (jdbcUrl == null) {
        println("ERROR_DB: jdbcUrl is null! Check Railway Variables.")
    }

    val config = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.driverClassName = driverClassName
        this.username = username
        this.password = password
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    try {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Auto-create tables (for development/migration purposes)
        runBlocking {
            dbQuery {
                SchemaUtils.create(OrdersTable)
            }
        }
        println("Database connected successfully!")
    } catch (e: Exception) {
        println("Warning: Failed to connect to the database. Running in degraded mode. Error: \${e.message}")
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
