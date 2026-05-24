package com.linogo.gestion.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.security.application.AuthResponse
import com.linogo.gestion.security.application.UserResponse
import com.linogo.gestion.security.infrastructure.AuthController
import com.linogo.gestion.security.infrastructure.GlobalExceptionHandler
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.security.infrastructure.UserRepository
import com.linogo.gestion.security.service.AuthService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        val auth = UsernamePasswordAuthenticationToken(
            User("admin", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `POST login should return 200 when credentials are valid`() {
        val response = AuthResponse(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiresIn = 900L,
            user = UserResponse(id = "user_1", username = "testuser", email = "test@example.com",
                fullName = "Test User", role = "USER")
        )
        org.mockito.Mockito.`when`(authService.login(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn(response)

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username": "testuser", "password": "password123"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access_token"))
            .andExpect(jsonPath("$.user.username").value("testuser"))
    }

    @Test
    fun `POST login should return 400 when username is blank`() {
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username": "", "password": "password123"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST login should return 400 when password is too short`() {
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username": "testuser", "password": "123"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST register should return 200 when data is valid`() {
        val response = AuthResponse(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiresIn = 900L,
            user = UserResponse(id = "user_1", username = "newuser", email = "new@example.com",
                fullName = "New User", role = "USER")
        )
        org.mockito.Mockito.`when`(authService.register(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn(response)

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"fullName": "New User", "username": "newuser", "email": "new@example.com", "password": "password123"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.username").value("newuser"))
    }

    @Test
    fun `POST register should return 400 when email is invalid`() {
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"fullName": "New User", "username": "newuser", "email": "not-an-email", "password": "password123"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST register should return 400 when password is too short`() {
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"fullName": "New User", "username": "newuser", "email": "new@example.com", "password": "short"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST refresh should return 200 when token is valid`() {
        val response = AuthResponse(
            accessToken = "new_access_token",
            refreshToken = "new_refresh_token",
            expiresIn = 900L,
            user = UserResponse(id = "user_1", username = "testuser", email = "test@example.com",
                fullName = "Test User", role = "USER")
        )
        org.mockito.Mockito.`when`(authService.refreshToken("valid_refresh_token"))
            .thenReturn(response)

        mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"refreshToken": "valid_refresh_token"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new_access_token"))
    }

    @Test
    fun `POST refresh should return 400 when token is invalid`() {
        org.mockito.Mockito.`when`(authService.refreshToken("invalid_token"))
            .thenThrow(IllegalArgumentException("Refresh token inválido"))

        mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"refreshToken": "invalid_token"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST logout should return 200`() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Sesión cerrada exitosamente"))
    }
}
