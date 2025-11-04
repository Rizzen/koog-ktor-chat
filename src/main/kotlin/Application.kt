package com.example

import ai.koog.agents.features.sql.providers.PostgresPersistenceSchemaMigrator
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.module() {
    val pgHost = System.getenv("POSTGRES_HOST") ?: "localhost"
    val pgPort = System.getenv("POSTGRES_PORT")?.toIntOrNull() ?: 5432
    val pgDb = System.getenv("POSTGRES_DB") ?: "agents"
    val pgUser = System.getenv("POSTGRES_USER") ?: "agent_user"
    val pgPassword = System.getenv("POSTGRES_PASSWORD") ?: "agent_pass"
    val pgDriver = System.getenv("POSTGRES_JDBC_DRIVER") ?: "org.postgresql.Driver"
    val pgUrl = System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://$pgHost:$pgPort/$pgDb"

    println("Connecting to PostgreSQL at $pgUrl with user $pgUser")
    val migrator = PostgresPersistenceSchemaMigrator(database = Database.connect(
        url = pgUrl,
        driver = pgDriver,
        user = pgUser,
        password = pgPassword
    ), "agent_checkpoints")

    migrator.migrate()
    println("PostgreSQL schema migration completed.")

    configureFrameworks()
    configureRouting()
}
