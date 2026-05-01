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
    val jdbcUrl = System.getenv("DATABASE_URL") ?: environment.config.propertyOrNull("storage.jdbcUrl")?.getString() ?: "jdbc:postgresql://localhost:5432/laundryhub"
    val username = System.getenv("DATABASE_USER") ?: environment.config.propertyOrNull("storage.username")?.getString() ?: "postgres"
    val password = System.getenv("DATABASE_PASSWORD") ?: environment.config.propertyOrNull("storage.password")?.getString() ?: "password"
    val driverClassName = "org.postgresql.Driver"

    println("DEBUG_DB: jdbcUrl=$jdbcUrl, user=$username")

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
