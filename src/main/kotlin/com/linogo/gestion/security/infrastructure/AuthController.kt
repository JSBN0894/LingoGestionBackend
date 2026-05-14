package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.application.AuthResponse
import com.linogo.gestion.security.application.LoginRequest
import com.linogo.gestion.security.application.RefreshTokenRequest
import com.linogo.gestion.security.application.RegisterRequest
import com.linogo.gestion.security.config.AdminOnly
import com.linogo.gestion.security.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para login, registro y gestión de tokens")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve access token y refresh token")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val clientIp = getClientIp(request)
        return ResponseEntity.ok(authService.login(loginRequest, clientIp))
    }

    @PostMapping("/register")
    @AdminOnly
    @Operation(summary = "Registrar usuario (solo ADMIN)", description = "Crea un nuevo usuario y devuelve tokens de acceso")
    @ApiResponse(responseCode = "200", description = "Registro exitoso")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existe")
    @ApiResponse(responseCode = "403", description = "No tiene permisos de administrador")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val clientIp = getClientIp(httpRequest)
        return ResponseEntity.ok(authService.register(request, clientIp))
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Obtiene un nuevo access token usando el refresh token")
    @ApiResponse(responseCode = "200", description = "Token refrescado exitosamente")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken))
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida los tokens del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Logout exitoso")
    fun logout(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, String>> {
        // En producción, obtener el userId del token o de la base de datos
        authService.logout(userDetails.username)
        return ResponseEntity.ok(mapOf("message" to "Sesión cerrada exitosamente"))
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xfHeader = request.getHeader("X-Forwarded-For")
        return if (xfHeader != null) {
            xfHeader.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
