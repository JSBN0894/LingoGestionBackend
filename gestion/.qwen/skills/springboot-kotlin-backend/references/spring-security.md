# Spring Security con JWT

## Configuración Completa

## Dependencias (build.gradle.kts)

```kotlin
dependencies {
    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Validación
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
```

## Estructura de Archivos

```
security/
├── config/
│   └── SecurityConfig.kt          # Configuración principal de seguridad
├── domain/
│   ├── User.kt                     # Entidad de usuario
│   ├── RefreshToken.kt             # Entidad de refresh token
│   └── Role.kt                     # Enum de roles
├── infrastructure/
│   ├── JwtTokenProvider.kt         # Generación y validación de tokens
│   ├── JwtAuthenticationFilter.kt  # Filtro para validar tokens
│   ├── UserRepository.kt           # Repositorio de usuarios
│   └── RefreshTokenRepository.kt   # Repositorio de refresh tokens
├── service/
│   ├── AuthService.kt              # Lógica de autenticación
│   └── CustomUserDetailsService.kt # Implementación de UserDetailsService
└── application/
    ├── AuthDTOs.kt                 # DTOs para auth (LoginRequest, RegisterRequest, etc.)
    └── AuthResponse.kt             # Response de autenticación
```

## SecurityConfig.kt

```kotlin
package com.example.project.security.config

import com.example.project.security.infrastructure.JwtAuthenticationFilter
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
@EnableMethodSecurity(prePostEnabled = true)  // Habilita @PreAuthorize, @PostAuthorize
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val handlerExceptionResolver: org.springframework.web.servlet.HandlerExceptionResolver
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF deshabilitado para API stateless
            .csrf { it.disable() }

            // CORS configurado
            .cors { it.configurationSource(corsConfigurationSource()) }

            // Manejo de excepciones
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { request, response, authException ->
                        handlerExceptionResolver.resolveException(request, response, null, authException)
                    }
                    .accessDeniedHandler { request, response, accessDeniedException ->
                        handlerExceptionResolver.resolveException(request, response, null, accessDeniedException)
                    }
            }

            // Sesión stateless (sin session HTTP)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // Autorizaciones
            .authorizeHttpRequests { auth ->
                // Endpoints públicos
                auth.requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh"
                ).permitAll()

                // Swagger (opcional: proteger en producción)
                auth.requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).authenticated()

                // OPTIONS para CORS preflight
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Todo lo demás requiere autenticación
                auth.anyRequest().authenticated()
            }

            // Filtro JWT antes del filtro de autenticación
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

            // Headers de seguridad
            .headers { headers ->
                headers.frameOptions { frame -> frame.sameOrigin() }
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
            allowedOrigins = listOf("https://tu-dominio.com")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
```

## JwtTokenProvider.kt

```kotlin
package com.example.project.security.infrastructure

import com.example.project.security.domain.User
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}")
    private val jwtSecret: String,

    @Value("\${app.jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,

    @Value("\${app.jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    private val signingKey: Key by lazy { 
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)) 
    }

    fun generateAccessToken(user: User): String {
        return generateToken(user, accessTokenExpiration, TokenType.ACCESS)
    }

    fun generateRefreshToken(user: User): String {
        return generateToken(user, refreshTokenExpiration, TokenType.REFRESH)
    }

    private fun generateToken(user: UserDetails, expirationMs: Long, tokenType: TokenType): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(user.username)
            .claim("role", user.authorities.first().authority)
            .claim("type", tokenType.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        return getClaimsFromToken(token).subject
    }

    fun getRoleFromToken(token: String): String {
        return getClaimsFromToken(token).get("role", String::class.java)
    }

    fun getTokenType(token: String): TokenType? {
        val type = getClaimsFromToken(token).get("type", String::class.java)
        return try {
            TokenType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: SecurityException) {
            logger.warn("Token inválido: ${e.message}")
            false
        } catch (e: MalformedJwtException) {
            logger.warn("Token mal formado: ${e.message}")
            false
        } catch (e: ExpiredJwtException) {
            logger.warn("Token expirado: ${e.message}")
            false
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
    }

    fun getAccessTokenExpirationMs(): Long = accessTokenExpiration
}

enum class TokenType {
    ACCESS,
    REFRESH
}
```

## JwtAuthenticationFilter.kt

```kotlin
package com.example.project.security.infrastructure

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)

            if (token != null && jwtTokenProvider.isTokenValid(token)) {
                val username = jwtTokenProvider.getUsernameFromToken(token)

                val userDetails = userDetailsService.loadUserByUsername(username)

                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                ).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }

                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: Exception) {
            logger.error("Error al procesar token JWT: ${ex.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
```

