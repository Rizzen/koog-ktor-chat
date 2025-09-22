package com.example

import ai.koog.ktor.Koog
import ai.koog.ktor.aiAgent
import ai.koog.prompt.llm.OllamaModels
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class ChatMessage(val role: String, val content: String, val timestamp: Long)
@Serializable
data class CreateChatResponse(val chatId: String, val history: List<ChatMessage> = emptyList())
@Serializable
data class ChatRequest(val chatId: String, val message: String)
@Serializable
data class ChatResponse(val reply: String, val history: List<ChatMessage>)

object ChatStore {
    val chats: ConcurrentHashMap<String, MutableList<ChatMessage>> = ConcurrentHashMap()
}

fun Application.configureFrameworks() {
    install(Koog) {
        llm {
            openAI(apiKey = "your-openai-api-key")
            anthropic(apiKey = "your-anthropic-api-key")
            ollama { baseUrl = "http://localhost:11434" }
            google(apiKey = "your-google-api-key")
            openRouter(apiKey = "your-openrouter-api-key")
            deepSeek(apiKey = "your-deepseek-api-key")
        }
    }

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = false })
    }

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
                val output = aiAgent(request.message, model = OllamaModels.Meta.LLAMA_3_2)
                val now2 = Instant.now().toEpochMilli()
                history.add(ChatMessage("assistant", output, now2))
                call.respond(ChatResponse(reply = output, history = history))
            }
        }
    }
}
