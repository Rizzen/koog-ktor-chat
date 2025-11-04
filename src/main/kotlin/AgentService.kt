package com.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.weave.addWeaveExporter
import ai.koog.agents.features.sql.providers.PostgresPersistenceStorageProvider
import ai.koog.agents.snapshot.feature.Persistency
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

class AgentService {
    fun askChatAgent(agentId: String, question: String): String = runBlocking {
        return@runBlocking getAgent(agentId).run(question)
    }

    fun getAgent(agentId: String): AIAgent<String, String> {
        val pgHost = System.getenv("POSTGRES_HOST") ?: "localhost"
        val pgPort = System.getenv("POSTGRES_PORT")?.toIntOrNull() ?: 5432
        val pgDb = System.getenv("POSTGRES_DB") ?: "agents"
        val pgUser = System.getenv("POSTGRES_USER") ?: "agent_user"
        val pgPassword = System.getenv("POSTGRES_PASSWORD") ?: "agent_pass"
        val pgDriver = System.getenv("POSTGRES_JDBC_DRIVER") ?: "org.postgresql.Driver"
        val pgUrl = System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://$pgHost:$pgPort/$pgDb"
        val weaveEntity = System.getenv()["WEAVE_ENTITY"] ?: throw IllegalArgumentException("WEAVE_ENTITY is not set")
        val apiKey = System.getenv()["WEAVE_API_KEY"] ?: throw IllegalArgumentException("WEAVE_API_KEY is not set")
        val openAiKey = System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY environment variable is not set")
        val anthropicKey = System.getenv("ANTHROPIC_API_KEY") ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable is not set")
        val googleAiKey = System.getenv("GOOGLE_AI_API_KEY") ?: throw IllegalStateException("GOOGLE_AI_API_KEY environment variable is not set")

        return AIAgent(
            promptExecutor = MultiLLMPromptExecutor(
                LLMProvider.OpenAI to OpenAILLMClient(openAiKey),
                LLMProvider.Anthropic to AnthropicLLMClient(anthropicKey),
                LLMProvider.Google to GoogleLLMClient(googleAiKey)
            ),
            llmModel = OpenAIModels.Chat.GPT4o,

        ) {
            install(OpenTelemetry) {
                addWeaveExporter(
                    weaveEntity = weaveEntity,
                    weaveApiKey = apiKey
                )
                setVerbose(true)
            }
            this.install(Persistency) {
                this.storage = PostgresPersistenceStorageProvider(
                    database = Database.connect(
                        url = pgUrl,
                        driver = pgDriver,
                        user = pgUser,
                        password = pgPassword
                    )
                )

                // Enable automatic checkpoint creation
                this.enableAutomaticPersistence = true

                // We preserve message history on restore
                this.rollbackStrategy = RollbackStrategy.MessageHistoryOnly
            }
        }
    }
}