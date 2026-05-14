package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.application.UserResponse
import com.linogo.gestion.security.config.AdminOnly
import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.service.CustomUserDetailsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Endpoints exclusivos para administradores")
@SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userDetailsService: CustomUserDetailsService
) {
    private val log = LoggerFactory.getLogger(AdminController::class.java)

    @GetMapping("/users")
    @AdminOnly
    @Operation(summary = "Listar usuarios", description = "Solo administradores pueden acceder")
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        val users = userRepository.findAll().map { it.toUserResponse() }
        return ResponseEntity.ok(users)
    }

    @GetMapping("/users/{id}")
    @AdminOnly
    @Operation(summary = "Obtener usuario por ID")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User with id $id not found") }
        return ResponseEntity.ok(user.toUserResponse())
    }

    @PostMapping("/users")
    @AdminOnly
    @Operation(summary = "Crear usuario con rol específico")
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        if (userDetailsService.existsByUsername(request.username)) {
            throw IllegalArgumentException("El username ya está en uso")
        }
        if (userDetailsService.existsByEmail(request.email)) {
            throw IllegalArgumentException("El email ya está registrado")
        }

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            role = request.role,
            isEnabled = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val saved = userRepository.save(user)
        log.info("ADMIN creó usuario: ${saved.username} con rol ${saved.role}")
        return ResponseEntity.ok(saved.toUserResponse())
    }

    @PatchMapping("/users/{id}/role")
    @AdminOnly
    @Operation(summary = "Cambiar rol de un usuario")
    fun updateUserRole(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateRoleRequest
    ): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User with id $id not found") }

        val previousRole = user.role
        user.role = request.role
        user.updatedAt = LocalDateTime.now()
        val saved = userRepository.save(user)
        log.info("ADMIN cambió rol de ${saved.username}: $previousRole → ${saved.role}")
        return ResponseEntity.ok(saved.toUserResponse())
    }

    @DeleteMapping("/users/{userId}")
    @AdminOnly
    @Operation(summary = "Eliminar usuario")
    fun deleteUser(
        @PathVariable userId: String,
        @AuthenticationPrincipal currentUser: UserDetails
    ): ResponseEntity<Map<String, String>> {
        if (!userRepository.existsById(userId)) {
            throw IllegalArgumentException("User with id $userId not found")
        }
        val target = userRepository.findById(userId).get()
        if (currentUser.username == target.username) {
            throw IllegalArgumentException("No puedes eliminarte a ti mismo")
        }
        val deletedUsername = target.username
        userRepository.deleteById(userId)
        log.warn("ADMIN eliminó usuario: $deletedUsername (id: $userId)")
        return ResponseEntity.ok(mapOf("message" to "Usuario $deletedUsername eliminado"))
    }

    private fun User.toUserResponse(): UserResponse {
        return UserResponse(
            id = this.id ?: "",
            username = this.username,
            email = this.email,
            fullName = this.fullName,
            role = this.role.name
        )
    }
}

data class CreateUserRequest(
    @field:NotBlank @field:Size(min = 2, max = 100) val fullName: String,
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 8, max = 100) val password: String,
    val role: Role = Role.USER
)

data class UpdateRoleRequest(
    val role: Role
)
