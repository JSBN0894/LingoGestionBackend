package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.application.AuditLogEntry
import com.linogo.gestion.security.application.AuditLogPageResponse
import com.linogo.gestion.security.application.UserResponse
import com.linogo.gestion.security.application.toUserResponse
import com.linogo.gestion.security.config.Audited
import com.linogo.gestion.security.config.RequiresPermission
import com.linogo.gestion.security.domain.Permission
import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.AuditLogJpaRepository
import com.linogo.gestion.security.service.CustomUserDetailsService
import com.linogo.gestion.security.service.RoleGuardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Endpoints exclusivos para administradores")
@SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userDetailsService: CustomUserDetailsService,
    private val auditLogRepository: AuditLogJpaRepository,
    private val refreshTokenRepository: RefreshTokenJpaRepository,
    private val roleGuardService: RoleGuardService
) {
    private val log = LoggerFactory.getLogger(AdminController::class.java)

    @GetMapping("/users")
    @RequiresPermission(Permission.USERS_MANAGE)
    @Operation(summary = "Listar usuarios")
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        val users = userRepository.findAll().map { it.toUserResponse() }
        return ResponseEntity.ok(users)
    }

    @GetMapping("/users/{id}")
    @RequiresPermission(Permission.USERS_MANAGE)
    @Operation(summary = "Obtener usuario por ID")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User with id $id not found") }
        return ResponseEntity.ok(user.toUserResponse())
    }

    @PostMapping("/users")
    @RequiresPermission(Permission.USERS_MANAGE)
    @Audited(action = "CREATE", entityType = "USER")
    @Operation(summary = "Crear usuario con roles específicos")
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        if (userDetailsService.existsByUsername(request.username)) {
            throw IllegalArgumentException("El username ya está en uso")
        }
        if (userDetailsService.existsByEmail(request.email)) {
            throw IllegalArgumentException("El email ya está registrado")
        }
        val roles = resolveRoles(request.roleIds)

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            roles = roles,
            _isEnabled = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val saved = userRepository.save(user)
        log.info("ADMIN creó usuario: ${saved.username} con roles ${roles.map { it.name }}")
        return ResponseEntity.ok(saved.toUserResponse())
    }

    @Transactional
    @PutMapping("/users/{id}/roles")
    @RequiresPermission(Permission.USERS_MANAGE)
    @Audited(action = "ROLES_CHANGE", entityType = "USER")
    @Operation(summary = "Cambiar los roles de un usuario", description = "Reemplaza el conjunto completo de roles asignados")
    fun updateUserRoles(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateUserRolesRequest
    ): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User with id $id not found") }

        user.roles = resolveRoles(request.roleIds)
        user.updatedAt = LocalDateTime.now()
        val saved = userRepository.save(user)
        roleGuardService.assertGuardPermissionRetained()
        log.info("ADMIN cambió roles de ${saved.username}: ${saved.roles.map { it.name }}")
        return ResponseEntity.ok(saved.toUserResponse())
    }

    @Transactional
    @DeleteMapping("/users/{userId}")
    @RequiresPermission(Permission.USERS_MANAGE)
    @Audited(action = "DELETE", entityType = "USER")
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
        roleGuardService.assertGuardPermissionRetained()
        log.warn("ADMIN eliminó usuario: $deletedUsername (id: $userId)")
        return ResponseEntity.ok(mapOf("message" to "Usuario $deletedUsername eliminado"))
    }

    @Transactional
    @PatchMapping("/users/{id}/status")
    @RequiresPermission(Permission.USERS_MANAGE)
    @Audited(action = "STATUS_TOGGLE", entityType = "USER")
    @Operation(summary = "Activar/desactivar usuario", description = "Alterna el estado enabled de un usuario")
    fun toggleUserStatus(
        @PathVariable id: String
    ): ResponseEntity<UserResponse> {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User with id $id not found") }
        val newStatus = user.toggleEnabled()
        val saved = userRepository.save(user)
        roleGuardService.assertGuardPermissionRetained()
        log.info("ADMIN toggled status of ${saved.username}: enabled=$newStatus")
        return ResponseEntity.ok(saved.toUserResponse())
    }

    private fun resolveRoles(roleIds: List<Long>): MutableSet<Role> {
        val roles = roleRepository.findAllById(roleIds).toMutableSet()
        if (roles.size != roleIds.toSet().size) {
            throw IllegalArgumentException("Uno o más roleIds no existen")
        }
        return roles
    }

    @Transactional
    @PatchMapping("/users/{id}/password")
    @RequiresPermission(Permission.USERS_MANAGE)
    // Nota: sin @Audited a propósito — AuditAspect serializa todos los argumentos
    // del método tal cual, y eso volcaría la nueva contraseña en texto plano
    // dentro de audit_logs. El log.info de abajo registra el evento sin el secreto.
    @Operation(summary = "Restablecer contraseña de un usuario", description = "El administrador fija una nueva contraseña sin necesitar la anterior. Cierra las sesiones activas del usuario.")
    fun resetUserPassword(
        @PathVariable id: String,
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<Map<String, String>> {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User with id $id not found") }

        user.changePassword(passwordEncoder.encode(request.newPassword))
        userRepository.save(user)
        refreshTokenRepository.deleteByUserId(id)

        log.info("ADMIN restableció la contraseña de ${user.username}")
        return ResponseEntity.ok(mapOf("message" to "Contraseña de ${user.username} actualizada"))
    }

    @GetMapping("/audit")
    @RequiresPermission(Permission.AUDIT_VIEW)
    @Operation(summary = "Listar audit logs")
    fun getAuditLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?
    ): ResponseEntity<AuditLogPageResponse> {
        val fromDateTime = fromDate?.atStartOfDay()
        val toDateTime = toDate?.atTime(LocalTime.MAX)

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val spec = AuditLogJpaRepository.byFilters(
            username = username,
            entityType = entityType,
            action = action,
            fromDate = fromDateTime,
            toDate = toDateTime
        )
        val auditPage = auditLogRepository.findAll(spec, pageable)

        val entries = auditPage.content.map { entity ->
            AuditLogEntry(
                id = entity.id,
                username = entity.username,
                action = entity.action,
                entityType = entity.entityType,
                entityId = entity.entityId,
                oldValues = entity.oldValues,
                newValues = entity.newValues,
                ipAddress = entity.ipAddress,
                createdAt = entity.createdAt
            )
        }

        val response = AuditLogPageResponse(
            content = entries,
            totalElements = auditPage.totalElements,
            totalPages = auditPage.totalPages,
            currentPage = auditPage.number,
            pageSize = auditPage.size
        )

        return ResponseEntity.ok(response)
    }
}

data class CreateUserRequest(
    @field:NotBlank @field:Size(min = 2, max = 100) val fullName: String,
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 8, max = 100) val password: String,
    val roleIds: List<Long> = emptyList()
)

data class UpdateUserRolesRequest(
    val roleIds: List<Long>
)

data class ResetPasswordRequest(
    @field:NotBlank @field:Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
    val newPassword: String
)
