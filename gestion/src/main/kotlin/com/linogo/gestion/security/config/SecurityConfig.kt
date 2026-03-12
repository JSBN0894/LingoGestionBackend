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
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    @org.springframework.beans.factory.annotation.Qualifier("handlerExceptionResolver")
    private val resolver: org.springframework.web.servlet.HandlerExceptionResolver
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF deshabilitado para API REST stateless
            .csrf { csrf -> csrf.disable() }
            
            // CORS configurado correctamente
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            
            // Manejo de excepciones centralizado
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { request, response, authException ->
                        resolver.resolveException(request, response, null, authException)
                    }
                    .accessDeniedHandler { request, response, accessDeniedException ->
                        resolver.resolveException(request, response, null, accessDeniedException)
                    }
            }
            
            // Sesión stateless - no se crea sesión HTTP
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            
            // Autorizaciones
            .authorizeHttpRequests { auth ->
                // Endpoints públicos - login, sync y Swagger UI
                auth.requestMatchers(
                    "/api/auth/login",
                    "/api/sync/**",
                    // Swagger UI debe ser público para poder autenticarse desde allí
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/api-docs/**"
                ).permitAll()

                // Registro solo para ADMIN
                auth.requestMatchers("/api/auth/register").hasRole("ADMIN")

                // Refresh token requiere autenticación
                auth.requestMatchers("/api/auth/refresh").authenticated()

                // Requests OPTIONS para preflight CORS
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Todo lo demás requiere autenticación
                auth.anyRequest().authenticated()
            }
            
            // Filtro JWT antes del filtro de autenticación por username/password
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            
            // Deshabilitar cache para respuestas
            .headers { headers ->
                headers.cacheControl { cache -> cache.disable() }
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
            // En producción, especificar orígenes permitidos explícitamente
            allowedOrigins = listOf(
                "https://tu-dominio.com",
                "https://app.tu-dominio.com",
                "android-app://com.linogo.app"
            )
            allowedOriginPatterns = listOf("*") // Solo para desarrollo
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
