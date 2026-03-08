package com.server.croniot.application

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

data class DbConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
)

fun createDataSource(cfg: DbConfig): HikariDataSource {
    val hc = HikariConfig().apply {
        jdbcUrl = cfg.jdbcUrl
        username = cfg.user
        password = cfg.password
        maximumPoolSize = 10
        driverClassName = "org.postgresql.Driver"
    }
    return HikariDataSource(hc)
}

fun createDsl(dataSource: HikariDataSource): DSLContext =
    DSL.using(dataSource, SQLDialect.POSTGRES)

fun main() {
    val ds = createDataSource(
        DbConfig(
            jdbcUrl = System.getenv("CRONIOT_DB_URL")
                ?: error("Missing required environment variable: CRONIOT_DB_URL"),
            user = System.getenv("CRONIOT_DB_USER")
                ?: error("Missing required environment variable: CRONIOT_DB_USER"),
            password = System.getenv("CRONIOT_DB_PASSWORD")
                ?: error("Missing required environment variable: CRONIOT_DB_PASSWORD"),
        )
    )
    val dsl = createDsl(ds)

    val one = dsl.fetchValue("select 1", Int::class.java)
    println("DB OK, select 1 => $one")

    val count = dsl.fetchCount(DSL.table("task"))
    println("Tasks in DB = $count")
}
