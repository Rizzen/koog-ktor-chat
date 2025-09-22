package com.example

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ChatStore {
    val chats: ConcurrentHashMap<String, MutableList<ChatMessage>> = ConcurrentHashMap()
}

fun Application.configureRouting() {
    val agentService = AgentService()
    routing {
        route("/ai") {
            post("/createChat") {
                val chatId = UUID.randomUUID().toString()
                ChatStore.chats[chatId] = mutableListOf()
                call.respond(CreateChatResponse(chatId, emptyList()))
            }

            post("/chat") {
                val now = Instant.now().toEpochMilli()
                val request = call.receive<ChatRequest>()

                val history = ChatStore.chats.computeIfAbsent(request.chatId) { mutableListOf() }
                history.add(ChatMessage("user", request.message, now))

                val (output, ts) = try {
                    val out = agentService.askChatAgent(request.chatId, request.message)
                    out to now
                } catch (e: Throwable) {
                    application.environment.log.error("AI agent call failed", e)
                    ("Sorry, the AI service is unavailable right now. Please try again later.") to Instant.now()
                        .toEpochMilli()
                }

                history.add(ChatMessage("assistant", output, ts))
                call.respond(ChatResponse(reply = output, history = history))
            }
        }

        get("/") {
            call.respondRedirect("/static/index.html")
        }

        staticResources("/static", "static")
    }
}
