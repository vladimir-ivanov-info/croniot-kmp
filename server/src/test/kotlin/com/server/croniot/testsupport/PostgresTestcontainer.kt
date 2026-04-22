package com.server.croniot.testsupport

import com.server.croniot.application.DatabaseSchemaInitializer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

/**
 * Shared Postgres container across tests in the same JVM.
 *
 * Why shared: spinning up a fresh container per test class is slow (~3s each). By reusing a
 * single container and truncating rows between tests, the full integration suite stays fast.
 *
 * How to apply: integration tests reference [dataSource] / [dsl] and call [reset] in @BeforeEach.
 */
object PostgresTestcontainer {

    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("croniot_test")
            withUsername("croniot")
            withPassword("croniot")
            start()
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { stop() } })
        }
    }

    val dataSource: DataSource by lazy {
        val config = HikariConfig().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            maximumPoolSize = 4
            minimumIdle = 1
        }
        val ds = HikariDataSource(config)
        DatabaseSchemaInitializer.createSchemaIfNeeded(ds)
        ds
    }

    val dsl: DSLContext by lazy { DSL.using(dataSource, SQLDialect.POSTGRES) }

    /**
     * Truncate all application tables in FK-safe order. Call from @BeforeEach.
     */
    fun reset() {
        dsl.execute(
            """
            TRUNCATE TABLE
                refresh_token,
                task_parameter_value,
                task_state_info,
                task,
                sensor_data,
                parameter_task_constraints,
                parameter_task,
                parameter_sensor_constraints,
                parameter_sensor,
                task_type,
                sensor_type,
                device_token,
                device_properties,
                device,
                account
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }
}
