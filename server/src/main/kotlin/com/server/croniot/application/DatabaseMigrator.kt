package com.server.croniot.application

import org.flywaydb.core.Flyway
import javax.sql.DataSource

// Brings the database schema up to date with the Flyway migrations in
// src/main/resources/db/migration (V1__baseline.sql, V2__..., ...), then
// seeds the default feature flags.
//
// To change the schema, add a new V<n>__<what>.sql. Never edit or delete
// one that is already on main: databases that ran it keep its checksum,
// and Flyway refuses to start against a migration that changed (CI blocks
// it too, see .github/workflows/server.yml).
object DatabaseMigrator {

    private val defaultFeatureFlags = listOf(
        Triple("new_task_ui", false, "Redesigned task creation screen"),
        Triple("sensor_charts", false, "Chart view for sensor history"),
        Triple("push_notifications", false, "Firebase push notifications"),
    )

    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            // V1 is the schema.sql that databases created before Flyway
            // already have: mark them as V1 instead of running it again.
            // An empty database runs V1 like any other migration.
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load()
            .migrate()

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                seedFeatureFlags(connection)
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Error seeding feature flags", e)
            }
        }
    }

    private fun seedFeatureFlags(connection: java.sql.Connection) {
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
}
