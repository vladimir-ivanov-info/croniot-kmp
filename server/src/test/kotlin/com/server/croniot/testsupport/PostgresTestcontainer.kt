package com.server.croniot.testsupport

import com.server.croniot.application.DatabaseMigrator
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.ds.PGSimpleDataSource
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
        DatabaseMigrator.migrate(ds)
        ds
    }

    val dsl: DSLContext by lazy { DSL.using(dataSource, SQLDialect.POSTGRES) }

    /**
     * A brand-new, empty database in the same container, NOT migrated - for tests of the
     * migration itself, which need a database without the shared one's schema.
     */
    fun newEmptyDatabase(name: String): DataSource {
        dataSource.connection.use { it.createStatement().execute("CREATE DATABASE $name") }
        return PGSimpleDataSource().apply {
            setURL(container.jdbcUrl.replace("/croniot_test", "/$name"))
            user = container.username
            password = container.password
        }
    }

    /**
     * Truncate all application tables in FK-safe order. Call from @BeforeEach.
     */
    fun reset() {
        dsl.execute(
            """
            TRUNCATE TABLE
                server_log,
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
