package com.linogo.gestion.security.config

import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF deshabilitado para API REST stateless
            // CSRF no es necesario cuando se usa JWT en headers Authorization
            .csrf { csrf -> csrf.disable() }

            // CORS configurado correctamente
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }

            // Manejo de excepciones
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { request, response, authException ->
                        response.status = 401
                        response.contentType = "application/json"
                        response.writer.write("""{"error": "Unauthorized", "message": "Autenticacion requerida"}""")
                    }
                    .accessDeniedHandler { request, response, accessDeniedException ->
                        response.status = 403
                        response.contentType = "application/json"
                        response.writer.write("""{"error": "Forbidden", "message": "Acceso denegado"}""")
                    }
            }

            // Sesion stateless - no se crea sesion HTTP
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // Autorizaciones
            .authorizeHttpRequests { auth ->
                // Endpoints publicos
                auth.requestMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/sync/**"
                ).permitAll()

                // Swagger/OpenAPI - solo ADMIN
                auth.requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/api-docs/**",
                    "/api-docs",
                    "/api-docs/swagger-config",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).hasRole("ADMIN")

                // Requests OPTIONS para preflight CORS
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Todo lo demas requiere autenticacion
                auth.anyRequest().authenticated()
            }

            // Filtro JWT antes del filtro de autenticacion por username/password
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

            // Security headers + deshabilitar cache
            .headers { headers ->
                headers
                    .cacheControl { cache -> cache.disable() }
                    .httpStrictTransportSecurity { hsts ->
                        hsts
                            .maxAgeInSeconds(31536000)
                            .includeSubDomains(true)
                    }
                    .frameOptions { frame ->
                        frame.deny()
                    }
                    .contentTypeOptions { } // X-Content-Type-Options: nosniff
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives("default-src 'self'")
                    }
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(
        userDetailsService: org.springframework.security.core.userdetails.UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationManager {
        val authenticationProvider = DaoAuthenticationProvider().apply {
            setPasswordEncoder(passwordEncoder)
            setUserDetailsService(userDetailsService)
        }
        return ProviderManager(authenticationProvider)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // Origenes permitidos - configurar mediante variable de entorno en produccion
            val allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS")
                ?: "https://tu-dominio.com,https://app.tu-dominio.com,android-app://com.linogo.app"

            allowedOrigins = allowedOriginsEnv.split(",").map { it.trim() }

            // allowedOriginPatterns solo para desarrollo (no usar en produccion)
            val isDev = System.getenv("SPRING_PROFILES_ACTIVE") == "dev"
            if (isDev) {
                allowedOriginPatterns = listOf(
                    "http://localhost:*",
                    "http://10.0.2.2:*",
                    "http://127.0.0.1:*",
                    "http://localhost:5173"
                )
            }

            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
            )
            exposedHeaders = listOf(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Authorization"
            )
            allowCredentials = true
            maxAge = 3600 // 1 hora
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
