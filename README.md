# ktor-sample

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                                     | Description                                            |
| ----------------------------------------------------------|-------------------------------------------------------- |
| [Routing](https://start.ktor.io/p/routing)               | Provides a structured routing DSL                      |
| [Static Content](https://start.ktor.io/p/static-content) | Serves static files from defined locations             |
| [Koog](https://start.ktor.io/p/koog)                     | Integrate LLMs and build AI Agents with Koog framework |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```



## Docker and Docker Compose

Build and run the app with Docker Compose:

```
docker compose up --build
```

Then open http://localhost:8080 in your browser. You should see a "Hello World!" response at the root path.

Notes:
- The app includes AI integrations (Koog) that reference external LLM providers. The provided docker-compose.yml exposes environment variables for API keys, but the current code uses placeholder values and does not yet read them. The basic routes (like "/") will work without any keys.
- If you want to experiment with Ollama locally, you can enable the optional ollama service in docker-compose.yml and update the application to use the service URL http://ollama:11434 (instead of localhost) from inside the container.
