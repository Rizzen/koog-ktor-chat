# Multi-stage Dockerfile for Ktor app

# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and build scripts first (better layer caching)
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy source
COPY src ./src

# Build a self-contained distribution (bin + libs)
RUN ./gradlew --no-daemon clean installDist

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Install wget for healthcheck compatibility (docker-compose uses wget)
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

# Copy the built distribution from the build stage
# The directory name equals the Gradle project name (see settings.gradle.kts)
COPY --from=build /app/build/install/ktor-sample /app

# Expose Ktor HTTP port
EXPOSE 8080

# Allow passing extra JVM args via JAVA_OPTS if needed
ENV JAVA_OPTS=""

# Start the app
CMD ["/bin/sh", "-lc", "./bin/ktor-sample $JAVA_OPTS"]
