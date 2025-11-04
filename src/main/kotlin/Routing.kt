package com.example

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ChatStore {
    val chats: ConcurrentHashMap<String, MutableList<ChatMessage>> = ConcurrentHashMap()
}

fun Application.configureRouting() {
    val agentService = AgentService()
    routing {
        // WebSocket chat endpoint
        webSocket("/ws/chat") {
            val log = application.environment.log
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            when (val msg = appJson.decodeFromString(WsInbound.serializer(), text)) {
                                is WsCreateChat -> {
                                    val chatId = UUID.randomUUID().toString()
                                    ChatStore.chats[chatId] = mutableListOf()
                                    val out = WsChatCreated(chatId, emptyList())
                                    send(Frame.Text(appJson.encodeToString(WsOutbound.serializer(), out)))
                                }
                                is WsUserMessage -> {
                                    val now = Instant.now().toEpochMilli()
                                    val history = ChatStore.chats.computeIfAbsent(msg.chatId) { mutableListOf() }
                                    history.add(ChatMessage("user", msg.message, now))

                                    val (reply, ts) = try {
                                        val out = withContext(Dispatchers.IO) { agentService.askChatAgent(msg.chatId, msg.message) }
                                        out to now
                                    } catch (t: Throwable) {
                                        log.error("AI agent call failed", t)
                                        ("Sorry, the AI service is unavailable right now. Please try again later.") to Instant.now().toEpochMilli()
                                    }

                                    history.add(ChatMessage("assistant", reply, ts))
                                    val out = WsAssistantMessage(msg.chatId, reply, ts, history)
                                    send(Frame.Text(appJson.encodeToString(WsOutbound.serializer(), out)))
                                }
                                is WsHistoryRequest -> {
                                    val history = ChatStore.chats[msg.chatId] ?: emptyList()
                                    val out = WsHistory(msg.chatId, history)
                                    send(Frame.Text(appJson.encodeToString(WsOutbound.serializer(), out)))
                                }
                            }
                        } catch (e: Throwable) {
                            log.warn("Failed to process WS message: ${'$'}text", e)
                            val err = WsError("Invalid message payload")
                            send(Frame.Text(appJson.encodeToString(WsOutbound.serializer(), err)))
                        }
                    }
                }
            } catch (t: Throwable) {
                // Connection closed or unexpected error
                application.environment.log.error("WebSocket error", t)
                val err = WsError("Internal server error")
                send(Frame.Text(appJson.encodeToString(WsOutbound.serializer(), err)))
            }
        }

        // REST endpoints (kept for backward compatibility)
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
