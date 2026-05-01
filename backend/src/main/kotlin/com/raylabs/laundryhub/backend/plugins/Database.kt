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
    // Membaca konfigurasi dari environment variables
    val host = System.getenv("DATABASE_HOST") ?: "db.ludtihnzlskvzdqsvube.supabase.co"
    val port = System.getenv("DATABASE_PORT") ?: "5432"
    val database = System.getenv("DATABASE_NAME") ?: "postgres"

    val user = System.getenv("DATABASE_USER") ?: "postgres"
    val password = System.getenv("DATABASE_PASSWORD") ?: "password"

    // Supabase mewajibkan SSL
    val jdbcUrl = "jdbc:postgresql://$host:$port/$database?ssl=true&sslmode=verify-full&sslrootcert=/etc/ssl/certs/ca-certificates.crt"

    val driverClassName = "org.postgresql.Driver"
    println("DEBUG_DB: jdbcUrl=$jdbcUrl, user=$user")

    val config = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.driverClassName = driverClassName
        this.username = user
        this.password = password
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    try {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        runBlocking {
            dbQuery {
                SchemaUtils.create(OrdersTable)
            }
        }
        println("Database connected successfully!")
    } catch (e: Exception) {
        println("FATAL: Could not connect to database: ${e.message}")
        // Railway akan restart container jika kita exit dengan status 1
        System.exit(1)
    }
}


suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
