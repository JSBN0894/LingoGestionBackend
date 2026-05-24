package com.linogo.gestion.security.application

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

data class UserMeResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String,
    val isEnabled: Boolean
)
