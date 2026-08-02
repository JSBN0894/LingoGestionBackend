package com.linogo.gestion.security.config

import com.linogo.gestion.security.domain.Permission
import com.linogo.gestion.security.infrastructure.CookieJwtFilter
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
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
class CorsConfig {

    @Bean
    @Primary
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            val allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS")
                ?: "https://tu-dominio.com,https://app.tu-dominio.com,android-app://com.linogo.app"

            allowedOrigins = allowedOriginsEnv.split(",").map { it.trim() }

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
                "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers", "X-CSRF-Token"
            )
            exposedHeaders = listOf(
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Authorization", "Set-Cookie"
            )
            allowCredentials = true
            maxAge = 3600
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class ApiSecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsConfigurationSource: CorsConfigurationSource,
    private val cookieJwtFilter: CookieJwtFilter
) {

    @Bean
    @Order(2)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfigurationSource) }
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
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/sync/**",
                    "/admin/**"
                ).permitAll()

                auth.requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs",
                    "/api-docs/**", "/api-docs", "/api-docs/swagger-config",
                    "/swagger-resources/**", "/webjars/**"
                ).hasAuthority("PERM_${Permission.SYSTEM_DOCS_VIEW.code}")

                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(cookieJwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers ->
                headers
                    .cacheControl { cache -> cache.disable() }
                    .httpStrictTransportSecurity { hsts ->
                        hsts.maxAgeInSeconds(31536000).includeSubDomains(true)
                    }
                    .frameOptions { frame -> frame.deny() }
                    .contentTypeOptions { }
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:;")
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
}
