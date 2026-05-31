package com.raylabs.laundryhub.backend.plugins

import com.raylabs.laundryhub.backend.db.schema.DeviceTokensTable
import com.raylabs.laundryhub.backend.db.schema.GrossTable
import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.raylabs.laundryhub.backend.db.schema.OutcomesTable
import com.raylabs.laundryhub.backend.db.schema.PackagesTable
import com.raylabs.laundryhub.backend.db.schema.SummaryTable
import com.raylabs.laundryhub.backend.db.schema.SyncDeleteEventsTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

private const val JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql:"
private const val POSTGRESQL_DRIVER_CLASS = "org.postgresql.Driver"

fun Application.configureDatabase() {
    if (System.getProperty("isTest") == "true") return

    val jdbcUrl = System.getenv("DATABASE_URL")
        ?.takeIf { it.isNotBlank() }
        ?: buildJdbcUrlFromEnv()
        ?: environment.config.propertyOrNull("storage.jdbcUrl")?.getString()
        ?: error("DATABASE_URL or storage.jdbcUrl must be configured")
    val user = System.getenv("DATABASE_USER")
        ?.takeIf { it.isNotBlank() }
        ?: environment.config.propertyOrNull("storage.username")?.getString()
        ?: error("DATABASE_USER or storage.username must be configured")
    val password = System.getenv("DATABASE_PASSWORD")
        ?.takeIf { it.isNotBlank() }
        ?: environment.config.propertyOrNull("storage.password")?.getString()
        ?: error("DATABASE_PASSWORD or storage.password must be configured")
    val driverClassName = when {
        jdbcUrl.startsWith(JDBC_POSTGRESQL_PREFIX, ignoreCase = true) -> POSTGRESQL_DRIVER_CLASS
        jdbcUrl.startsWith("jdbc:h2:", ignoreCase = true) -> "org.h2.Driver"
        else -> environment.config.propertyOrNull("storage.driverClassName")?.getString() ?: POSTGRESQL_DRIVER_CLASS
    }

    var finalJdbcUrl = jdbcUrl
    if (finalJdbcUrl.startsWith(JDBC_POSTGRESQL_PREFIX, ignoreCase = true)) {
        if (!finalJdbcUrl.contains("prepareThreshold=")) {
            finalJdbcUrl += if (finalJdbcUrl.contains("?")) "&prepareThreshold=0" else "?prepareThreshold=0"
        }
        if (!finalJdbcUrl.contains("connectTimeout=")) {
            finalJdbcUrl += "&connectTimeout=10"
        }
        if (!finalJdbcUrl.contains("socketTimeout=")) {
            finalJdbcUrl += "&socketTimeout=10"
        }
    }

    val config = HikariConfig().apply {
        this.jdbcUrl = finalJdbcUrl
        this.driverClassName = driverClassName
        this.username = user
        this.password = password
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        if (driverClassName == POSTGRESQL_DRIVER_CLASS) {
            addDataSourceProperty("prepareThreshold", "0")
            addDataSourceProperty("connectTimeout", "10")
            addDataSourceProperty("socketTimeout", "10")
        }
        validate()
    }

    try {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        runBlocking {
            dbQuery {
                SchemaUtils.createMissingTablesAndColumns(
                    OrdersTable,
                    PackagesTable,
                    OutcomesTable,
                    GrossTable,
                    SummaryTable,
                    DeviceTokensTable,
                    SyncDeleteEventsTable
                )
                ensureDeviceTokenColumnCapacity(jdbcUrl)
            }
        }
        logger.info("Database connected successfully!")
    } catch (e: Exception) {
        logger.error("FATAL: Could not connect to database: ${e.message}")
        // Railway akan restart container jika kita exit dengan status 1
        System.exit(1)
    }
}

private fun ensureDeviceTokenColumnCapacity(jdbcUrl: String) {
    if (!jdbcUrl.startsWith(JDBC_POSTGRESQL_PREFIX, ignoreCase = true)) return

    TransactionManager.current().exec(
        "ALTER TABLE device_tokens ALTER COLUMN token TYPE VARCHAR(512)"
    )
}

private fun buildJdbcUrlFromEnv(): String? {
    val host = System.getenv("DATABASE_HOST")?.takeIf { it.isNotBlank() } ?: return null
    val port = System.getenv("DATABASE_PORT")?.takeIf { it.isNotBlank() } ?: "5432"
    val database = System.getenv("DATABASE_NAME")?.takeIf { it.isNotBlank() } ?: "postgres"
    val sslMode = System.getenv("DATABASE_SSL_MODE")?.takeIf { it.isNotBlank() } ?: "require"
    val sslParams = if (sslMode.equals("disable", ignoreCase = true)) {
        "ssl=false"
    } else {
        "ssl=true&sslmode=$sslMode"
    }
    return "jdbc:postgresql://$host:$port/$database?$sslParams"
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) {
        if (System.getenv("ENABLE_SQL_LOGGING") == "true") {
            addLogger(StdOutSqlLogger)
        }
        block()
    }
