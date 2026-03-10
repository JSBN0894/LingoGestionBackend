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

# Exponemos el puerto
EXPOSE 8080

# Comando de inicio directo, confiamos en Spring Boot para manejar variables de entorno
ENTRYPOINT ["java", "-jar", "app.jar"]
