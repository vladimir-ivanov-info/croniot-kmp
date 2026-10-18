package com.server.croniot.application

import com.server.croniot.testsupport.PostgresTestcontainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class DatabaseMigratorTest {

    private fun DataSource.history(): List<Pair<String, String>> = connection.use { connection ->
        connection.createStatement().executeQuery(
            "SELECT version, type FROM flyway_schema_history WHERE success ORDER BY installed_rank",
        ).use { rs ->
            buildList { while (rs.next()) add(rs.getString(1) to rs.getString(2)) }
        }
    }

    private fun DataSource.count(sql: String): Int = connection.use { connection ->
        connection.createStatement().executeQuery(sql).use { rs -> rs.next(); rs.getInt(1) }
    }

    @Test
    fun `WHEN the database is empty THEN migrate runs V1 and seeds the feature flags`() {
        val ds = PostgresTestcontainer.newEmptyDatabase("mig_empty")

        DatabaseMigrator.migrate(ds)

        assertEquals(listOf("1" to "SQL"), ds.history())
        assertEquals(1, ds.count("SELECT count(*) FROM information_schema.tables WHERE table_name = 'device_log'"))
        assertEquals(3, ds.count("SELECT count(*) FROM feature_flag"))
    }

    // The dev database (and any created before Flyway) already holds V1 from
    // the old schema.sql: it must be marked as V1, not have V1 run on top of it.
    @Test
    fun `WHEN the database predates Flyway THEN migrate baselines it at V1 and keeps its data`() {
        val ds = PostgresTestcontainer.newEmptyDatabase("mig_legacy")
        val v1 = javaClass.getResource("/db/migration/V1__baseline.sql")!!.readText()
        ds.connection.use { connection ->
            connection.createStatement().use { it.execute(v1) }
            connection.createStatement().use {
                it.execute("INSERT INTO account (uuid, nickname, email, password) VALUES ('a-1', 'n', 'e@x', 'p')")
            }
        }

        DatabaseMigrator.migrate(ds)

        assertEquals(listOf("1" to "BASELINE"), ds.history())
        assertEquals(1, ds.count("SELECT count(*) FROM account"))
    }

    @Test
    fun `WHEN migrate runs again THEN nothing new is applied`() {
        val ds = PostgresTestcontainer.newEmptyDatabase("mig_twice")
        DatabaseMigrator.migrate(ds)

        DatabaseMigrator.migrate(ds)

        assertEquals(listOf("1" to "SQL"), ds.history())
        assertTrue(ds.count("SELECT count(*) FROM feature_flag") == 3)
    }
}