## CustomUserDetailsService.kt

```kotlin
package com.example.project.security.service

import com.example.project.security.infrastructure.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("Usuario no encontrado: $username")

        if (!user.enabled) {
            throw UsernameNotFoundException("Usuario deshabilitado: $username")
        }

        return user
    }

    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }
}
```

## AuthService.kt

```kotlin
package com.example.project.security.service

import com.example.project.security.application.*
import com.example.project.security.domain.*
import com.example.project.security.infrastructure.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userDetailsService: CustomUserDetailsService
) {

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        // Autenticar
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val user = userRepository.findByUsername(request.username)
            ?: throw BadCredentialsException("Credenciales inválidas")

        if (!user.enabled) {
            throw BadCredentialsException("Usuario deshabilitado")
        }

        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = createRefreshToken(user)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toUserResponse()
        )
    }

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        // Validar que no exista
        if (userDetailsService.existsByUsername(request.username)) {
            throw IllegalArgumentException("El username ya está en uso")
        }
        if (userDetailsService.existsByEmail(request.email)) {
            throw IllegalArgumentException("El email ya está registrado")
        }

        // Crear usuario
        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            role = Role.USER,
            enabled = true
        )

        val savedUser = userRepository.save(user)

        // Generar tokens
        val accessToken = jwtTokenProvider.generateAccessToken(savedUser)
        val refreshToken = createRefreshToken(savedUser)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = savedUser.toUserResponse()
        )
    }

    @Transactional
    fun refreshToken(refreshToken: String): AuthResponse {
        val refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
            ?: throw IllegalArgumentException("Refresh token inválido")

        if (refreshTokenEntity.isRevoked) {
            revokeAllUserTokens(refreshTokenEntity.user.id!!)
            throw IllegalArgumentException("Refresh token revocado")
        }

        if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshTokenEntity)
            throw IllegalArgumentException("Refresh token expirado")
        }

        val user = refreshTokenEntity.user
        
        // Rotar refresh token
        refreshTokenRepository.delete(refreshTokenEntity)
        val newRefreshToken = createRefreshToken(user)
        val newAccessToken = jwtTokenProvider.generateAccessToken(user)

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toUserResponse()
        )
    }

    @Transactional
    fun logout(userId: String) {
        revokeAllUserTokens(userId)
    }

    private fun createRefreshToken(user: User): String {
        // Eliminar refresh token anterior si existe
        refreshTokenRepository.findByUserId(user.id)?.let {
            refreshTokenRepository.delete(it)
        }

        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val refreshTokenEntity = RefreshToken(
            user = user,
            token = refreshToken,
            expiryDate = Instant.now().plusMillis(
                jwtTokenProvider.getAccessTokenExpirationMs() * 7
            ),
            isRevoked = false
        )

        refreshTokenRepository.save(refreshTokenEntity)
        return refreshToken
    }

    private fun revokeAllUserTokens(userId: String) {
        refreshTokenRepository.findByUserId(userId)?.let {
            it.isRevoked = true
            refreshTokenRepository.save(it)
        }
    }
}

// Extension function para convertir User a UserResponse
private fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id ?: "",
        username = this.username,
        email = this.email,
        fullName = this.fullName,
        role = this.role.name
    )
}
```

## AuthDTOs.kt

```kotlin
package com.example.project.security.application

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "El username es requerido")
    @field:Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    val username: String,

    @field:NotBlank(message = "La contraseña es requerida")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "El nombre completo es requerido")
    @field:Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    val fullName: String,

    @field:NotBlank(message = "El username es requerido")
    @field:Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    val username: String,

    @field:NotBlank(message = "El email es requerido")
    @field:Email(message = "El email debe ser válido")
    val email: String,

    @field:NotBlank(message = "La contraseña es requerida")
    @field:Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "El refresh token es requerido")
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String
)
```

## Variables de Entorno (application.properties)

```properties
# JWT Configuration
app.jwt.secret=VGhpc0lzQVN1cGVyU2VjdXJlSldUU2VjcmV0S2V5VGhhdElzQXRMZWFzdDI1NkJpdHNMb25n
app.jwt.access-token-expiration=900000        # 15 minutos
app.jwt.refresh-token-expiration=604800000    # 7 días
```

## Uso en Controllers Protegidos

```kotlin
@RestController
@RequestMapping("/api/protected")
@SecurityRequirement(name = "Bearer Authentication")
class ProtectedController {

    @GetMapping("/user-info")
    fun getUserInfo(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "username" to userDetails.username,
            "roles" to userDetails.authorities.joinToString { it.authority }
        ))
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun adminEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Solo administradores")
    }
}
```
