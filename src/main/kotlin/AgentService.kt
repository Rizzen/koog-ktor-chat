package com.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.features.sql.providers.PostgresPersistencyStorageProvider
import ai.koog.agents.snapshot.feature.Persistency
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
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

        return AIAgent(
            promptExecutor = simpleOllamaAIExecutor(),
            llmModel = OllamaModels.Meta.LLAMA_3_2,
        ) {
            this.install(Persistency) {
                this.storage = PostgresPersistencyStorageProvider(
                    persistenceId = agentId,
                    database = Database.connect(
                        url = pgUrl,
                        driver = pgDriver,
                        user = pgUser,
                        password = pgPassword
                    )
                )

                // Enable automatic checkpoint creation
                this.enableAutomaticPersistency = true

                // We preserve message history on restore
                this.rollbackStrategy = RollbackStrategy.MessageHistoryOnly
            }
        }
    }
}