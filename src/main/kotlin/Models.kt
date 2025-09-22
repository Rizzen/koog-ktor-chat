package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(val role: String, val content: String, val timestamp: Long)

@Serializable
data class CreateChatResponse(val chatId: String, val history: List<ChatMessage> = emptyList())

@Serializable
data class ChatRequest(val chatId: String, val message: String)

@Serializable
data class ChatResponse(val reply: String, val history: List<ChatMessage>)

