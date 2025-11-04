package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

// Shared JSON configuration for both REST and WebSocket payloads
val appJson: Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    classDiscriminator = "type"
}

fun Application.configureFrameworks() {
    install(ContentNegotiation) {
        json(appJson)
    }
    install(WebSockets) {
        pingPeriod = 20.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
