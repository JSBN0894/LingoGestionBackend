package com.linogo.gestion.security

import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.security.infrastructure.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JwtTokenProviderTest {

    private lateinit var tokenProvider: JwtTokenProvider
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        tokenProvider = JwtTokenProvider(
            "9a452d12b07e4c6c9b3a5b0c8d1e2f3g4h5i6j7k8l9m0n1o2p3q4r5s6t7u8v9w0x1y2z3A4B5C6D7E8F9G0H1I2J3K4L5M6N7O8P9Q0R1S2T3U4V5W6X7Y8Z",
            900000L,
            604800000L
        )
        user = User(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            fullName = "Test User",
            role = Role.USER,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `generateAccessToken should return a valid JWT token`() {
        val token = tokenProvider.generateAccessToken(user)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertEquals(TokenType.ACCESS, tokenProvider.getTokenType(token))
        assertTrue(tokenProvider.isTokenValid(token))
    }

    @Test
    fun `generateRefreshToken should return a valid JWT token`() {
        val token = tokenProvider.generateRefreshToken(user)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertEquals(TokenType.REFRESH, tokenProvider.getTokenType(token))
        assertTrue(tokenProvider.isTokenValid(token))
    }

    @Test
    fun `getUsernameFromToken should return the username`() {
        val token = tokenProvider.generateAccessToken(user)

        val username = tokenProvider.getUsernameFromToken(token)

        assertEquals("testuser", username)
    }

    @Test
    fun `isTokenValid should return false for expired token`() {
        val expiredProvider = JwtTokenProvider(
            "9a452d12b07e4c6c9b3a5b0c8d1e2f3g4h5i6j7k8l9m0n1o2p3q4r5s6t7u8v9w0x1y2z3A4B5C6D7E8F9G0H1I2J3K4L5M6N7O8P9Q0R1S2T3U4V5W6X7Y8Z",
            -1000L,
            604800000L
        )
        val token = expiredProvider.generateAccessToken(user)

        assertFalse(expiredProvider.isTokenValid(token))
    }

    @Test
    fun `isTokenValid should return false for malformed token`() {
        assertFalse(tokenProvider.isTokenValid("malformed-token"))
    }

    @Test
    fun `getAccessTokenExpirationMs should return configured value`() {
        assertEquals(900000L, tokenProvider.getAccessTokenExpirationMs())
    }
}
