package com.server.croniot.application

import javax.sql.DataSource

object DatabaseSchemaInitializer {

    private const val SCHEMA_RESOURCE = "/schema.sql"

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
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Error creating database schema", e)
            }
        }
    }

    private fun loadSchemaSql(): String {
        val resource = DatabaseSchemaInitializer::class.java.getResource(SCHEMA_RESOURCE)
            ?: error("Missing classpath resource $SCHEMA_RESOURCE")
        return resource.readText()
    }
}
