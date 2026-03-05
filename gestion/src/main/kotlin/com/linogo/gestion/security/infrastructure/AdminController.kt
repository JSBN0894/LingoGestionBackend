package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.config.AdminOnly
import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.SecurityChecks
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Endpoints exclusivos para administradores")
@SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val securityChecks: SecurityChecks
) {

    @GetMapping("/users")
    @AdminOnly
    @Operation(summary = "Listar usuarios", description = "Solo administradores pueden acceder")
    fun getAllUsers(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, Any>> {
        // Ejemplo: verificar rol explícitamente
        if (!securityChecks.isAdmin()) {
            throw AccessDeniedException("Acceso denegado")
        }

        return ResponseEntity.ok(mapOf(
            "message" to "Lista de usuarios (solo admin)",
            "currentUser" to userDetails.username
        ))
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Estadísticas", description = "Admins y vendedores pueden acceder")
    fun getStats(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "totalUsers" to "150",
            "totalOrders" to "1200",
            "revenue" to "$50000"
        ))
    }

    @DeleteMapping("/users/{userId}")
    @AdminOnly
    @Operation(summary = "Eliminar usuario", description = "Solo administradores pueden eliminar")
    fun deleteUser(@PathVariable userId: String): ResponseEntity<Map<String, String>> {
        // Lógica de eliminación...
        return ResponseEntity.ok(mapOf(
            "message" to "Usuario $userId eliminado"
        ))
    }
}
