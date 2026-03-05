package com.linogo.gestion.security.service

import com.bucket4j.Bandwidth
import com.bucket4j.Bucket
import com.bucket4j.Bucket4j
import com.bucket4j.Refill
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
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
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

    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    
    // Rate limiting buckets por IP
    private val loginAttempts: MutableMap<String, Bucket> = mutableMapOf()

    @Transactional
    fun login(request: LoginRequest, clientIp: String): AuthResponse {
        // Verificar rate limiting
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

        if (!user.isEnabled) {
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

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        // Validar que no exista
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
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            role = Role.USER,
            isEnabled = true
        )

        val savedUser = userRepository.save(user)
        logger.info("Usuario registrado: ${savedUser.username}")

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
            revokeAllUserTokens(refreshTokenEntity.user.id)
            throw IllegalArgumentException("Refresh token revocado")
        }

        if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshTokenEntity)
            throw IllegalArgumentException("Refresh token expirado")
        }

        val user = refreshTokenEntity.user
        
        // Revocar token anterior y crear uno nuevo (rotation)
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
        logger.info("Logout realizado para usuario: $userId")
    }

    private fun createRefreshToken(user: User): String {
        // Eliminar refresh token existente si hay
        refreshTokenRepository.findByUserId(user.id)?.let {
            refreshTokenRepository.delete(it)
        }

        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val refreshTokenEntity = RefreshToken(
            id = java.util.UUID.randomUUID().toString(),
            user = user,
            token = refreshToken,
            expiryDate = Instant.now().plusMillis(jwtTokenProvider.getAccessTokenExpirationMs() * 7), // 7 días
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

    // Rate limiting: 5 intentos por minuto por IP
    private fun checkRateLimit(clientIp: String) {
        val bucket = loginAttempts.computeIfAbsent(clientIp) {
            createBucket()
        }

        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit excedido para IP: $clientIp")
            throw RuntimeException("Demasiados intentos de login. Intente en 1 minuto.")
        }
    }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.simple(5, Duration.ofMinutes(1))
        return Bucket4j.builder()
            .addLimit(limit)
            .build()
    }
}

private fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id,
        username = this.username,
        email = this.email,
        fullName = this.fullName,
        role = this.role.name
    )
}
