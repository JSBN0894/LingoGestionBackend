# Build stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring profile activo (puede ser sobrescrito por variable de entorno en Railway)
ENV SPRING_PROFILES_ACTIVE=prod

# Script de entrada que convierte DATABASE_URL al formato JDBC
# Usamos un enfoque que funciona en tiempo de ejecución
RUN echo '#!/bin/sh' > /app/entrypoint.sh && \
    echo 'set -e' >> /app/entrypoint.sh && \
    echo 'if [ -n "$DATABASE_URL" ] && [ -z "$SPRING_DATASOURCE_URL" ]; then' >> /app/entrypoint.sh && \
    echo '    export SPRING_DATASOURCE_URL="jdbc:$DATABASE_URL"' >> /app/entrypoint.sh && \
    echo '    echo "Database URL configurada: $SPRING_DATASOURCE_URL"' >> /app/entrypoint.sh && \
    echo 'fi' >> /app/entrypoint.sh && \
    echo 'exec java -jar app.jar' >> /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
