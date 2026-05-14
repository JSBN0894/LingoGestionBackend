package com.linogo.gestion.security

import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.security.infrastructure.TokenType
import com.linogo.gestion.security.service.CustomUserDetailsService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var userDetailsService: CustomUserDetailsService

    @InjectMocks
    private lateinit var filter: JwtAuthenticationFilter

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `doFilter should set authentication when valid access token is provided`() {
        val user = User(username = "testuser", email = "test@example.com",
            password = "password", fullName = "Test User", role = Role.USER,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        `when`(request.getHeader("Authorization")).thenReturn("Bearer valid_token")
        `when`(jwtTokenProvider.isTokenValid("valid_token")).thenReturn(true)
        `when`(jwtTokenProvider.getTokenType("valid_token")).thenReturn(TokenType.ACCESS)
        `when`(jwtTokenProvider.getUsernameFromToken("valid_token")).thenReturn("testuser")
        `when`(userDetailsService.loadUserByUsername("testuser")).thenReturn(user)

        filter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertTrue(authentication!!.isAuthenticated)
        assertTrue(authentication!!.name == "testuser")
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `doFilter should not set authentication when no Authorization header`() {
        `when`(request.getHeader("Authorization")).thenReturn(null)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `doFilter should not set authentication when token is invalid`() {
        `when`(request.getHeader("Authorization")).thenReturn("Bearer invalid_token")
        `when`(jwtTokenProvider.isTokenValid("invalid_token")).thenReturn(false)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `doFilter should not set authentication when token is refresh type`() {
        `when`(request.getHeader("Authorization")).thenReturn("Bearer refresh_token")
        `when`(jwtTokenProvider.isTokenValid("refresh_token")).thenReturn(true)
        `when`(jwtTokenProvider.getTokenType("refresh_token")).thenReturn(TokenType.REFRESH)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `doFilter should not set authentication when header does not start with Bearer`() {
        `when`(request.getHeader("Authorization")).thenReturn("Basic some_token")

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `doFilter should proceed with filter chain even when exception occurs`() {
        `when`(request.getHeader("Authorization")).thenReturn("Bearer valid_token")
        `when`(jwtTokenProvider.isTokenValid("valid_token")).thenThrow(RuntimeException("JWT error"))

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
    }
}
