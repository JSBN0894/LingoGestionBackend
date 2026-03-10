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
# Usa 'prod' por defecto para producción
ENV SPRING_PROFILES_ACTIVE=prod

# Script para convertir DATABASE_URL de Railway a SPRING_DATASOURCE_URL
# Railway usa 'postgresql://' pero Spring necesita 'jdbc:postgresql://'
RUN printf '#!/bin/sh\nif [ -n "$DATABASE_URL" ] && [ -z "$SPRING_DATASOURCE_URL" ]; then\n    export SPRING_DATASOURCE_URL="jdbc:${DATABASE_URL}"\nfi\nexec java -jar app.jar\n' > /app/entrypoint.sh && chmod +x /app/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
