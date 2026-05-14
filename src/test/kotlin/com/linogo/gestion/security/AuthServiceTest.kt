package com.linogo.gestion.security

import com.linogo.gestion.security.application.LoginRequest
import com.linogo.gestion.security.application.RefreshTokenRequest
import com.linogo.gestion.security.application.RegisterRequest
import com.linogo.gestion.security.domain.RefreshToken
import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.security.infrastructure.RefreshTokenRepository
import com.linogo.gestion.security.infrastructure.UserRepository
import com.linogo.gestion.security.service.AuthService
import com.linogo.gestion.security.service.CustomUserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var userDetailsService: CustomUserDetailsService

    @InjectMocks
    private lateinit var authService: AuthService

    private lateinit var user: User
    private lateinit var loginRequest: LoginRequest
    private lateinit var registerRequest: RegisterRequest
    private lateinit var refreshToken: RefreshToken

    @BeforeEach
    fun setUp() {
        user = User(
            id = "user_id_123",
            username = "testuser",
            email = "test@example.com",
            password = "encoded_password",
            fullName = "Test User",
            role = Role.USER,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        loginRequest = LoginRequest(username = "testuser", password = "password123")

        registerRequest = RegisterRequest(
            fullName = "New User",
            username = "newuser",
            email = "new@example.com",
            password = "password123"
        )

        refreshToken = RefreshToken(
            id = "token_id_123",
            user = user,
            token = "refresh_token_value",
            expiryDate = Instant.now().plusSeconds(3600),
            isRevoked = false
        )
    }

    @Test
    fun `login should return AuthResponse when credentials are valid`() {
        `when`(authenticationManager.authenticate(any())).thenReturn(null)
        `when`(userRepository.findByUsername("testuser")).thenReturn(user)
        `when`(jwtTokenProvider.generateAccessToken(user)).thenReturn("access_token")
        `when`(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh_token")
        `when`(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L)

        val response = authService.login(loginRequest, "127.0.0.1")

        assertNotNull(response)
        assertEquals("access_token", response.accessToken)
        assertEquals("refresh_token", response.refreshToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals(900L, response.expiresIn)
        assertEquals("testuser", response.user.username)
        assertEquals("test@example.com", response.user.email)
        verify(authenticationManager).authenticate(any())
        verify(userRepository).findByUsername("testuser")
    }

    @Test
    fun `login should throw BadCredentialsException when credentials are invalid`() {
        `when`(authenticationManager.authenticate(any()))
            .thenThrow(BadCredentialsException("Credenciales inválidas"))

        val exception = assertThrows(BadCredentialsException::class.java) {
            authService.login(loginRequest, "127.0.0.1")
        }

        assertEquals("Credenciales inválidas", exception.message)
    }

    @Test
    fun `login should throw BadCredentialsException when user not found`() {
        `when`(authenticationManager.authenticate(any())).thenReturn(null)
        `when`(userRepository.findByUsername("testuser")).thenReturn(null)

        val exception = assertThrows(BadCredentialsException::class.java) {
            authService.login(loginRequest, "127.0.0.1")
        }

        assertEquals("Usuario no encontrado", exception.message)
    }

    @Test
    fun `register should return AuthResponse when data is valid`() {
        `when`(userDetailsService.existsByUsername("newuser")).thenReturn(false)
        `when`(userDetailsService.existsByEmail("new@example.com")).thenReturn(false)
        `when`(passwordEncoder.encode("password123")).thenReturn("encoded_password")
        `when`(userRepository.save(any())).thenReturn(user)
        `when`(jwtTokenProvider.generateAccessToken(any())).thenReturn("access_token")
        `when`(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh_token")
        `when`(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L)

        val response = authService.register(registerRequest)

        assertNotNull(response)
        assertEquals("access_token", response.accessToken)
        assertEquals("testuser", response.user.username)
        verify(userRepository).save(any())
    }

    @Test
    fun `register should throw IllegalArgumentException when username already exists`() {
        `when`(userDetailsService.existsByUsername("newuser")).thenReturn(true)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.register(registerRequest)
        }

        assertEquals("El username ya está en uso", exception.message)
    }

    @Test
    fun `register should throw IllegalArgumentException when email already exists`() {
        `when`(userDetailsService.existsByUsername("newuser")).thenReturn(false)
        `when`(userDetailsService.existsByEmail("new@example.com")).thenReturn(true)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.register(registerRequest)
        }

        assertEquals("El email ya está registrado", exception.message)
    }

    @Test
    fun `refreshToken should return new tokens when refresh token is valid`() {
        `when`(refreshTokenRepository.findByToken("valid_refresh_token")).thenReturn(refreshToken)
        `when`(jwtTokenProvider.generateAccessToken(user)).thenReturn("new_access_token")
        `when`(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new_refresh_token")
        `when`(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L)

        val response = authService.refreshToken("valid_refresh_token")

        assertNotNull(response)
        assertEquals("new_access_token", response.accessToken)
        assertEquals("new_refresh_token", response.refreshToken)
        verify(refreshTokenRepository).delete(refreshToken)
    }

    @Test
    fun `refreshToken should throw IllegalArgumentException when token not found`() {
        `when`(refreshTokenRepository.findByToken("invalid_token")).thenReturn(null)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.refreshToken("invalid_token")
        }

        assertEquals("Refresh token inválido", exception.message)
    }

    @Test
    fun `refreshToken should throw IllegalArgumentException when token is revoked`() {
        val revokedToken = refreshToken.copy(isRevoked = true)
        `when`(refreshTokenRepository.findByToken("revoked_token")).thenReturn(revokedToken)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.refreshToken("revoked_token")
        }

        assertEquals("Refresh token revocado", exception.message)
    }

    @Test
    fun `refreshToken should throw IllegalArgumentException when token is expired`() {
        val expiredToken = refreshToken.copy(expiryDate = Instant.now().minusSeconds(3600))
        `when`(refreshTokenRepository.findByToken("expired_token")).thenReturn(expiredToken)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.refreshToken("expired_token")
        }

        assertEquals("Refresh token expirado", exception.message)
        verify(refreshTokenRepository).delete(expiredToken)
    }

    @Test
    fun `logout should revoke user tokens when user has refresh token`() {
        `when`(refreshTokenRepository.findByUserId(any())).thenReturn(refreshToken)

        authService.logout("user_id")

        verify(refreshTokenRepository).findByUserId(any())
        verify(refreshTokenRepository).save(refreshToken)
    }

    @Test
    fun `logout should do nothing when user has no refresh token`() {
        `when`(refreshTokenRepository.findByUserId(any())).thenReturn(null)

        authService.logout("user_id")

        verify(refreshTokenRepository).findByUserId(any())
    }
}
