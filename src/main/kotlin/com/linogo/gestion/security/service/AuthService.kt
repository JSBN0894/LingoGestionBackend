package com.linogo.gestion.security.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import com.linogo.gestion.security.application.AuthResponse
import com.linogo.gestion.security.application.LoginRequest
import com.linogo.gestion.security.application.RegisterRequest
import com.linogo.gestion.security.application.UserResponse
import com.linogo.gestion.security.domain.RefreshToken
import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.security.infrastructure.RefreshTokenRepository
import com.linogo.gestion.security.infrastructure.UserRepository
import org.slf4j.LoggerFactory
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
    @param:Value("\${app.rate-limit.login-attempts}") private val maxAttempts: Int,
    @param:Value("\${app.rate-limit.window-minutes}") private val windowMinutes: Long
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    // 1. Usar ConcurrentHashMap para evitar problemas de concurrencia
    // 2. Tipar explícitamente <String, Bucket> para evitar el error de inferencia <K, V>
    private val loginAttempts: MutableMap<String, Bucket> = ConcurrentHashMap<String, Bucket>()

    @Transactional
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
            role = Role.USER,
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
        refreshTokenRepository.delete(refreshTokenEntity)
        val newRefreshToken = createRefreshToken(user)
        val newAccessToken = jwtTokenProvider.generateAccessToken(user)

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toUserResponse()
        )
    }

    @Transactional
    fun logout(userId: String) {
        revokeAllUserTokens(userId)
    }

    fun createRefreshTokenPublic(user: User): String {
        return createRefreshToken(user)
    }

    private fun createRefreshToken(user: User): String {
        // Use deleteByUserId to avoid optimistic locking issues
        refreshTokenRepository.deleteByUserId(user.id!!)

        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val refreshTokenEntity = RefreshToken(
            id = java.util.UUID.randomUUID().toString(),
            user = user,
            token = refreshToken,
            expiryDate = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()),
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

private fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id ?: "",
        username = this.username,
        email = this.email,
        fullName = this.fullName,
        role = this.role.name,
        isEnabled = this.isEnabled()
    )
}