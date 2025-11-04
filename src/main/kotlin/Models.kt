package com.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class ChatMessage(val role: String, val content: String, val timestamp: Long)

// REST DTOs (kept for backward compatibility)
@Serializable
data class CreateChatResponse(val chatId: String, val history: List<ChatMessage> = emptyList())

@Serializable
data class ChatRequest(val chatId: String, val message: String)

@Serializable
data class ChatResponse(val reply: String, val history: List<ChatMessage>)

// WebSocket protocol models
@JsonClassDiscriminator("type")
@Serializable
sealed interface WsInbound

@Serializable
@SerialName("create_chat")
data object WsCreateChat : WsInbound

@Serializable
@SerialName("user_message")
data class WsUserMessage(val chatId: String, val message: String) : WsInbound

@Serializable
@SerialName("history_request")
data class WsHistoryRequest(val chatId: String) : WsInbound

@JsonClassDiscriminator("type")
@Serializable
sealed interface WsOutbound

@Serializable
@SerialName("chat_created")
data class WsChatCreated(val chatId: String, val history: List<ChatMessage> = emptyList()) : WsOutbound

@Serializable
@SerialName("assistant_message")
data class WsAssistantMessage(val chatId: String, val reply: String, val timestamp: Long, val history: List<ChatMessage>) : WsOutbound

@Serializable
@SerialName("history")
data class WsHistory(val chatId: String, val history: List<ChatMessage>) : WsOutbound

@Serializable
@SerialName("error")
data class WsError(val message: String) : WsOutbound

