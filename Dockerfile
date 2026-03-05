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

# NOTA: SPRING_PROFILES_ACTIVE ahora se define como variable de entorno 
# en el Dashboard de Railway por cada ambiente (dev o prod)
# Por defecto caerá a "default" si no se define.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
