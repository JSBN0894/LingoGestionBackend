# Build stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# Copy the entire project context (relative to rootDirectory if set in railway.json)
COPY . .

# Ensure gradlew is executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Define port (Railway uses PORT env var, but we expose 8080 as well)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
