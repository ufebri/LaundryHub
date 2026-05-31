package com.raylabs.laundryhub.backend.plugins

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseTest {

    @Suppress("UNCHECKED_CAST")
    private fun setEnv(key: String, value: String?) {
        try {
            val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
            val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
            theEnvironmentField.isAccessible = true
            val env = theEnvironmentField.get(null) as MutableMap<String, String>
            if (value == null) {
                env.remove(key)
            } else {
                env[key] = value
            }
            val theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
            theCaseInsensitiveEnvironmentField.isAccessible = true
            val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
            if (value == null) {
                cienv.remove(key)
            } else {
                cienv[key] = value
            }
        } catch (e: Exception) {
            try {
                val envField = System::class.java.getDeclaredField("mep")
                envField.isAccessible = true
                val mep = envField.get(null) as MutableMap<String, String>
                if (value == null) {
                    mep.remove(key)
                } else {
                    mep[key] = value
                }
            } catch (e2: Exception) {
                // Fallback if environment editing is blocked or not supported on this platform
            }
        }
    }

    private fun callBuildJdbcUrlFromEnv(env: Map<String, String>): String? {
        val method = Class.forName("com.raylabs.laundryhub.backend.plugins.DatabaseKt")
            .getDeclaredMethod("buildJdbcUrlFromEnv", Map::class.java)
        method.isAccessible = true
        return method.invoke(null, env) as String?
    }

    private fun callEnsureDeviceTokenColumnCapacity(jdbcUrl: String) {
        val method = Class.forName("com.raylabs.laundryhub.backend.plugins.DatabaseKt")
            .getDeclaredMethod("ensureDeviceTokenColumnCapacity", String::class.java)
        method.isAccessible = true
        method.invoke(null, jdbcUrl)
    }

    @Test
    fun testBuildJdbcUrlFromEnvBranches() {
        // 1. DATABASE_HOST is null -> should return null
        assertNull(callBuildJdbcUrlFromEnv(emptyMap()))

        // 2. DATABASE_HOST is set, others default -> uses default port 5432, db postgres, ssl require
        val env2 = mapOf("DATABASE_HOST" to "myhost")
        assertEquals("jdbc:postgresql://myhost:5432/postgres?ssl=true&sslmode=require", callBuildJdbcUrlFromEnv(env2))

        // 3. DATABASE_HOST set, DATABASE_SSL_MODE is disable -> uses ssl=false
        val env3 = mapOf("DATABASE_HOST" to "myhost", "DATABASE_SSL_MODE" to "disable")
        assertEquals("jdbc:postgresql://myhost:5432/postgres?ssl=false", callBuildJdbcUrlFromEnv(env3))

        // 4. DATABASE_HOST set, Custom port, name, and SSL mode
        val env4 = mapOf(
            "DATABASE_HOST" to "myhost",
            "DATABASE_PORT" to "9999",
            "DATABASE_NAME" to "mydb",
            "DATABASE_SSL_MODE" to "prefer"
        )
        assertEquals("jdbc:postgresql://myhost:9999/mydb?ssl=true&sslmode=prefer", callBuildJdbcUrlFromEnv(env4))
    }


    @Test
    fun testEnsureDeviceTokenColumnCapacityReturnsEarlyOnNonPostgres() {
        // Calling it with a non-postgresql URL should return immediately and not call TransactionManager.current()
        // which would otherwise crash if not in a transaction
        callEnsureDeviceTokenColumnCapacity("jdbc:h2:mem:test")
    }

    @Test
    fun testDatabaseConfigurationShortCircuitsIfIsTest() = testApplication {
        val originalIsTest = System.getProperty("isTest")
        System.setProperty("isTest", "true")
        try {
            application {
                configureDatabase() // Should return immediately
            }
        } finally {
            if (originalIsTest != null) {
                System.setProperty("isTest", originalIsTest)
            } else {
                System.clearProperty("isTest")
            }
        }
    }

    @Test
    fun testDatabaseConfigurationH2Path() = testApplication {
        val originalIsTest = System.getProperty("isTest")
        System.setProperty("isTest", "false")

        val testConfig = MapApplicationConfig().apply {
            put("storage.jdbcUrl", "jdbc:h2:mem:test_configure_db_main;MODE=MySQL;DB_CLOSE_DELAY=-1;")
            put("storage.username", "sa")
            put("storage.password", "")
            put("storage.driverClassName", "org.h2.Driver")
        }

        environment {
            config = testConfig
        }

        application {
            configureDatabase()
        }

        if (originalIsTest != null) {
            System.setProperty("isTest", originalIsTest)
        } else {
            System.clearProperty("isTest")
        }

        var success = false
        kotlinx.coroutines.runBlocking {
            dbQuery {
                success = true
            }
        }
        assertTrue(success)
    }

    @Test
    fun testDbQueryWithSqlLogging() = kotlinx.coroutines.runBlocking {
        val originalLog = System.getenv("ENABLE_SQL_LOGGING")
        try {
            setEnv("ENABLE_SQL_LOGGING", "true")
            var ran = false
            dbQuery {
                ran = true
            }
            assertTrue(ran)

            setEnv("ENABLE_SQL_LOGGING", "false")
            ran = false
            dbQuery {
                ran = true
            }
            assertTrue(ran)
        } finally {
            setEnv("ENABLE_SQL_LOGGING", originalLog)
        }
    }

    @Test
    fun testSelectDriverClassName() {
        assertEquals("org.postgresql.Driver", selectDriverClassName("jdbc:postgresql://localhost:5432/db", null))
        assertEquals("org.h2.Driver", selectDriverClassName("jdbc:h2:mem:test", null))
        assertEquals("com.mysql.cj.jdbc.Driver", selectDriverClassName("jdbc:mysql://localhost:3306/db", "com.mysql.cj.jdbc.Driver"))
        assertEquals("org.postgresql.Driver", selectDriverClassName("jdbc:mysql://localhost:3306/db", null))
    }

    @Test
    fun testFormatPostgresUrl() {
        // 1. With prepareThreshold, connectTimeout, socketTimeout already present
        val originalUrl = "jdbc:postgresql://localhost:5432/db?prepareThreshold=0&connectTimeout=10&socketTimeout=10"
        assertEquals(originalUrl, formatPostgresUrl(originalUrl))

        // 2. Postgres URL without parameters
        assertEquals(
            "jdbc:postgresql://localhost:5432/db?prepareThreshold=0&connectTimeout=10&socketTimeout=10",
            formatPostgresUrl("jdbc:postgresql://localhost:5432/db")
        )

        // 3. Postgres URL with existing other parameters
        assertEquals(
            "jdbc:postgresql://localhost:5432/db?ssl=true&prepareThreshold=0&connectTimeout=10&socketTimeout=10",
            formatPostgresUrl("jdbc:postgresql://localhost:5432/db?ssl=true")
        )

        // 4. Non-postgres URL
        assertEquals("jdbc:h2:mem:test", formatPostgresUrl("jdbc:h2:mem:test"))
    }

    @Test
    fun testPrepareHikariConfig() {
        // 1. Postgres config mapping
        val pgConfig = prepareHikariConfig(
            jdbcUrl = "jdbc:postgresql://localhost:5432/db?prepareThreshold=0",
            user = "postgres",
            pass = "secret",
            driverClassName = "org.postgresql.Driver"
        )
        assertEquals("jdbc:postgresql://localhost:5432/db?prepareThreshold=0", pgConfig.jdbcUrl)
        assertEquals("postgres", pgConfig.username)
        assertEquals("secret", pgConfig.password)
        assertEquals("org.postgresql.Driver", pgConfig.driverClassName)
        assertEquals(3, pgConfig.maximumPoolSize)
        assertEquals("TRANSACTION_REPEATABLE_READ", pgConfig.transactionIsolation)

        // 2. H2 config mapping
        val h2Config = prepareHikariConfig(
            jdbcUrl = "jdbc:h2:mem:test",
            user = "sa",
            pass = "",
            driverClassName = "org.h2.Driver"
        )
        assertEquals("jdbc:h2:mem:test", h2Config.jdbcUrl)
        assertEquals("sa", h2Config.username)
        assertEquals("", h2Config.password)
        assertEquals("org.h2.Driver", h2Config.driverClassName)
    }
}
