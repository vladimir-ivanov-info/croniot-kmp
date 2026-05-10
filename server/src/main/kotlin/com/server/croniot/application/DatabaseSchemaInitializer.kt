package com.server.croniot.application

import javax.sql.DataSource

object DatabaseSchemaInitializer {

    private const val SCHEMA_RESOURCE = "/schema.sql"

    private val defaultFeatureFlags = listOf(
        Triple("new_task_ui", false, "Redesigned task creation screen"),
        Triple("sensor_charts", false, "Chart view for sensor history"),
        Triple("push_notifications", false, "Firebase push notifications"),
    )

    fun createSchemaIfNeeded(dataSource: DataSource) {
        val schemaSql = loadSchemaSql()
        val statements = schemaSql
            .split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statements.forEach { statement.execute(it) }
                }
                seedFeatureFlags(connection)
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Error creating database schema", e)
            }
        }
    }

    private fun seedFeatureFlags(connection: java.sql.Connection) {
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS feature_flag (
                    name        VARCHAR(255) PRIMARY KEY,
                    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
                    description VARCHAR(1000),
                    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
                )
                """.trimIndent()
            )
        }

        val sql = """
            INSERT INTO feature_flag (name, enabled, description)
            VALUES (?, ?, ?)
            ON CONFLICT (name) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            for ((name, enabled, description) in defaultFeatureFlags) {
                stmt.setString(1, name)
                stmt.setBoolean(2, enabled)
                stmt.setString(3, description)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadSchemaSql(): String {
        val resource = DatabaseSchemaInitializer::class.java.getResource(SCHEMA_RESOURCE)
            ?: error("Missing classpath resource $SCHEMA_RESOURCE")
        return resource.readText()
    }
}
