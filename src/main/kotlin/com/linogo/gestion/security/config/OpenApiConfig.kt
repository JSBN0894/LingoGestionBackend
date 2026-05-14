package com.linogo.gestion.security.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${app.name:Linogo Gestion API}")
    private val appName: String = "Linogo Gestion API"

    @Value("\${app.version:1.0.0}")
    private val appVersion: String = "1.0.0"

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("$appName - Documentación API")
                    .version(appVersion)
                    .description(
                        """
                        |API REST para gestión de pedidos y clientes.
                        |
                        |## 🔐 Autenticación
                        |
                        |Esta es una **aplicación privada**. Solo usuarios autorizados pueden acceder.
                        |
                        |### Primer acceso (Administrador)
                        |
                        |1. El usuario administrador se crea automáticamente al iniciar la aplicación (en perfil 'dev')
                        |2. Credenciales por defecto:
                        |   - **Username:** `admin`
                        |   - **Password:** `Admin@123456`
                        |3. **Importante:** Cambiar la contraseña después del primer login!
                        |
                        |### Flujo de autenticación
                        |
                        |1. Inicia sesión en `/api/auth/login` con tus credenciales
                        |2. Copia el `accessToken` de la respuesta
                        |3. Usa el botón 🔓 "Authorize" arriba para ingresar tu token
                        |4. El formato es: `Bearer <tu-access-token>`
                        |
                        |### Registro de usuarios
                        |
                        |⚠️ **Solo el ADMINISTRADOR puede crear usuarios.**
                        |
                        |El endpoint `/api/auth/register` requiere rol de ADMINISTRADOR.
                        |
                        |### Endpoints públicos (sin autenticación)
                        |
                        |- `POST /api/auth/login` - Login de usuarios
                        |- `/api/sync/**` - Sincronización
                        |
                        |### Endpoints protegidos
                        |
                        |- `POST /api/auth/register` - Solo ADMIN
                        |- `POST /api/auth/refresh` - Usuarios autenticados
                        |- `POST /api/auth/logout` - Usuarios autenticados
                        |- Todos los demás endpoints requieren autenticación
                        """.trimMargin()
                    )
                    .contact(
                        Contact()
                            .name("Linogo Support")
                            .email("support@linogo.com")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                    )
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("Bearer Authentication")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "Bearer Authentication",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Ingresa tu token JWT sin el prefijo 'Bearer '")
                    )
            )
    }
}
