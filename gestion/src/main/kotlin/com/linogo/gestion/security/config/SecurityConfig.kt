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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
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
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS configurado correctamente
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            
            // Manejo de excepciones
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { request, response, authException ->
                        response.status = 401
                        response.contentType = "application/json"
                        response.writer.write("""{"error": "Unauthorized", "message": "Autenticación requerida"}""")
                    }
                    .accessDeniedHandler { request, response, accessDeniedException ->
                        response.status = 403
                        response.contentType = "application/json"
                        response.writer.write("""{"error": "Forbidden", "message": "Acceso denegado"}""")
                    }
            }
            
            // Sesión stateless - no se crea sesión HTTP
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            
            // Autorizaciones
            .authorizeHttpRequests { auth ->
                // Endpoints públicos
                auth.requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/api/sync/**"
                ).permitAll()
                
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
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder)
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
