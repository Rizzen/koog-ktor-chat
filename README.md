# Koog + Ktor Persistent Chat Agent Example

This repository is a minimal example of a simple chat “agent” built with:
- Ktor for the HTTP server and routing
- Koog for LLM integration and agent lifecycle
- Postgres for persistent agent state (checkpoints)
- A tiny static frontend for chatting in the browser

The goal is to demonstrate how to wire Koog’s agent and persistence features into a Ktor app and expose a tiny JSON API the UI can talk to.

## What it does
- Creates a chat per browser session (client asks, assistant answers)
- Uses Koog to run an LLM-backed agent
- Persists agent checkpoints to PostgreSQL so the agent can restore across restarts
- Provides a small, self-contained web UI at /

By default the agent runs against an Ollama model (llama3.2). You can swap it for another Koog-supported backend if you want.

## Architecture (high level)
- Ktor app initializes Koog and JSON serialization.
- Two API endpoints are served under /ai:
  - POST /ai/createChat → creates a new chatId
  - POST /ai/chat → sends a message and returns the assistant reply plus history
- AgentService wires a Koog AIAgent with:
  - Ollama executor (default model: Meta.LLAMA_3_2)
  - Koog Persistency using PostgreSQL
  - RollbackStrategy.MessageHistoryOnly and automatic checkpoints
- Application startup runs a PostgreSQL schema migrator to ensure required tables exist (table name: agent_checkpoints).

Relevant files:
- src/main/kotlin/Application.kt — app startup, DB migration, wiring
- src/main/kotlin/Frameworks.kt — Koog + JSON setup and routes
- src/main/kotlin/AgentService.kt — Koog agent configuration (Ollama + Postgres persistency)
- src/main/resources/static/index.html — minimal chat UI

## Running the project

### Option A: Docker Compose (recommended)
This starts three containers: the app, PostgreSQL, and Ollama.

1) Build and run:
   docker compose up --build

2) Open the UI:
   http://localhost:8080

Notes:
- docker-compose.yml pulls the “llama3.2:latest” model automatically and serves Ollama at http://ollama:11434.
- The app connects to Postgres with pre-set credentials and runs DB migrations on startup.

### Option B: Run locally (without Compose)
Prerequisites:
- JDK 17+
- A running PostgreSQL instance (see env vars below for defaults)
- A running Ollama instance with the model pulled:
  - Install: https://ollama.ai
  - Start server: ollama serve
  - Pull model: ollama pull llama3.2:latest

Environment variables (defaults shown):
- OLLAMA_BASE_URL=http://localhost:11434
- POSTGRES_HOST=localhost
- POSTGRES_PORT=5432
- POSTGRES_DB=agents
- POSTGRES_USER=agent_user
- POSTGRES_PASSWORD=agent_pass
- POSTGRES_JDBC_DRIVER=org.postgresql.Driver
- Optionally, override all of the above with a single JDBC URL:
  - POSTGRES_URL=jdbc:postgresql://localhost:5432/agents

Run the app:
- ./gradlew run
- Open http://localhost:8080

The app will migrate the DB schema automatically (creates/updates the agent_checkpoints table).

## API
Base URL: http://localhost:8080

- POST /ai/createChat
  - Response: { "chatId": "<uuid>", "history": [] }

- POST /ai/chat
  - Request: { "chatId": "<uuid>", "message": "Hello" }
  - Response: { "reply": "...", "history": [ {"role":"user","content":"...","timestamp":...}, {"role":"assistant","content":"...","timestamp":...} ] }

The frontend in static/index.html calls these endpoints for you, but you can also use curl or an HTTP client.

## Configuration notes
- Frontend: served from / (redirects to /static/index.html). It uses a dark theme.
- Model: AgentService uses OllamaModels.Meta.LLAMA_3_2 by default.
- Persistence: Koog Persistency uses PostgreSQL. The agent’s persistenceId equals the chatId, so state can be restored after restarts.

## Troubleshooting
- The UI loads but replies fail:
  - Make sure Ollama is running and the model is pulled; check OLLAMA_BASE_URL.
- Database errors on startup:
  - Ensure PostgreSQL is reachable using the configured credentials/URL.
- Port conflicts:
  - The app uses 8080, Ollama uses 11434, and Postgres 5432 by default. Adjust ports as needed.