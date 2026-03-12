# --- Etapa de Compilación ---
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copiar archivos de configuración de Gradle primero para aprovechar el cacheo
COPY build.gradle.kts settings.gradle.kts gradlew /app/
COPY gradle /app/gradle

# Descargar dependencias (esto acelera futuras compilaciones)
RUN ./gradlew dependencies --no-daemon

# Copiar el código fuente y compilar
COPY src /app/src
RUN ./gradlew bootJar --no-daemon

# --- Etapa de Ejecución ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Exponer el puerto por defecto de Spring Boot
EXPOSE 8080

# Copiar el JAR generado desde la etapa de compilación
COPY --from=build /app/build/libs/*.jar app.jar

# Configuración de variables de entorno por defecto (Local)
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
