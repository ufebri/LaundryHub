package com.raylabs.laundryhub.backend.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import com.raylabs.laundryhub.backend.db.schema.GrossTable
import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.raylabs.laundryhub.backend.db.schema.OutcomesTable
import com.raylabs.laundryhub.backend.db.schema.PackagesTable
import com.raylabs.laundryhub.backend.db.schema.SummaryTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Application.configureDatabase() {
    // Menggunakan Supavisor Pooler (IPv4 compatible) untuk Railway
    val host = System.getenv("DATABASE_HOST") ?: "aws-1-ap-south-1.pooler.supabase.com"
    val port = System.getenv("DATABASE_PORT") ?: "5432"
    val database = System.getenv("DATABASE_NAME") ?: "postgres"

    val user = System.getenv("DATABASE_USER") ?: "postgres.ludtihnzlskvzdqsvube"
    val password = System.getenv("DATABASE_PASSWORD") ?: "password"

    // Supavisor Pooler (Session Mode) mendukung SSL, gunakan sslmode=require untuk enkripsi
    val jdbcUrl = "jdbc:postgresql://$host:$port/$database?ssl=true&sslmode=require"

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
                SchemaUtils.create(OrdersTable, PackagesTable, OutcomesTable, GrossTable, SummaryTable)
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
