package com.linogo.gestion.security.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import com.linogo.gestion.security.application.AuthResponse
import com.linogo.gestion.security.application.LoginRequest
import com.linogo.gestion.security.application.RegisterRequest
import com.linogo.gestion.security.application.toUserResponse
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.security.infrastructure.RefreshTokenRepository
import com.linogo.gestion.security.infrastructure.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userDetailsService: CustomUserDetailsService,
    private val jdbcTemplate: JdbcTemplate,
    @param:Value("\${app.rate-limit.login-attempts}") private val maxAttempts: Int,
    @param:Value("\${app.rate-limit.window-minutes}") private val windowMinutes: Long
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    // 1. Usar ConcurrentHashMap para evitar problemas de concurrencia
    // 2. Tipar explícitamente <String, Bucket> para evitar el error de inferencia <K, V>
    private val loginAttempts: MutableMap<String, Bucket> = ConcurrentHashMap<String, Bucket>()

    fun login(request: LoginRequest, clientIp: String): AuthResponse {
        checkRateLimit(clientIp)

        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password)
            )
        } catch (ex: Exception) {
            logger.warn("Intento de login fallido para usuario: ${request.username}")
            throw BadCredentialsException("Credenciales inválidas")
        }

        val user = userRepository.findByUsername(request.username)
            ?: throw BadCredentialsException("Usuario no encontrado")

        if (!user.isEnabled()) {
            throw BadCredentialsException("Usuario deshabilitado")
        }

        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = createRefreshToken(user)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toUserResponse()
        )
    }

    // ... (El resto de tus métodos register, refreshToken, logout se mantienen igual)

    @Transactional
    fun register(request: RegisterRequest, clientIp: String): AuthResponse {
        checkRateLimit(clientIp)
        if (userDetailsService.existsByUsername(request.username)) {
            throw IllegalArgumentException("El username ya está en uso")
        }
        if (userDetailsService.existsByEmail(request.email)) {
            throw IllegalArgumentException("El email ya está registrado")
        }

        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password!!),
            fullName = request.fullName,
            roles = mutableSetOf(),
            _isEnabled = true
        )

        val savedUser = userRepository.save(user)
        val accessToken = jwtTokenProvider.generateAccessToken(savedUser)
        val refreshToken = createRefreshToken(savedUser)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = savedUser.toUserResponse()
        )
    }

    fun refreshToken(refreshToken: String): AuthResponse {
        // Via JPA (no reconstrucción manual desde SQL crudo): con roles
        // muchos-a-muchos no hay forma sana de replicar ese join a mano, y
        // RefreshToken.user ya viene hidratado con sus roles (EAGER).
        val stored = refreshTokenRepository.findByToken(refreshToken)
            ?: throw IllegalArgumentException("Refresh token inválido")

        val user = stored.user
        val userId = user.id!!

        if (stored.isRevoked) {
            revokeAllUserTokens(userId)
            throw IllegalArgumentException("Refresh token revocado")
        }

        if (stored.expiryDate.isBefore(Instant.now())) {
            revokeAllUserTokens(userId)
            throw IllegalArgumentException("Refresh token expirado")
        }

        revokeAllUserTokens(userId)
        val newRefreshToken = createRefreshToken(user)
        val newAccessToken = jwtTokenProvider.generateAccessToken(user)

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toUserResponse()
        )
    }

    fun logout(userId: String) {
        revokeAllUserTokens(userId)
    }

    private fun createRefreshToken(user: User): String {
        val userId = user.id!!
        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val expiryDate = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        val tokenUuid = java.util.UUID.randomUUID().toString()
        
        // Use JdbcTemplate to completely bypass Hibernate persistence context
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId)
        jdbcTemplate.update(
            "INSERT INTO refresh_tokens (id, user_id, token, expiry_date, is_revoked, created_at) " +
            "VALUES (?, ?, ?, ?, false, NOW())",
            tokenUuid, userId, refreshToken, java.sql.Timestamp.from(expiryDate)
        )
        
        return refreshToken
    }

    private fun revokeAllUserTokens(userId: String) {
        jdbcTemplate.update("UPDATE refresh_tokens SET is_revoked = true WHERE user_id = ?", userId)
    }

    // --- CORRECCIONES DE BUCKET4J 8.x ---

    private fun checkRateLimit(clientIp: String) {
        val bucket = loginAttempts.computeIfAbsent(clientIp) {
            createBucket()
        }

        // tryConsume(1) sigue siendo válido en la 8.x
        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit excedido para IP: $clientIp")
            throw RuntimeException("Demasiados intentos de login. Intente en 1 minuto.")
        }
    }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.builder()
            .capacity(maxAttempts.toLong())
            .refillGreedy(maxAttempts.toLong(), Duration.ofMinutes(windowMinutes))
            .build()

        return Bucket.builder()
            .addLimit(limit)
            .build()
    }
}